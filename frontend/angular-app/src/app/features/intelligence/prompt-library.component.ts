import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import {
  PromptApiService,
  PromptSummary,
  PromptDetail,
} from '../../core/services/prompt-api.service';

@Component({
  selector: 'app-prompt-library',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PanelComponent, IconComponent],
  templateUrl: './prompt-library.component.html',
  styleUrl: './prompt-library.component.scss',
})
export class PromptLibraryComponent implements OnInit {
  readonly prompts = signal<PromptSummary[]>([]);
  readonly selectedPrompt = signal<PromptDetail | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly creating = signal(false);
  readonly newName = signal('');
  readonly newDescription = signal('');
  readonly newTemplate = signal('');

  readonly addingVersion = signal(false);
  readonly newVersionTemplate = signal('');

  constructor(private readonly promptApi: PromptApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.promptApi.list().subscribe({
      next: (prompts) => {
        this.prompts.set(prompts);
        this.errorMessage.set(null);
      },
      error: () => this.errorMessage.set('Could not load prompts from the Spring Boot API.'),
    });
  }

  select(id: number): void {
    this.promptApi.get(id).subscribe({
      next: (detail) => {
        this.selectedPrompt.set(detail);
        this.newVersionTemplate.set(detail.versions[0]?.template ?? '');
      },
      error: () => this.errorMessage.set('Could not load that prompt.'),
    });
  }

  createPrompt(): void {
    const name = this.newName().trim();
    const template = this.newTemplate().trim();
    if (!name || !template) return;

    this.creating.set(true);
    this.promptApi.create({ name, description: this.newDescription().trim() || undefined, template }).subscribe({
      next: (detail) => {
        this.creating.set(false);
        this.newName.set('');
        this.newDescription.set('');
        this.newTemplate.set('');
        this.load();
        this.selectedPrompt.set(detail);
      },
      error: () => {
        this.creating.set(false);
        this.errorMessage.set('Failed to create the prompt.');
      },
    });
  }

  addVersion(): void {
    const prompt = this.selectedPrompt();
    const template = this.newVersionTemplate().trim();
    if (!prompt || !template) return;

    this.addingVersion.set(true);
    this.promptApi.addVersion(prompt.id, template).subscribe({
      next: () => {
        this.addingVersion.set(false);
        this.select(prompt.id);
      },
      error: () => {
        this.addingVersion.set(false);
        this.errorMessage.set('Failed to add a new version.');
      },
    });
  }
}
