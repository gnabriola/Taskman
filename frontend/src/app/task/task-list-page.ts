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

import { LabelStore } from '../label/label.store';
import { ProjectStore } from '../project/project.store';
import { AddTaskForm } from './add-task-form';
import { TaskItem } from './task-item';
import { TaskStore } from './task.store';

@Component({
  selector: 'app-task-list-page',
  imports: [AddTaskForm, TaskItem],
  providers: [TaskStore],
  templateUrl: './task-list-page.html',
})
export class TaskListPage {
  /** Bound from the :projectId route parameter. */
  readonly projectId = input.required<string>();

  protected readonly store = inject(TaskStore);
  private readonly projects = inject(ProjectStore);
  private readonly labels = inject(LabelStore);
  private readonly form = viewChild.required(AddTaskForm);

  /** Refreshed every minute so "Today" and overdue markers stay correct. */
  protected readonly now = signal(new Date());

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
      next: (task) => {
        this.form().clear();
        // Quick-add creates unknown @labels on the server; fetch them to show their names.
        if (!this.labels.knowsAll(task.labelIds)) {
          this.labels.load();
        }
      },
      // The store already shows the error; the text stays in the input so it can be fixed.
      error: () => undefined,
    });
  }
}
