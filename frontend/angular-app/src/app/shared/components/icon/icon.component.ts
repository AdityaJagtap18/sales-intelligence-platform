import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Minimal line-icon set (feather-style, single stroke) so the whole app
 * shares one icon language instead of mixing icon libraries/styles.
 * Paths are a fixed, developer-controlled dictionary — never user input.
 */
const ICONS: Record<string, string> = {
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  'bar-chart': '<line x1="5" y1="20" x2="5" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="19" y1="20" x2="19" y2="14"/>',
  'bar-chart-2': '<rect x="4" y="12" width="4" height="8"/><rect x="10" y="6" width="4" height="14"/><rect x="16" y="9" width="4" height="11"/>',
  'file-text': '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><polyline points="14 3 14 8 19 8"/><line x1="9" y1="13" x2="15" y2="13"/><line x1="9" y1="17" x2="15" y2="17"/>',
  compass: '<circle cx="12" cy="12" r="9"/><polygon points="14.5 9.5 10 10 9.5 14.5 14 14 14.5 9.5"/>',
  users: '<path d="M16 19v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1"/><circle cx="9" cy="7" r="3.2"/><path d="M22 19v-1a4 4 0 0 0-3-3.87"/><path d="M16 3.3a4 4 0 0 1 0 7.5"/>',
  box: '<path d="M21 8l-9-5-9 5 9 5 9-5z"/><path d="M3 8v8l9 5 9-5V8"/><line x1="12" y1="13" x2="12" y2="21"/>',
  'trending-up': '<polyline points="3 17 9 11 13 15 21 6"/><polyline points="15 6 21 6 21 12"/>',
  'trending-down': '<polyline points="3 7 9 13 13 9 21 18"/><polyline points="15 18 21 18 21 12"/>',
  'alert-triangle': '<path d="M12 3 2 20h20L12 3z"/><line x1="12" y1="10" x2="12" y2="14.5"/><circle cx="12" cy="17.2" r="0.6" fill="currentColor"/>',
  'git-branch': '<line x1="6" y1="3" x2="6" y2="15"/><circle cx="18" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M18 9a9 9 0 0 1-9 9"/>',
  activity: '<polyline points="3 12 7 12 9 18 15 6 17 12 21 12"/>',
  plug: '<path d="M9 2v4"/><path d="M15 2v4"/><path d="M6 10h12v3a6 6 0 0 1-12 0z"/><path d="M12 19v3"/>',
  database: '<ellipse cx="12" cy="5.5" rx="8" ry="3"/><path d="M4 5.5V18c0 1.7 3.6 3 8 3s8-1.3 8-3V5.5"/><path d="M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/>',
  layers: '<polygon points="12 2 2 8 12 14 22 8 12 2"/><polyline points="2 13 12 19 22 13"/>',
  link: '<path d="M10 13a5 5 0 0 0 7.5.5l2-2a5 5 0 0 0-7-7l-1.2 1.2"/><path d="M14 11a5 5 0 0 0-7.5-.5l-2 2a5 5 0 0 0 7 7l1.1-1.1"/>',
  shield: '<path d="M12 3l8 3v6c0 5-3.5 8.5-8 9-4.5-.5-8-4-8-9V6l8-3z"/>',
  share: '<circle cx="6" cy="12" r="2.4"/><circle cx="18" cy="5.5" r="2.4"/><circle cx="18" cy="18.5" r="2.4"/><line x1="8.1" y1="10.8" x2="15.9" y2="6.7"/><line x1="8.1" y1="13.2" x2="15.9" y2="17.3"/>',
  search: '<circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.2" y2="16.2"/>',
  'message-circle': '<path d="M21 12a8.5 8.5 0 0 1-12.4 7.6L3 21l1.5-5.3A8.5 8.5 0 1 1 21 12z"/>',
  terminal: '<polyline points="5 7 10 12 5 17"/><line x1="12" y1="17" x2="19" y2="17"/>',
  lightbulb: '<path d="M9 18h6"/><path d="M10 22h4"/><path d="M12 2a6.5 6.5 0 0 0-4 11.6c.6.5 1 1.3 1 2.4h6c0-1.1.4-1.9 1-2.4A6.5 6.5 0 0 0 12 2z"/>',
  heart: '<path d="M12 21s-7.5-4.6-10-9.1C.5 8.6 2.3 5 6 5c2 0 3.5 1.1 4.3 2.6C11.1 6.1 12.6 5 14.6 5c3.7 0 5.5 3.6 4 6.9C19.5 16.4 12 21 12 21z"/>',
  bell: '<path d="M18 16v-5a6 6 0 0 0-12 0v5l-2 2h16z"/><path d="M10 20a2 2 0 0 0 4 0"/>',
  sliders: '<line x1="4" y1="6" x2="20" y2="6"/><circle cx="9" cy="6" r="2"/><line x1="4" y1="12" x2="20" y2="12"/><circle cx="16" cy="12" r="2"/><line x1="4" y1="18" x2="20" y2="18"/><circle cx="7" cy="18" r="2"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.2a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.2a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.2a1.7 1.7 0 0 0-1.4 1z"/>',
  'grid-plus': '<rect x="3" y="3" width="8" height="8" rx="1.5"/><rect x="13" y="3" width="8" height="8" rx="1.5"/><line x1="17" y1="15" x2="17" y2="21"/><line x1="14" y1="18" x2="20" y2="18"/>',
  cpu: '<rect x="6" y="6" width="12" height="12" rx="1.5"/><rect x="9" y="9" width="6" height="6"/><line x1="12" y1="2" x2="12" y2="5"/><line x1="12" y1="19" x2="12" y2="22"/><line x1="2" y1="12" x2="5" y2="12"/><line x1="19" y1="12" x2="22" y2="12"/>',
  user: '<circle cx="12" cy="8" r="3.5"/><path d="M5 20a7 7 0 0 1 14 0"/>',
  'chevron-left': '<polyline points="15 18 9 12 15 6"/>',
  'chevron-right': '<polyline points="9 18 15 12 9 6"/>',
  'chevron-down': '<polyline points="6 9 12 15 18 9"/>',
  sparkles: '<path d="M12 3l1.6 4.8L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.2L12 3z"/><path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z"/>',
  'help-circle': '<circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 0 1 4.8 1c0 1.6-2.3 1.8-2.3 3.4"/><circle cx="12" cy="17" r="0.6" fill="currentColor"/>',
  refresh: '<polyline points="1 4 1 10 7 10"/><polyline points="23 20 23 14 17 14"/><path d="M20.5 9A9 9 0 0 0 4.6 6.3L1 10m22 4-3.6 3.7A9 9 0 0 1 3.5 15"/>',
};

@Component({
  selector: 'app-icon',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg
      [attr.width]="size"
      [attr.height]="size"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
      [innerHTML]="path"
    ></svg>
  `,
  styles: [
    `:host { display: inline-flex; line-height: 0; }`,
  ],
})
export class IconComponent {
  @Input() name = 'grid';
  @Input() size = 18;

  constructor(private sanitizer: DomSanitizer) {}

  /**
   * Safe: ICONS is a fixed, developer-authored dictionary, never user input.
   * The default sanitizer strips SVG child elements from [innerHTML], so
   * this trusted, static markup is explicitly exempted.
   */
  get path(): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(ICONS[this.name] ?? ICONS['grid']);
  }
}
