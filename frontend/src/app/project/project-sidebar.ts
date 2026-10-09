import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { ProjectStore } from './project.store';

@Component({
  selector: 'app-project-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './project-sidebar.html',
})
export class ProjectSidebar {
  protected readonly store = inject(ProjectStore);
  private readonly router = inject(Router);

  protected addProject(event: Event, input: HTMLInputElement): void {
    event.preventDefault();
    const name = input.value.trim();
    if (!name) {
      return;
    }
    this.store.create(name).subscribe({
      next: (project) => {
        input.value = '';
        this.router.navigate(['/projects', project.id]);
      },
      error: () => undefined, // the store shows the error
    });
  }
}
