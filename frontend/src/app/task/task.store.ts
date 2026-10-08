import { Injectable, inject, signal } from '@angular/core';

import { TaskApi } from './task.api';
import { Task } from './task.model';

/** Open tasks of one project. Provided per page, so each page gets its own state. */
@Injectable()
export class TaskStore {
  private readonly api = inject(TaskApi);

  private projectId: string | null = null;

  private readonly _tasks = signal<Task[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly tasks = this._tasks.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  load(projectId: string): void {
    this.projectId = projectId;
    this._tasks.set([]);
    this._loading.set(true);
    this._error.set(null);
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

  add(title: string): void {
    const projectId = this.projectId;
    if (!projectId) {
      return;
    }
    this.api.create(projectId, title).subscribe({
      next: (task) => {
        if (this.projectId === projectId) {
          this._tasks.update((tasks) => [...tasks, task]);
        }
      },
      error: () => this._error.set('Could not add the task.'),
    });
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
