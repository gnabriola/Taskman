import { Injectable, inject, signal } from '@angular/core';
import { EMPTY, Observable, tap } from 'rxjs';

import { problemDetail } from '../core/problem-detail';
import { TIME_ZONE } from '../core/time-zone';
import { TaskApi } from './task.api';
import { Task } from './task.model';

/** Open tasks of one project. Provided per page, so each page gets its own state. */
@Injectable()
export class TaskStore {
  private readonly api = inject(TaskApi);
  private readonly timeZone = inject(TIME_ZONE);

  private projectId: string | null = null;

  private readonly _tasks = signal<Task[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _addedToOtherProject = signal<string | null>(null);

  readonly tasks = this._tasks.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  /** Id of the project the last quick-added task went to, if it is not this one. */
  readonly addedToOtherProject = this._addedToOtherProject.asReadonly();

  load(projectId: string): void {
    this.projectId = projectId;
    this._tasks.set([]);
    this._loading.set(true);
    this._error.set(null);
    this._addedToOtherProject.set(null);
    this.api.listOpen(projectId).subscribe({
      next: (tasks) => {
        // Ignore a slow response for a project the user has already navigated away from.
        if (this.projectId === projectId) {
          this._tasks.set(tasks);
          this._loading.set(false);
        }
      },
      error: () => {
        if (this.projectId === projectId) {
          this._error.set('Could not load tasks.');
          this._loading.set(false);
        }
      },
    });
  }

  /**
   * Creates a task from quick-add text. Returns the request so the caller can react
   * to success (e.g. clear the input) without the store knowing about the form.
   */
  quickAdd(text: string): Observable<Task> {
    const projectId = this.projectId;
    if (!projectId) {
      return EMPTY;
    }
    return this.api.quickAdd(text, projectId, this.timeZone).pipe(
      tap({
        next: (task) => {
          this._error.set(null);
          if (task.projectId === this.projectId) {
            this._tasks.update((tasks) => [...tasks, task]);
            this._addedToOtherProject.set(null);
          } else {
            this._addedToOtherProject.set(task.projectId);
          }
        },
        error: (error: unknown) => {
          this._error.set(problemDetail(error, 'Could not add the task.'));
          this._addedToOtherProject.set(null);
        },
      }),
    );
  }

  /** Optimistic: the task disappears at once and comes back if the server call fails. */
  complete(task: Task): void {
    const previous = this._tasks();
    this._tasks.set(previous.filter((t) => t.id !== task.id));
    this.api.complete(task.id).subscribe({
      error: () => {
        this._tasks.set(previous);
        this._error.set(`Could not complete "${task.title}".`);
      },
    });
  }
}
