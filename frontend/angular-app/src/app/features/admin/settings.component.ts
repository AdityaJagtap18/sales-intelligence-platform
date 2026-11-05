import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { SettingsApiService, SettingsSection } from '../../core/services/settings-api.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, PanelComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-empty-state icon="settings" title="Effective configuration (read-only)"
        description="Every value here comes straight from this app's startup configuration — environment variables and application.properties. Secrets are masked; nothing here can be changed at runtime without a restart." />

      @if (loadError()) {
        <app-empty-state icon="alert-triangle" tone="error" title="Could not load settings" [description]="loadError()!" />
      } @else {
        @for (s of sections(); track s.section) {
          <app-panel [title]="s.section">
            <dl class="entries">
              @for (e of s.entries; track e.key) {
                <div class="entry">
                  <dt>{{ e.key }}</dt>
                  <dd>{{ e.value }}</dd>
                </div>
              }
            </dl>
          </app-panel>
        }
      }
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .entries { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: var(--space-3) var(--space-5); margin: 0; }
    .entry { display: flex; justify-content: space-between; gap: var(--space-3); padding: 6px 0; border-bottom: 1px solid var(--color-border); }
    dt { font-size: var(--font-size-sm); color: var(--color-text-muted); margin: 0; }
    dd { font-size: var(--font-size-sm); color: var(--color-text-primary); margin: 0; font-weight: 500; text-align: right; word-break: break-word; }
    `,
  ],
})
export class SettingsComponent implements OnInit {
  readonly sections = signal<SettingsSection[]>([]);
  readonly loadError = signal<string | null>(null);

  constructor(private readonly api: SettingsApiService) {}

  ngOnInit(): void {
    this.api.effectiveConfiguration().subscribe({
      next: (s) => this.sections.set(s),
      error: () => this.loadError.set('Could not reach the Spring Boot API.'),
    });
  }
}
