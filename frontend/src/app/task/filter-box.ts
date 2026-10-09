import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

/** Sidebar input that opens the filter page for a query like "today | overdue". */
@Component({
  selector: 'app-filter-box',
  template: `
    <form (submit)="open($event, queryInput)" class="px-4 pt-4">
      <input
        #queryInput
        type="search"
        placeholder="Filter, e.g. today | overdue"
        aria-label="Filter tasks"
        class="w-full rounded border border-gray-300 bg-white px-2 py-1.5 font-mono text-xs placeholder:font-sans placeholder:text-gray-500 focus:border-gray-500 focus:outline-none"
      />
    </form>
  `,
})
export class FilterBox {
  private readonly router = inject(Router);

  protected open(event: Event, input: HTMLInputElement): void {
    event.preventDefault();
    const q = input.value.trim();
    if (q) {
      this.router.navigate(['/filter'], { queryParams: { q } });
    }
  }
}
