import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import {
  AiInsightsApiService,
  AiInsightResponse,
  AiInsightHistoryEntry,
} from '../../core/services/ai-insights-api.service';

@Component({
  selector: 'app-ai-insights',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, BadgeComponent, IconComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-panel title="Ask AI Insights" subtitle="Answers are grounded in live Databricks sales data and documents indexed in the Context Lake, with real citations">
        <form class="ask-form" (ngSubmit)="ask()">
          <input class="ask-input" type="text" [(ngModel)]="question" name="question"
            placeholder="Ask a question about the indexed documents…" [disabled]="asking()" />
          <button type="submit" class="btn-primary" [disabled]="asking() || !question.trim()">
            <app-icon name="sparkles" [size]="14" /> {{ asking() ? 'Thinking…' : 'Ask' }}
          </button>
        </form>
      </app-panel>

      @if (response()) {
        <app-panel [title]="'Answer'" [subtitle]="response()!.question">
          @switch (response()!.status) {
            @case ('ANSWERED') {
              <p class="answer">{{ response()!.answer }}</p>
              <h4>Sources</h4>
              <div class="citation-list">
                @for (c of response()!.citations; track c.documentId + '-' + c.chunkIndex) {
                  <div class="citation">
                    <div class="citation-head">
                      @if (c.documentId > 0) {
                        <a class="citation-filename" (click)="openDocument(c.documentId)">{{ c.filename }}</a>
                      } @else {
                        <span class="citation-filename citation-filename-live">{{ c.filename }}</span>
                      }
                      @if (c.chunkIndex >= 0) {
                        <app-badge tone="accent">chunk {{ c.chunkIndex }}</app-badge>
                      }
                      <app-badge tone="neutral">score {{ c.score }}</app-badge>
                    </div>
                    <p class="citation-snippet">{{ c.snippet }}</p>
                    @if (c.relatedEntities.length) {
                      <div class="related-entities">
                        <app-icon name="share" [size]="12" />
                        @for (e of c.relatedEntities; track e.type + e.name) {
                          <span class="related-entity">{{ e.type }}: {{ e.name }}</span>
                        }
                      </div>
                    }
                  </div>
                }
              </div>
            }
            @default {
              <app-empty-state icon="alert-triangle" [title]="statusTitle(response()!.status)"
                [description]="response()!.note ?? ''" />
              @if (response()!.status === 'NO_SOURCES') {
                <div class="toolbar">
                  <button type="button" class="btn-secondary" (click)="openContextLake()">
                    <app-icon name="database" [size]="14" /> Open Context Lake
                  </button>
                </div>
              }
            }
          }
        </app-panel>
      }

      <app-panel title="Question History">
        @if (history().length) {
          <div class="history-list">
            @for (h of history(); track h.id) {
              <div class="history-row">
                <app-badge [tone]="historyTone(h.status)">{{ h.status }}</app-badge>
                <span class="history-question">{{ h.question }}</span>
                <span class="history-meta">{{ h.citationCount }} source(s) · {{ h.askedAt | date: 'short' }}</span>
              </div>
            }
          </div>
        } @else {
          <app-empty-state title="No questions asked yet" />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .ask-form { display: flex; gap: var(--space-3); }
    .ask-input {
      flex: 1; padding: 9px var(--space-3); border-radius: var(--radius-sm); border: 1px solid var(--color-border);
      background: var(--color-surface-card-hover); color: var(--color-text-primary); font-size: var(--font-size-sm);
    }
    .btn-primary, .btn-secondary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-primary { background: var(--color-accent); border: none; color: #fff; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    .answer { font-size: var(--font-size-md); color: var(--color-text-primary); line-height: 1.6; white-space: pre-wrap; }
    h4 { margin: var(--space-4) 0 var(--space-2); font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .citation-list { display: flex; flex-direction: column; gap: var(--space-3); }
    .citation { padding: var(--space-3); background: var(--color-surface-card-hover); border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .citation-head { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
    .citation-filename { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-accent-hover); cursor: pointer; }
    .citation-filename-live { cursor: default; color: var(--color-text-primary); }
    .citation-snippet { margin-top: 6px; font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .related-entities { margin-top: 6px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-size: var(--font-size-xs); color: var(--color-text-muted); }
    .related-entity { background: var(--color-surface-card); border-radius: var(--radius-sm); padding: 2px 6px; }
    .toolbar { margin-top: var(--space-3); }
    .history-list { display: flex; flex-direction: column; gap: var(--space-2); }
    .history-row { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-2) 0; border-bottom: 1px solid var(--color-border); }
    .history-question { flex: 1; font-size: var(--font-size-sm); color: var(--color-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .history-meta { font-size: var(--font-size-xs); color: var(--color-text-muted); flex-shrink: 0; }
    `,
  ],
})
export class AiInsightsComponent implements OnInit {
  question = '';
  readonly asking = signal(false);
  readonly response = signal<AiInsightResponse | null>(null);
  readonly history = signal<AiInsightHistoryEntry[]>([]);

  constructor(
    private readonly api: AiInsightsApiService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.loadHistory();
    const q = this.route.snapshot.queryParamMap.get('q');
    if (q) {
      this.question = q;
      this.ask();
    }
  }

  ask(): void {
    const q = this.question.trim();
    if (!q) return;
    this.asking.set(true);
    this.api.ask(q).subscribe({
      next: (r) => {
        this.response.set(r);
        this.asking.set(false);
        this.loadHistory();
      },
      error: () => {
        this.asking.set(false);
      },
    });
  }

  loadHistory(): void {
    this.api.history().subscribe({ next: (h) => this.history.set(h) });
  }

  openDocument(documentId: number): void {
    this.router.navigate(['/data-platform/context-lake'], { queryParams: { documentId } });
  }

  openContextLake(): void {
    this.router.navigate(['/data-platform/context-lake']);
  }

  statusTitle(status: string): string {
    switch (status) {
      case 'NO_SOURCES':
        return 'No documents indexed yet';
      case 'INSUFFICIENT_CONTEXT':
        return 'Nothing relevant found';
      case 'GENERATION_FAILED':
        return 'The local model could not answer';
      default:
        return status;
    }
  }

  historyTone(status: string): BadgeTone {
    return status === 'ANSWERED' ? 'success' : status === 'GENERATION_FAILED' ? 'error' : 'neutral';
  }
}
