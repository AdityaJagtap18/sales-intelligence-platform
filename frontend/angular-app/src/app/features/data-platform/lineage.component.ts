import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { LineageApiService, Lineage } from '../../core/services/lineage-api.service';

@Component({
  selector: 'app-lineage',
  standalone: true,
  imports: [CommonModule, PanelComponent, IconComponent],
  templateUrl: './lineage.component.html',
  styleUrl: './lineage.component.scss',
})
export class LineageComponent implements OnInit {
  readonly tables = signal<string[]>([]);
  readonly selectedTable = signal<string | null>(null);
  readonly lineage = signal<Lineage | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly lineageApi: LineageApiService) {}

  ngOnInit(): void {
    this.lineageApi.listTables().subscribe({
      next: (tables) => {
        this.tables.set(tables);
        if (tables.length > 0) {
          this.selectTable(tables[0]);
        }
      },
      error: () => this.errorMessage.set('Could not load the table list from the Spring Boot API.'),
    });
  }

  selectTable(table: string): void {
    this.selectedTable.set(table);
    this.loading.set(true);
    this.lineageApi.getLineage(table).subscribe({
      next: (lineage) => {
        this.loading.set(false);
        this.lineage.set(lineage);
        this.errorMessage.set(null);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set(`Could not load lineage for ${table}.`);
      },
    });
  }

  stageIcon(stage: string): string {
    switch (stage) {
      case 'Source':
        return 'database';
      case 'Catalog':
        return 'layers';
      default:
        return 'git-branch';
    }
  }
}
