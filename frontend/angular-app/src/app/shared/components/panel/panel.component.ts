import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './panel.component.html',
  styleUrl: './panel.component.scss',
})
export class PanelComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() accent = false;
}
