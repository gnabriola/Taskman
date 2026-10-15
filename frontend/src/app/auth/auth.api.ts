import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { User } from './auth.model';

/**
 * Session-based auth: the backend keeps the session, the browser keeps an HttpOnly
 * cookie. Angular's HttpClient sends the CSRF token (XSRF-TOKEN cookie -> X-XSRF-TOKEN
 * header) on POSTs automatically.
 */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);

  me(): Observable<User> {
    return this.http.get<User>('/api/auth/me');
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<User>('/api/auth/login', { email, password });
  }

  register(email: string, displayName: string, password: string): Observable<User> {
    return this.http.post<User>('/api/auth/register', { email, displayName, password });
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null);
  }
}
