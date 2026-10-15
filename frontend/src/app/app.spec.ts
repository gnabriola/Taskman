import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { AuthStore } from './auth/auth.store';

describe('App', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('shows no sidebar and loads nothing while logged out', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('aside')).toBeNull();
    http.expectNone('/api/projects');
  });

  it("loads the user's projects and labels once logged in", async () => {
    TestBed.inject(AuthStore).ensureChecked().subscribe();
    http.expectOne('/api/auth/me').flush({ id: 'u1', email: 'a@b.c', displayName: 'Ada' });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    http
      .expectOne('/api/projects')
      .flush([{ id: 'p1', name: 'Home', createdAt: '', updatedAt: '' }]);
    http.expectOne('/api/labels').flush([]);
    await fixture.whenStable();

    const aside = (fixture.nativeElement as HTMLElement).querySelector('aside')!;
    expect(aside.textContent).toContain('Home');
    expect(aside.textContent).toContain('Ada');
  });
});
