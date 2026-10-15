import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthStore } from './auth.store';

/**
 * Pages for logged-in users; others go to /login. If the backend cannot be reached at
 * all, the login page is still better than a blank screen.
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const toLogin = router.createUrlTree(['/login']);
  return inject(AuthStore)
    .ensureChecked()
    .pipe(
      map((user) => (user ? true : toLogin)),
      catchError(() => of(toLogin)),
    );
};

/** Login and register pages; logged-in users go to the app instead. */
export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthStore)
    .ensureChecked()
    .pipe(
      map((user) => (user ? router.createUrlTree(['/']) : true)),
      catchError(() => of(true)),
    );
};
