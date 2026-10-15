import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  async function submit(email: string, password: string) {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;
    page.querySelector<HTMLInputElement>('input[name="email"]')!.value = email;
    page.querySelector<HTMLInputElement>('input[name="password"]')!.value = password;
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    return { fixture, page };
  }

  it('logs in and opens the app', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await submit('ada@example.com', 'correct horse');

    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ email: 'ada@example.com', password: 'correct horse' });
    req.flush({ id: 'u1', email: 'ada@example.com', displayName: 'Ada' });

    expect(navigate).toHaveBeenCalledWith(['/']);
  });

  it('shows why logging in failed', async () => {
    const { fixture, page } = await submit('ada@example.com', 'wrong');

    http
      .expectOne('/api/auth/login')
      .flush({ detail: 'Invalid email or password' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain(
      'Invalid email or password',
    );
  });
});
