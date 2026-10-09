import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LabelSidebar } from './label/label-sidebar';
import { LabelStore } from './label/label.store';
import { ProjectSidebar } from './project/project-sidebar';
import { ProjectStore } from './project/project.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ProjectSidebar, LabelSidebar],
  templateUrl: './app.html',
})
export class App {
  constructor() {
    inject(ProjectStore).load();
    inject(LabelStore).load();
  }
}
