import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Project } from './project.model';

@Injectable({ providedIn: 'root' })
export class ProjectApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Project[]> {
    return this.http.get<Project[]>('/api/projects');
  }

  create(name: string): Observable<Project> {
    return this.http.post<Project>('/api/projects', { name });
  }
}
