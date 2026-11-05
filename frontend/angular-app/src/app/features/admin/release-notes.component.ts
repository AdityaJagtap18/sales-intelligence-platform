import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ReleaseNotesApiService, ReleaseNote } from '../../core/services/release-notes-api.service';

@Component({
  selector: 'app-release-notes',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, EmptyStateComponent],
  template: `
    <div class="page">
      @if (loadError()) {
        <app-empty-state icon="alert-triangle" tone="error" title="Could not load release notes" [description]="loadError()!" />
      } @else {
        <app-panel title="Version" [subtitle]="'Currently running v' + currentVersion()">
          <p class="intro">What's changed in this app, most recent first — mirrors the repo's own CHANGELOG.md.</p>
        </app-panel>

        @for (n of notes(); track n.version) {
          <app-panel [title]="'v' + n.version" [subtitle]="n.title">
            @if (n.added.length) {
              <div class="note-group">
                <app-badge tone="success">Added</app-badge>
                <ul>
                  @for (item of n.added; track item) { <li>{{ item }}</li> }
                </ul>
              </div>
            }
            @if (n.changed.length) {
              <div class="note-group">
                <app-badge tone="info">Changed</app-badge>
                <ul>
                  @for (item of n.changed; track item) { <li>{{ item }}</li> }
                </ul>
              </div>
            }
            @if (n.fixed.length) {
              <div class="note-group">
                <app-badge tone="warning">Fixed</app-badge>
                <ul>
                  @for (item of n.fixed; track item) { <li>{{ item }}</li> }
                </ul>
              </div>
            }
          </app-panel>
        }
      }
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .intro { margin: 0; font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .note-group { margin-bottom: var(--space-3); }
    .note-group:last-child { margin-bottom: 0; }
    .note-group ul { margin: var(--space-2) 0 0; padding-left: 20px; }
    .note-group li { font-size: var(--font-size-sm); color: var(--color-text-primary); line-height: 1.6; }
    `,
  ],
})
export class ReleaseNotesComponent implements OnInit {
  readonly currentVersion = signal('');
  readonly notes = signal<ReleaseNote[]>([]);
  readonly loadError = signal<string | null>(null);

  constructor(private readonly api: ReleaseNotesApiService) {}

  ngOnInit(): void {
    this.api.get().subscribe({
      next: (r) => {
        this.currentVersion.set(r.currentVersion);
        this.notes.set(r.notes);
      },
      error: () => this.loadError.set('Could not reach the Spring Boot API.'),
    });
  }
}
