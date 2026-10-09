import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Label } from './label.model';

@Injectable({ providedIn: 'root' })
export class LabelApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Label[]> {
    return this.http.get<Label[]>('/api/labels');
  }

  create(name: string): Observable<Label> {
    return this.http.post<Label>('/api/labels', { name });
  }
}
