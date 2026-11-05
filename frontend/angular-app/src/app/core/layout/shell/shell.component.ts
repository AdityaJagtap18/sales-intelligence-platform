import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';

function leafTitle(snapshot: ActivatedRouteSnapshot): string {
  let current = snapshot;
  while (current.firstChild) {
    current = current.firstChild;
  }
  return current.data['title'] ?? 'Sellix AI';
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private router = inject(Router);

  collapsed = signal(false);

  pageTitle = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => leafTitle(this.router.routerState.snapshot.root)),
      startWith('Executive Overview'),
    ),
    { initialValue: 'Executive Overview' },
  );
}
