import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  it('loads projects into the sidebar', async () => {
    const fixture = TestBed.createComponent(App);
    TestBed.inject(HttpTestingController)
      .expectOne('/api/projects')
      .flush([{ id: 'p1', name: 'Home', createdAt: '', updatedAt: '' }]);
    await fixture.whenStable();

    const nav = fixture.nativeElement.querySelector('nav') as HTMLElement;
    expect(nav.textContent).toContain('Home');
  });
});
