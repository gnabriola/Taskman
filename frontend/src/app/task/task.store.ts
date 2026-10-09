import { Injectable, inject, signal } from '@angular/core';
import { EMPTY, Observable, tap } from 'rxjs';

import { problemDetail } from '../core/problem-detail';
import { TIME_ZONE } from '../core/time-zone';
import { TaskApi } from './task.api';
import { Task } from './task.model';

/**
 * Open tasks of one project, or of a filter query. Provided per page, so each page
 * gets its own state.
 */
@Injectable()
export class TaskStore {
  private readonly api = inject(TaskApi);
  private readonly timeZone = inject(TIME_ZONE);

  /** Set on a project page; quick-add needs it as the default project. */
  private projectId: string | null = null;
  /** What is currently shown, e.g. "project:<id>" or "filter:today"; guards against stale responses. */
  private source: string | null = null;

  private readonly _tasks = signal<Task[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _addedToOtherProject = signal<string | null>(null);

  readonly tasks = this._tasks.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  /** Id of the project the last quick-added task went to, if it is not this one. */
  readonly addedToOtherProject = this._addedToOtherProject.asReadonly();

  /** Loads the open tasks of a project. */
  load(projectId: string): void {
    this.projectId = projectId;
    this.fetch(`project:${projectId}`, this.api.listOpen(projectId));
  }

  /** Loads the open tasks matching a filter query; syntax errors show the server's message. */
  loadFilter(query: string): void {
    this.projectId = null;
    this.fetch(`filter:${query}`, this.api.filter(query, this.timeZone));
  }

  private fetch(source: string, request: Observable<Task[]>): void {
    this.source = source;
    this._tasks.set([]);
    this._loading.set(true);
    this._error.set(null);
    this._addedToOtherProject.set(null);
    request.subscribe({
      next: (tasks) => {
        // Ignore a slow response for a page the user has already navigated away from.
        if (this.source === source) {
          this._tasks.set(tasks);
          this._loading.set(false);
        }
      },
      error: (error: unknown) => {
        if (this.source === source) {
          this._error.set(problemDetail(error, 'Could not load tasks.'));
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

  /**
   * A one-off task disappears at once (optimistic) and comes back if the server call
   * fails. A recurring task stays and is replaced by the server's answer, which carries
   * its next due date; that date is computed on the server, so there is nothing to
   * show optimistically.
   */
  complete(task: Task): Observable<Task> {
    const previous = this._tasks();
    if (!task.recurrence) {
      this._tasks.set(previous.filter((t) => t.id !== task.id));
    }
    return this.api.complete(task.id, this.timeZone).pipe(
      tap({
        next: (updated) => {
          if (updated.recurrence) {
            this._tasks.update((tasks) => tasks.map((t) => (t.id === updated.id ? updated : t)));
          }
        },
        error: () => {
          this._tasks.set(previous);
          this._error.set(`Could not complete "${task.title}".`);
        },
      }),
    );
  }
}
