import { Component, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';

import { TaskItem } from './task-item';
import { TaskStore } from './task.store';

/** Open tasks matching a filter query, at /filter?q=… so filters can be bookmarked. */
@Component({
  selector: 'app-filter-page',
  imports: [TaskItem],
  providers: [TaskStore],
  template: `
    <section class="mx-auto flex max-w-2xl flex-col gap-6 p-8">
      <div>
        <h1 class="text-2xl font-semibold">Filter</h1>
        <p class="mt-1 font-mono text-sm text-gray-600">{{ q() }}</p>
      </div>

      @if (store.error(); as error) {
        <p role="alert" class="text-sm text-red-600">{{ error }}</p>
      } @else if (store.loading()) {
        <p class="text-sm text-gray-500">Loading…</p>
      } @else {
        <ul class="divide-y divide-gray-100">
          @for (task of store.tasks(); track task.id) {
            <li appTaskItem [task]="task" [now]="now()" [showProject]="true"></li>
          } @empty {
            <li class="py-2 text-sm text-gray-500">No tasks match.</li>
          }
        </ul>
      }
    </section>
  `,
})
export class FilterPage {
  /** Bound from the ?q= query parameter. */
  readonly q = input('');

  protected readonly store = inject(TaskStore);

  /** Refreshed every minute so "Today" and overdue markers stay correct. */
  protected readonly now = signal(new Date());

  constructor() {
    effect(() => {
      const query = this.q();
      untracked(() => this.store.loadFilter(query));
    });
    const timer = setInterval(() => this.now.set(new Date()), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }
}
