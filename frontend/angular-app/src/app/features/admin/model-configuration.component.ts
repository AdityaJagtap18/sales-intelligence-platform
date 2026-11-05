import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import {
  ModelConfigurationApiService,
  ModelConfiguration,
} from '../../core/services/model-configuration-api.service';

@Component({
  selector: 'app-model-configuration',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, EmptyStateComponent],
  template: `
    <div class="page">
      @if (loadError()) {
        <app-empty-state icon="alert-triangle" tone="error" title="Could not load model configuration" [description]="loadError()!" />
      } @else if (config()) {
        <app-panel title="Ollama Connection">
          <app-badge [tone]="config()!.ollamaStatus === 'UP' ? 'success' : 'error'">{{ config()!.ollamaStatus }}</app-badge>
          <p class="hint">{{ config()!.ollamaStatus === 'UP' ? config()!.availableModels.length + ' model(s) pulled locally.' : 'Ollama is unreachable — pull a model and ensure the container is running.' }}</p>
        </app-panel>

        <app-panel title="Embedding Model" subtitle="Fixed — used for every vector in the Context Lake's Qdrant collection">
          <p class="fixed-model">{{ config()!.embeddingModel }}</p>
          <p class="hint">Changing this would require re-embedding every document and recreating the Qdrant collection at a new vector size, so it isn't switchable from here.</p>
        </app-panel>

        <app-panel title="Chat Model" subtitle="Used by Prompt Playground and AI Insights — takes effect immediately">
          @if (config()!.availableModels.length) {
            <div class="model-grid">
              @for (m of config()!.availableModels; track m) {
                <button type="button" class="model-option" [class.active]="m === config()!.activeChatModel"
                  (click)="select(m)" [disabled]="saving()">
                  @if (m === config()!.activeChatModel) { <app-icon name="check" [size]="14" /> }
                  {{ m }}
                </button>
              }
            </div>
          } @else {
            <app-empty-state icon="cpu" title="No models pulled yet"
              [description]="'Run ollama pull ' + config()!.defaultChatModel + ' (or another model) on the Ollama container, then refresh.'" />
          }
          <p class="hint">Active: <strong>{{ config()!.activeChatModel }}</strong>@if (config()!.updatedAt) { (changed {{ config()!.updatedAt | date: 'short' }}) }</p>
        </app-panel>
      }
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .hint { font-size: var(--font-size-sm); color: var(--color-text-muted); margin-top: var(--space-2); }
    .fixed-model { font-size: var(--font-size-md); font-weight: 600; color: var(--color-text-primary); }
    .model-grid { display: flex; flex-wrap: wrap; gap: var(--space-2); }
    .model-option {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
      background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary);
    }
    .model-option.active { background: var(--color-accent-soft); border-color: var(--color-accent); color: var(--color-accent-hover); }
    .model-option:disabled { opacity: 0.6; cursor: not-allowed; }
    `,
  ],
})
export class ModelConfigurationComponent implements OnInit {
  readonly config = signal<ModelConfiguration | null>(null);
  readonly saving = signal(false);
  readonly loadError = signal<string | null>(null);

  constructor(private readonly api: ModelConfigurationApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.current().subscribe({
      next: (c) => this.config.set(c),
      error: () => this.loadError.set('Could not reach the Spring Boot API.'),
    });
  }

  select(model: string): void {
    if (model === this.config()?.activeChatModel) return;
    this.saving.set(true);
    this.api.setChatModel(model).subscribe({
      next: (c) => {
        this.config.set(c);
        this.saving.set(false);
      },
      error: () => this.saving.set(false),
    });
  }
}
