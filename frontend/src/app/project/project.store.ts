import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { ProjectApi } from './project.api';
import { Project } from './project.model';

/** App-wide project list, shared by the sidebar and the pages. */
@Injectable({ providedIn: 'root' })
export class ProjectStore {
  private readonly api = inject(ProjectApi);

  private readonly _projects = signal<Project[]>([]);
  private readonly _loaded = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly projects = this._projects.asReadonly();
  readonly loaded = this._loaded.asReadonly();
  readonly error = this._error.asReadonly();

  load(): void {
    this.api.list().subscribe({
      next: (projects) => {
        this._projects.set(projects);
        this._loaded.set(true);
        this._error.set(null);
      },
      error: () => this._error.set('Could not load projects.'),
    });
  }

  create(name: string): Observable<Project> {
    return this.api.create(name).pipe(
      tap({
        next: (project) => {
          this._projects.update((projects) => [...projects, project]);
          this._error.set(null);
        },
        error: () => this._error.set('Could not create the project.'),
      }),
    );
  }

  findById(id: string): Project | undefined {
    return this._projects().find((project) => project.id === id);
  }
}
