import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import {
  PromptApiService,
  PromptSummary,
  PromptDetail,
  PromptRun,
  PromptRating,
} from '../../core/services/prompt-api.service';

@Component({
  selector: 'app-prompt-playground',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, BadgeComponent],
  templateUrl: './prompt-playground.component.html',
  styleUrl: './prompt-playground.component.scss',
})
export class PromptPlaygroundComponent implements OnInit {
  readonly prompts = signal<PromptSummary[]>([]);
  readonly selectedPromptId = signal<number | null>(null);
  readonly selectedPrompt = signal<PromptDetail | null>(null);
  readonly selectedVersionId = signal<number | null>(null);
  readonly input = signal('');

  readonly running = signal(false);
  readonly lastRun = signal<PromptRun | null>(null);
  readonly runHistory = signal<PromptRun[]>([]);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly promptApi: PromptApiService, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    const preselectedId = Number(this.route.snapshot.queryParamMap.get('promptId')) || null;
    this.promptApi.list().subscribe({
      next: (prompts) => {
        this.prompts.set(prompts);
        const initial = preselectedId ?? prompts[0]?.id ?? null;
        if (initial) {
          this.onPromptChange(initial);
        }
      },
      error: () => this.errorMessage.set('Could not load prompts from the Spring Boot API.'),
    });
  }

  onPromptChange(id: number | string): void {
    const promptId = Number(id);
    this.selectedPromptId.set(promptId);
    this.promptApi.get(promptId).subscribe({
      next: (detail) => {
        this.selectedPrompt.set(detail);
        this.selectedVersionId.set(detail.versions[0]?.id ?? null);
        this.errorMessage.set(null);
      },
      error: () => this.errorMessage.set('Could not load that prompt.'),
    });
    this.loadHistory(promptId);
  }

  onVersionChange(id: string): void {
    this.selectedVersionId.set(Number(id));
  }

  loadHistory(promptId: number): void {
    this.promptApi.listRuns(promptId).subscribe({
      next: (runs) => this.runHistory.set(runs),
      error: () => this.errorMessage.set('Could not load run history.'),
    });
  }

  run(): void {
    const promptId = this.selectedPromptId();
    if (!promptId) return;

    this.running.set(true);
    this.promptApi.run(promptId, this.selectedVersionId(), this.input()).subscribe({
      next: (run) => {
        this.running.set(false);
        this.lastRun.set(run);
        this.errorMessage.set(null);
        this.loadHistory(promptId);
      },
      error: () => {
        this.running.set(false);
        this.errorMessage.set('Failed to run the prompt — is Ollama running?');
      },
    });
  }

  rate(runId: number, rating: PromptRating): void {
    this.promptApi.rate(runId, rating).subscribe({
      next: (updated) => {
        if (this.lastRun()?.id === updated.id) {
          this.lastRun.set(updated);
        }
        const promptId = this.selectedPromptId();
        if (promptId) this.loadHistory(promptId);
      },
    });
  }

  get selectedVersionTemplate(): string {
    const detail = this.selectedPrompt();
    const versionId = this.selectedVersionId();
    return detail?.versions.find((v) => v.id === versionId)?.template ?? '';
  }

  ratingTone(rating: PromptRating): BadgeTone {
    if (rating === 'PASS') return 'success';
    if (rating === 'FAIL') return 'error';
    return 'neutral';
  }
}
