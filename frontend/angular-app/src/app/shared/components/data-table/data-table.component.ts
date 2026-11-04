import { Component, EventEmitter, Input, Output, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TableColumn {
  key: string;
  label: string;
  align?: 'left' | 'right' | 'center';
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
})
export class DataTableComponent {
  @Input() columns: TableColumn[] = [];
  @Input() rows: Record<string, any>[] = [];
  /** Optional per-column custom cell template, keyed by column key. */
  @Input() cellTemplates: Record<string, TemplateRef<any>> = {};
  @Input() emptyMessage = 'No data available.';
  /** Set true (or bind rowClick) to make rows clickable — used for drill-down/detail views. */
  @Input() clickableRows = false;
  @Output() rowClick = new EventEmitter<Record<string, any>>();

  onRowClick(row: Record<string, any>): void {
    if (this.clickableRows || this.rowClick.observed) {
      this.rowClick.emit(row);
    }
  }
}
