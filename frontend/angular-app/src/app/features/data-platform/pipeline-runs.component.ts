import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, forkJoin, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent, BadgeTone } from '../../shared/components/badge/badge.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { PipelineApiService, PipelineRun } from '../../core/services/pipeline-api.service';

const POLL_INTERVAL_MS = 5000;

@Component({
  selector: 'app-pipeline-runs',
  standalone: true,
  imports: [CommonModule, PanelComponent, BadgeComponent, IconComponent, DataTableComponent],
  templateUrl: './pipeline-runs.component.html',
  styleUrl: './pipeline-runs.component.scss',
})
export class PipelineRunsComponent implements OnInit, OnDestroy {
  readonly runs = signal<PipelineRun[]>([]);
  readonly triggering = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly columns: TableColumn[] = [
    { key: 'id', label: 'Run' },
    { key: 'databricksRunId', label: 'Databricks Run ID' },
    { key: 'lifecycleState', label: 'Status' },
    { key: 'triggeredBy', label: 'Triggered By' },
    { key: 'startedAt', label: 'Started' },
    { key: 'completedAt', label: 'Completed' },
  ];

  private pollSubscription?: Subscription;

  constructor(private readonly pipelineApi: PipelineApiService) {}

  ngOnInit(): void {
    this.refresh();
    // Every tick, re-check Databricks for any run that isn't terminal yet
    // (GET /pipelines/runs/{id} makes Spring Boot poll Databricks and persist
    // the result) so the UI reflects real pipeline progress, not just a
    // cached "PENDING" row.
    this.pollSubscription = interval(POLL_INTERVAL_MS)
      .pipe(
        switchMap(() => {
          const nonTerminal = this.runs().filter((run) => !this.isTerminal(run.lifecycleState));
          if (nonTerminal.length === 0) {
            return this.pipelineApi.listRuns();
          }
          return forkJoin(nonTerminal.map((run) => this.pipelineApi.getRun(run.id))).pipe(
            switchMap(() => this.pipelineApi.listRuns()),
          );
        }),
      )
      .subscribe({
        next: (runs) => this.runs.set(runs),
        error: () => this.errorMessage.set('Lost connection to the Spring Boot API.'),
      });
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
  }

  refresh(): void {
    this.pipelineApi.listRuns().subscribe({
      next: (runs) => {
        this.runs.set(runs);
        this.errorMessage.set(null);
      },
      error: () => this.errorMessage.set('Could not load pipeline runs from the Spring Boot API.'),
    });
  }

  runPipeline(): void {
    this.triggering.set(true);
    this.pipelineApi.triggerRun().subscribe({
      next: () => {
        this.triggering.set(false);
        this.refresh();
      },
      error: () => {
        this.triggering.set(false);
        this.errorMessage.set('Failed to trigger the pipeline run.');
      },
    });
  }

  private isTerminal(lifecycleState: string): boolean {
    return lifecycleState === 'TERMINATED' || lifecycleState === 'SKIPPED' || lifecycleState === 'INTERNAL_ERROR';
  }

  statusTone(lifecycleState: string, resultState: string | null): BadgeTone {
    if (lifecycleState === 'TERMINATED') {
      return resultState === 'SUCCESS' ? 'success' : 'error';
    }
    if (lifecycleState === 'INTERNAL_ERROR' || lifecycleState === 'SKIPPED') {
      return 'error';
    }
    if (lifecycleState === 'RUNNING' || lifecycleState === 'PENDING') {
      return 'info';
    }
    return 'neutral';
  }
}
