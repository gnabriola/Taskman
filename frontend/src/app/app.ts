import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { ProjectSidebar } from './project/project-sidebar';
import { ProjectStore } from './project/project.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ProjectSidebar],
  templateUrl: './app.html',
})
export class App {
  constructor() {
    inject(ProjectStore).load();
  }
}
