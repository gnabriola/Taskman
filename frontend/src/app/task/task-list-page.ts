import {
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';

import { ProjectStore } from '../project/project.store';
import { AddTaskForm } from './add-task-form';
import { DueLabel, dueLabel } from './due-label';
import { Priority, Task } from './task.model';
import { TaskStore } from './task.store';

const PRIORITY_CLASSES: Record<Priority, string> = {
  1: 'bg-red-100 text-red-700',
  2: 'bg-orange-100 text-orange-700',
  3: 'bg-blue-100 text-blue-700',
  4: '',
};

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
  protected readonly priorityClasses = PRIORITY_CLASSES;
  private readonly projects = inject(ProjectStore);
  private readonly form = viewChild.required(AddTaskForm);

  /** Refreshed every minute so "Today" and overdue markers stay correct. */
  private readonly now = signal(new Date());

  protected readonly projectName = computed(
    () => this.projects.projects().find((p) => p.id === this.projectId())?.name ?? '',
  );

  protected readonly addedNotice = computed(() => {
    const otherId = this.store.addedToOtherProject();
    if (!otherId) {
      return null;
    }
    const name = this.projects.projects().find((p) => p.id === otherId)?.name;
    return name ? `Added to ${name}` : 'Added to another project';
  });

  constructor() {
    effect(() => {
      const projectId = this.projectId();
      untracked(() => this.store.load(projectId));
    });
    const timer = setInterval(() => this.now.set(new Date()), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected add(text: string): void {
    this.store.quickAdd(text).subscribe({
      next: () => this.form().clear(),
      // The store already shows the error; the text stays in the input so it can be fixed.
      error: () => undefined,
    });
  }

  protected due(task: Task): DueLabel | null {
    return dueLabel(task.dueDate, task.dueTime, this.now());
  }
}
