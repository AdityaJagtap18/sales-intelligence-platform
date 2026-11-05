import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { UsersApiService, AppUser } from '../../core/services/users-api.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, BadgeComponent, IconComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-empty-state icon="shield" title="Scoped to user administration"
        description="This manages real, persisted users, roles and salted-password credentials. It does not yet gate the rest of this API behind a login — see docs/implementation/known-limitations.md." />

      <app-panel title="Add User">
        <form class="add-form" (ngSubmit)="create()">
          <input class="field" type="text" [(ngModel)]="newUsername" name="username" placeholder="Username" required minlength="3" />
          <input class="field" type="password" [(ngModel)]="newPassword" name="password" placeholder="Password (min 8 chars)" required minlength="8" />
          <select class="field" [(ngModel)]="newRole" name="role">
            <option value="VIEWER">Viewer</option>
            <option value="ANALYST">Analyst</option>
            <option value="ADMIN">Admin</option>
          </select>
          <button type="submit" class="btn-primary" [disabled]="creating()">
            <app-icon name="user" [size]="14" /> {{ creating() ? 'Adding…' : 'Add User' }}
          </button>
        </form>
        @if (createError()) {
          <p class="error-text">{{ createError() }}</p>
        }
      </app-panel>

      <app-panel title="Users">
        @if (users().length) {
          <div class="user-list">
            @for (u of users(); track u.id) {
              <div class="user-row">
                <div class="user-identity">
                  <span class="username">{{ u.username }}</span>
                  <span class="meta">created {{ u.createdAt | date: 'short' }} ·
                    {{ u.lastLoginAt ? 'last login ' + (u.lastLoginAt | date: 'short') : 'never logged in' }}</span>
                </div>
                <select class="role-select" [value]="u.role" (change)="setRole(u, $any($event.target).value)">
                  <option value="VIEWER">Viewer</option>
                  <option value="ANALYST">Analyst</option>
                  <option value="ADMIN">Admin</option>
                </select>
                <app-badge [tone]="u.enabled ? 'success' : 'neutral'">{{ u.enabled ? 'Enabled' : 'Disabled' }}</app-badge>
                <div class="row-actions">
                  <button type="button" class="btn-secondary" (click)="toggleEnabled(u)">{{ u.enabled ? 'Disable' : 'Enable' }}</button>
                  <button type="button" class="btn-secondary" (click)="resetPassword(u)">Reset Password</button>
                </div>
              </div>
            }
          </div>
        } @else {
          <app-empty-state title="No users yet" />
        }
      </app-panel>
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .add-form { display: flex; gap: var(--space-2); flex-wrap: wrap; }
    .field {
      padding: 7px var(--space-3); border-radius: var(--radius-sm); border: 1px solid var(--color-border);
      background: var(--color-surface-card-hover); color: var(--color-text-primary); font-size: var(--font-size-sm);
    }
    .btn-primary, .btn-secondary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
    }
    .btn-primary { background: var(--color-accent); border: none; color: #fff; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-secondary { background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
    .error-text { color: var(--color-error); font-size: var(--font-size-sm); margin-top: var(--space-2); }
    .user-list { display: flex; flex-direction: column; gap: var(--space-2); }
    .user-row { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3); background: var(--color-surface-card-hover); border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .user-identity { flex: 1; display: flex; flex-direction: column; }
    .username { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-primary); }
    .meta { font-size: var(--font-size-xs); color: var(--color-text-muted); }
    .role-select {
      padding: 6px var(--space-3); border-radius: var(--radius-sm); border: 1px solid var(--color-border);
      background: var(--color-surface-card); color: var(--color-text-secondary); font-size: var(--font-size-sm);
    }
    .row-actions { display: flex; gap: var(--space-2); }
    `,
  ],
})
export class UserManagementComponent implements OnInit {
  readonly users = signal<AppUser[]>([]);
  readonly creating = signal(false);
  readonly createError = signal<string | null>(null);

  newUsername = '';
  newPassword = '';
  newRole = 'VIEWER';

  constructor(private readonly api: UsersApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe({ next: (u) => this.users.set(u) });
  }

  create(): void {
    if (this.newUsername.trim().length < 3 || this.newPassword.length < 8) return;
    this.creating.set(true);
    this.createError.set(null);
    this.api.create({ username: this.newUsername.trim(), password: this.newPassword, role: this.newRole }).subscribe({
      next: () => {
        this.creating.set(false);
        this.newUsername = '';
        this.newPassword = '';
        this.newRole = 'VIEWER';
        this.reload();
      },
      error: (err) => {
        this.creating.set(false);
        this.createError.set(err?.status === 409 ? 'That username already exists.' : 'Could not create the user.');
      },
    });
  }

  setRole(user: AppUser, role: string): void {
    this.api.setRole(user.id, role).subscribe({ next: () => this.reload() });
  }

  toggleEnabled(user: AppUser): void {
    this.api.setEnabled(user.id, !user.enabled).subscribe({ next: () => this.reload() });
  }

  resetPassword(user: AppUser): void {
    const next = window.prompt(`New password for ${user.username} (min 8 characters):`);
    if (!next || next.length < 8) return;
    this.api.resetPassword(user.id, next).subscribe({ next: () => this.reload() });
  }
}
