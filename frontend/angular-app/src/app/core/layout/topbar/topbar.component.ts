import { Component, HostListener, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { AlertsApiService, Alert } from '../../services/alerts-api.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
})
export class TopbarComponent implements OnInit {
  @Input() pageTitle = '';

  readonly openAlerts = signal<Alert[]>([]);
  searchQuery = '';
  notifOpen = false;
  helpOpen = false;

  constructor(private readonly router: Router, private readonly alertsApi: AlertsApiService) {}

  ngOnInit(): void {
    this.alertsApi.list('OPEN').subscribe({ next: (alerts) => this.openAlerts.set(alerts) });
  }

  @HostListener('document:click')
  closeMenus(): void {
    this.notifOpen = false;
    this.helpOpen = false;
  }

  toggleNotifications(event: Event): void {
    event.stopPropagation();
    this.helpOpen = false;
    this.notifOpen = !this.notifOpen;
  }

  toggleHelp(event: Event): void {
    event.stopPropagation();
    this.notifOpen = false;
    this.helpOpen = !this.helpOpen;
  }

  goToAiAssistant(): void {
    this.router.navigate(['/intelligence/ai-insights']);
  }

  submitSearch(): void {
    const q = this.searchQuery.trim();
    if (!q) return;
    this.router.navigate(['/intelligence/ai-insights'], { queryParams: { q } });
  }

  severityTone(severity: string): 'error' | 'warning' | 'info' {
    if (severity === 'CRITICAL') return 'error';
    if (severity === 'WARNING') return 'warning';
    return 'info';
  }
}
