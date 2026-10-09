import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TIME_ZONE } from '../core/time-zone';
import { ProjectStore } from '../project/project.store';
import { FilterPage } from './filter-page';
import { aTask } from './task.fixtures';

describe('FilterPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FilterPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TIME_ZONE, useValue: 'UTC' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  async function render(q: string) {
    const fixture = TestBed.createComponent(FilterPage);
    fixture.componentRef.setInput('q', q);
    await fixture.whenStable();
    return fixture;
  }

  it('lists matching tasks with their project', async () => {
    TestBed.inject(ProjectStore).load();
    http
      .expectOne('/api/projects')
      .flush([{ id: 'p1', name: 'Work', createdAt: '', updatedAt: '' }]);
    const fixture = await render('#work');

    http
      .expectOne((r) => r.url === '/api/tasks' && r.params.get('filter') === '#work')
      .flush([aTask({ title: 'Write report', projectId: 'p1' })]);
    await fixture.whenStable();

    const list = (fixture.nativeElement as HTMLElement).querySelector('ul')!;
    expect(list.textContent).toContain('Write report');
    expect(list.textContent).toContain('#Work');
  });

  it("shows the server's message for an invalid filter", async () => {
    const fixture = await render('#nope');

    http
      .expectOne((r) => r.url === '/api/tasks')
      .flush(
        { detail: "No project named 'nope'" },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("No project named 'nope'");
  });

  it('reloads when the query changes', async () => {
    const fixture = await render('today');
    http.expectOne((r) => r.params.get('filter') === 'today').flush([]);

    fixture.componentRef.setInput('q', 'overdue');
    await fixture.whenStable();

    http.expectOne((r) => r.params.get('filter') === 'overdue').flush([]);
  });
});
