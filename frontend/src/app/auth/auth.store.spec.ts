import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthStore } from './auth.store';

const ada = { id: 'u1', email: 'ada@example.com', displayName: 'Ada' };

describe('AuthStore', () => {
  let store: AuthStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(AuthStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks the backend once, even with several callers', () => {
    const results: unknown[] = [];
    store.ensureChecked().subscribe((user) => results.push(user));
    store.ensureChecked().subscribe((user) => results.push(user));

    http.expectOne('/api/auth/me').flush(ada);

    expect(results).toEqual([ada, ada]);
    expect(store.user()).toEqual(ada);
    store.ensureChecked().subscribe(); // answered from memory, no request
  });

  it('treats 401 from /me as logged out', () => {
    let result: unknown = 'pending';
    store.ensureChecked().subscribe((user) => (result = user));

    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(result).toBeNull();
    expect(store.user()).toBeNull();
  });

  it('remembers the user after logging in', () => {
    store.login('ada@example.com', 'correct horse').subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ email: 'ada@example.com', password: 'correct horse' });
    req.flush(ada);
    expect(store.user()).toEqual(ada);
  });

  it('forgets the user on logout, even if the request fails', () => {
    store.login('ada@example.com', 'x').subscribe();
    http.expectOne('/api/auth/login').flush(ada);

    store.logout().subscribe();
    http.expectOne('/api/auth/logout').flush(null, { status: 500, statusText: 'Error' });

    expect(store.user()).toBeNull();
  });
});
