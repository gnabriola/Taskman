import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, shareReplay, tap } from 'rxjs';

import { AuthApi } from './auth.api';
import { User } from './auth.model';

/** Who is logged in. `undefined` until the first check, `null` when logged out. */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApi);

  private readonly _user = signal<User | null | undefined>(undefined);
  readonly user = this._user.asReadonly();

  private check$: Observable<User | null> | null = null;

  /**
   * Asks the backend once whether the session is logged in; later calls reuse the
   * answer. Guards call this, so the first navigation waits for it.
   */
  ensureChecked(): Observable<User | null> {
    const known = this._user();
    if (known !== undefined) {
      return of(known);
    }
    this.check$ ??= this.api.me().pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          return of(null);
        }
        throw error;
      }),
      tap((user) => this._user.set(user)),
      shareReplay(1),
    );
    return this.check$;
  }

  login(email: string, password: string): Observable<User> {
    return this.api.login(email, password).pipe(tap((user) => this._user.set(user)));
  }

  register(email: string, displayName: string, password: string): Observable<User> {
    return this.api
      .register(email, displayName, password)
      .pipe(tap((user) => this._user.set(user)));
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(
      catchError(() => of(undefined)), // logged out either way
      map(() => this.clear()),
    );
  }

  /** The session ended (logout, or a 401 from the backend). */
  clear(): void {
    this._user.set(null);
    this.check$ = null;
  }
}
