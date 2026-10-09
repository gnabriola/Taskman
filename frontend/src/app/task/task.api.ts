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

  /** Open tasks matching a filter query such as "(today | overdue) & #work". */
  filter(query: string, timeZone: string): Observable<Task[]> {
    return this.http.get<Task[]>('/api/tasks', { params: { filter: query, timeZone } });
  }

  /** Creates a task from text like "pay rent tomorrow 5pm #home p1". */
  quickAdd(text: string, defaultProjectId: string, timeZone: string): Observable<Task> {
    return this.http.post<Task>('/api/tasks/quick-add', { text, defaultProjectId, timeZone });
  }

  /**
   * Completes a task. A recurring task stays open and comes back with its next due date;
   * the time zone decides what "today" is for that.
   */
  complete(taskId: string, timeZone: string): Observable<Task> {
    return this.http.post<Task>(`/api/tasks/${taskId}/complete`, { timeZone });
  }
}
