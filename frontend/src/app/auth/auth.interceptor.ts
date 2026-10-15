import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthStore } from './auth.store';

/**
 * A 401 from the API means the session is gone (expired, or the backend restarted):
 * forget the user and go to the login page. Auth endpoints are excluded, because a 401
 * there is an answer (wrong password, not logged in yet), not a lost session.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !request.url.startsWith('/api/auth/')
      ) {
        auth.clear();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
