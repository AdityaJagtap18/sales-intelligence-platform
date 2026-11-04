import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { ContextLakeApiService, ContextDocument, SearchResult } from '../../core/services/context-lake-api.service';

@Component({
  selector: 'app-context-lake',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, DataTableComponent],
  templateUrl: './context-lake.component.html',
  styleUrl: './context-lake.component.scss',
})
export class ContextLakeComponent implements OnInit {
  readonly documents = signal<ContextDocument[]>([]);
  readonly uploading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly searchQuery = signal('');
  readonly searching = signal(false);
  readonly searchResults = signal<SearchResult[]>([]);
  readonly searchError = signal<string | null>(null);

  readonly columns: TableColumn[] = [
    { key: 'filename', label: 'Document' },
    { key: 'status', label: 'Status' },
    { key: 'chunkCount', label: 'Chunks', align: 'right' },
    { key: 'sizeBytes', label: 'Size', align: 'right' },
    { key: 'uploadedAt', label: 'Uploaded' },
  ];

  constructor(private readonly contextLakeApi: ContextLakeApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.contextLakeApi.listDocuments().subscribe({
      next: (documents) => {
        this.documents.set(documents);
        this.errorMessage.set(null);
      },
      error: () => this.errorMessage.set('Could not load documents from the Spring Boot API.'),
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploading.set(true);
    this.contextLakeApi.upload(file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.load();
        input.value = '';
      },
      error: () => {
        this.uploading.set(false);
        this.errorMessage.set(`Failed to upload ${file.name}.`);
        input.value = '';
      },
    });
  }

  search(): void {
    const query = this.searchQuery().trim();
    if (!query) return;

    this.searching.set(true);
    this.contextLakeApi.search(query).subscribe({
      next: (results) => {
        this.searching.set(false);
        this.searchResults.set(results);
        this.searchError.set(null);
      },
      error: () => {
        this.searching.set(false);
        this.searchError.set('Semantic search failed — is Ollama running?');
      },
    });
  }

  statusTone(status: string): BadgeTone {
    if (status === 'INDEXED') return 'success';
    if (status === 'FAILED') return 'error';
    return 'info';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
