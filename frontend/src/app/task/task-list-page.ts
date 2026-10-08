import { Component, computed, effect, inject, input, untracked } from '@angular/core';

import { ProjectStore } from '../project/project.store';
import { AddTaskForm } from './add-task-form';
import { TaskStore } from './task.store';

@Component({
  selector: 'app-task-list-page',
  imports: [AddTaskForm],
  providers: [TaskStore],
  templateUrl: './task-list-page.html',
})
export class TaskListPage {
  /** Bound from the :projectId route parameter. */
  readonly projectId = input.required<string>();

  protected readonly store = inject(TaskStore);
  private readonly projects = inject(ProjectStore);

  protected readonly projectName = computed(
    () => this.projects.projects().find((p) => p.id === this.projectId())?.name ?? '',
  );

  constructor() {
    effect(() => {
      const projectId = this.projectId();
      untracked(() => this.store.load(projectId));
    });
  }
}
