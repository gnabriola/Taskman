import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Label } from './label.model';
import { LabelStore } from './label.store';

function label(id: string, name: string): Label {
  return { id, name, createdAt: '', updatedAt: '' };
}

describe('LabelStore', () => {
  let store: LabelStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(LabelStore);
    http = TestBed.inject(HttpTestingController);

    store.load();
    http.expectOne('/api/labels').flush([label('l1', 'errands'), label('l2', 'work')]);
  });

  afterEach(() => http.verify());

  it('resolves names of known labels and skips unknown ids', () => {
    expect(store.namesOf(['l2', 'unknown', 'l1'])).toEqual(['work', 'errands']);
  });

  it('knows whether all ids are loaded', () => {
    expect(store.knowsAll(['l1', 'l2'])).toBe(true);
    expect(store.knowsAll(['l1', 'new'])).toBe(false);
  });

  it('adds a created label in name order', () => {
    store.create('home').subscribe();

    const req = http.expectOne('/api/labels');
    expect(req.request.body).toEqual({ name: 'home' });
    req.flush(label('l3', 'home'));
    expect(store.labels().map((l) => l.name)).toEqual(['errands', 'home', 'work']);
  });

  it("shows the server's validation message when creating fails", () => {
    store.create('two words').subscribe({ error: () => undefined });

    http.expectOne('/api/labels').flush(
      {
        detail: 'Request validation failed',
        errors: [{ field: 'name', message: 'must be a single word without spaces, @ or #' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(store.error()).toBe('name must be a single word without spaces, @ or #');
  });
});
