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

  /** Creates a task from text like "pay rent tomorrow 5pm #home p1". */
  quickAdd(text: string, defaultProjectId: string, timeZone: string): Observable<Task> {
    return this.http.post<Task>('/api/tasks/quick-add', { text, defaultProjectId, timeZone });
  }

  complete(taskId: string): Observable<Task> {
    return this.http.post<Task>(`/api/tasks/${taskId}/complete`, null);
  }
}
