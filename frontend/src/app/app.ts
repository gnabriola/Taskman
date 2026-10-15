import { Component, effect, inject, untracked } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AuthStore } from './auth/auth.store';
import { UserMenu } from './auth/user-menu';
import { LabelSidebar } from './label/label-sidebar';
import { LabelStore } from './label/label.store';
import { ProjectSidebar } from './project/project-sidebar';
import { ProjectStore } from './project/project.store';
import { FilterBox } from './task/filter-box';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, FilterBox, ProjectSidebar, LabelSidebar, UserMenu],
  templateUrl: './app.html',
})
export class App {
  protected readonly auth = inject(AuthStore);

  constructor() {
    const projects = inject(ProjectStore);
    const labels = inject(LabelStore);
    // Load the user's data once someone is logged in.
    effect(() => {
      if (this.auth.user()) {
        untracked(() => {
          projects.load();
          labels.load();
        });
      }
    });
  }
}
