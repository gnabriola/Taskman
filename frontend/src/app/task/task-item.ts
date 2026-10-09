import { Component, computed, inject, input } from '@angular/core';
import { finalize } from 'rxjs';

import { LabelStore } from '../label/label.store';
import { ProjectStore } from '../project/project.store';
import { dueLabel } from './due-label';
import { Priority, Task } from './task.model';
import { TaskStore } from './task.store';

const PRIORITY_CLASSES: Record<Priority, string> = {
  1: 'bg-red-100 text-red-700',
  2: 'bg-orange-100 text-orange-700',
  3: 'bg-blue-100 text-blue-700',
  4: '',
};

/**
 * One task row, shared by the project page and the filter page. Used as
 * `<li appTaskItem [task]="…" [now]="…">` so the list keeps its list semantics.
 * Completes through the page's TaskStore.
 */
@Component({
  selector: 'li[appTaskItem]',
  host: { class: 'flex items-center gap-3 py-2' },
  template: `
    <input
      #checkbox
      type="checkbox"
      [attr.aria-label]="'Complete ' + task().title"
      (change)="complete(checkbox)"
      class="size-4 cursor-pointer rounded-full"
    />
    <span class="flex-1 text-sm">{{ task().title }}</span>
    @if (showProject() && projectName(); as project) {
      <span class="text-xs text-gray-500">#{{ project }}</span>
    }
    @if (task().priority < 4) {
      <span class="rounded px-1.5 text-xs font-semibold" [class]="priorityClass()"
        >P{{ task().priority }}</span
      >
    }
    @for (name of labelNames(); track name) {
      <span class="rounded bg-gray-100 px-1.5 text-xs text-gray-600">&#64;{{ name }}</span>
    }
    @if (task().recurrenceLabel; as recurrence) {
      <span class="text-xs text-gray-500" title="Repeats">↻ {{ recurrence }}</span>
    }
    @if (due(); as label) {
      <span
        class="text-xs"
        [class.text-red-600]="label.overdue"
        [class.text-gray-500]="!label.overdue"
        >{{ label.text }}</span
      >
    }
  `,
})
export class TaskItem {
  readonly task = input.required<Task>();
  /** The page's clock, so all rows agree on what "today" and "overdue" are. */
  readonly now = input.required<Date>();
  readonly showProject = input(false);

  private readonly store = inject(TaskStore);
  private readonly labels = inject(LabelStore);
  private readonly projects = inject(ProjectStore);

  protected readonly priorityClass = computed(() => PRIORITY_CLASSES[this.task().priority]);
  protected readonly labelNames = computed(() => this.labels.namesOf(this.task().labelIds));
  protected readonly projectName = computed(
    () => this.projects.findById(this.task().projectId)?.name ?? null,
  );
  protected readonly due = computed(() =>
    dueLabel(this.task().dueDate, this.task().dueTime, this.now()),
  );

  protected complete(checkbox: HTMLInputElement): void {
    this.store
      .complete(this.task())
      // A recurring task keeps its row (rows are tracked by id), so untick it again.
      .pipe(finalize(() => (checkbox.checked = false)))
      .subscribe({ error: () => undefined }); // the store shows the error
  }
}
