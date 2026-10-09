import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { LabelStore } from './label.store';

@Component({
  selector: 'app-label-sidebar',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <section class="flex flex-col gap-4 p-4" aria-label="Labels">
      <h2 class="px-2 text-xs font-semibold tracking-wide text-gray-500 uppercase">Labels</h2>

      <ul class="flex flex-col gap-0.5">
        @for (label of store.labels(); track label.id) {
          <li>
            <a
              routerLink="/filter"
              [queryParams]="{ q: '@' + label.name }"
              routerLinkActive="bg-gray-200 font-medium"
              [routerLinkActiveOptions]="{ exact: true }"
              class="block truncate rounded px-2 py-1.5 text-sm hover:bg-gray-100"
              >&#64;{{ label.name }}</a
            >
          </li>
        }
      </ul>

      <form (submit)="addLabel($event, nameInput)">
        <input
          #nameInput
          type="text"
          maxlength="60"
          placeholder="+ New label"
          aria-label="New label name"
          class="w-full rounded border border-transparent bg-transparent px-2 py-1.5 text-sm placeholder:text-gray-500 focus:border-gray-300 focus:bg-white focus:outline-none"
        />
      </form>

      @if (store.error(); as error) {
        <p role="alert" class="px-2 text-sm text-red-600">{{ error }}</p>
      }
    </section>
  `,
})
export class LabelSidebar {
  protected readonly store = inject(LabelStore);

  protected addLabel(event: Event, input: HTMLInputElement): void {
    event.preventDefault();
    const name = input.value.trim().replace(/^@/, '');
    if (!name) {
      return;
    }
    this.store.create(name).subscribe({
      next: () => (input.value = ''),
      error: () => undefined, // the store shows the error; the text stays for fixing
    });
  }
}
