import { Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ProjectStore } from './project.store';

/** Sends the user to their first project, or explains how to create one. */
@Component({
  selector: 'app-home-page',
  template: `
    @if (store.loaded() && store.projects().length === 0) {
      <div class="p-8 text-gray-600">
        <h1 class="mb-2 text-xl font-semibold text-gray-900">No projects yet</h1>
        <p>Create your first project in the sidebar.</p>
      </div>
    }
  `,
})
export class HomePage {
  protected readonly store = inject(ProjectStore);
  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      const first = this.store.projects()[0];
      if (first) {
        this.router.navigate(['/projects', first.id], { replaceUrl: true });
      }
    });
  }
}
