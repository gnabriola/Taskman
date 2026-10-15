import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';

import { authGuard, guestGuard } from './auth.guard';

describe('auth guards', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  function run(guard: typeof authGuard): Promise<boolean | UrlTree> {
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;
    return firstValueFrom(result);
  }

  it('sends logged-out users to /login', async () => {
    const result = run(authGuard);
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(String(await result)).toBe('/login');
  });

  it('lets logged-in users in', async () => {
    const result = run(authGuard);
    http.expectOne('/api/auth/me').flush({ id: 'u1', email: 'a@b.c', displayName: 'Ada' });

    expect(await result).toBe(true);
  });

  it('shows the login page instead of a blank screen when the backend is unreachable', async () => {
    const app = run(authGuard);
    http.expectOne('/api/auth/me').flush(null, { status: 504, statusText: 'Gateway Timeout' });
    expect(String(await app)).toBe('/login');

    const login = run(guestGuard);
    http.expectOne('/api/auth/me').flush(null, { status: 504, statusText: 'Gateway Timeout' });
    expect(await login).toBe(true);
  });

  it('keeps logged-in users away from the login page', async () => {
    const result = run(guestGuard);
    http.expectOne('/api/auth/me').flush({ id: 'u1', email: 'a@b.c', displayName: 'Ada' });

    expect(String(await result)).toBe('/');
  });
});
