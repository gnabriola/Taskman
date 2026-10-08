import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Task } from './task.model';

@Injectable({ providedIn: 'root' })
export class TaskApi {
  private readonly http = inject(HttpClient);

  listOpen(projectId: string): Observable<Task[]> {
    return this.http.get<Task[]>(`/api/projects/${projectId}/tasks`, {
      params: { completed: false },
    });
  }

  create(projectId: string, title: string): Observable<Task> {
    return this.http.post<Task>(`/api/projects/${projectId}/tasks`, { title });
  }

  complete(taskId: string): Observable<Task> {
    return this.http.post<Task>(`/api/tasks/${taskId}/complete`, null);
  }
}
