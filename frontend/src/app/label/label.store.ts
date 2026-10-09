import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { problemDetail } from '../core/problem-detail';
import { LabelApi } from './label.api';
import { Label } from './label.model';

/** App-wide label list, shared by the sidebar and the task rows. */
@Injectable({ providedIn: 'root' })
export class LabelStore {
  private readonly api = inject(LabelApi);

  private readonly _labels = signal<Label[]>([]);
  private readonly _error = signal<string | null>(null);

  readonly labels = this._labels.asReadonly();
  readonly error = this._error.asReadonly();

  private readonly byId = computed(() => new Map(this._labels().map((label) => [label.id, label])));

  load(): void {
    this.api.list().subscribe({
      next: (labels) => {
        this._labels.set(labels);
        this._error.set(null);
      },
      error: () => this._error.set('Could not load labels.'),
    });
  }

  create(name: string): Observable<Label> {
    return this.api.create(name).pipe(
      tap({
        next: (label) => {
          this._labels.update((labels) =>
            [...labels, label].sort((a, b) => a.name.localeCompare(b.name)),
          );
          this._error.set(null);
        },
        error: (error: unknown) =>
          this._error.set(problemDetail(error, 'Could not create the label.')),
      }),
    );
  }

  /** Names of the given labels, skipping ids that are not loaded (yet). */
  namesOf(ids: readonly string[]): string[] {
    const byId = this.byId();
    return ids.flatMap((id) => {
      const label = byId.get(id);
      return label ? [label.name] : [];
    });
  }

  /** Whether all ids are known; if not, the list is stale (e.g. quick-add created a label). */
  knowsAll(ids: readonly string[]): boolean {
    const byId = this.byId();
    return ids.every((id) => byId.has(id));
  }
}
