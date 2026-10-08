import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TIME_ZONE } from '../core/time-zone';
import { aTask } from './task.fixtures';
import { TaskStore } from './task.store';

describe('TaskStore', () => {
  let store: TaskStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TaskStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TIME_ZONE, useValue: 'Europe/Berlin' },
      ],
    });
    store = TestBed.inject(TaskStore);
    http = TestBed.inject(HttpTestingController);

    store.load('p1');
    http
      .expectOne('/api/projects/p1/tasks?completed=false')
      .flush([aTask({ id: 't1', title: 'A' }), aTask({ id: 't2', title: 'B' })]);
  });

  afterEach(() => http.verify());

  it('removes a completed task before the server answers', () => {
    store.complete(store.tasks()[0]);

    expect(store.tasks().map((t) => t.id)).toEqual(['t2']);
    http.expectOne('/api/tasks/t1/complete').flush(aTask({ id: 't1', completed: true }));
    expect(store.tasks().map((t) => t.id)).toEqual(['t2']);
  });

  it('restores the task and reports an error when completing fails', () => {
    store.complete(store.tasks()[0]);

    http
      .expectOne('/api/tasks/t1/complete')
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(store.tasks().map((t) => t.id)).toEqual(['t1', 't2']);
    expect(store.error()).toBe('Could not complete "A".');
  });

  it('quick-adds with the current project and time zone and appends the task', () => {
    store.quickAdd('C tomorrow').subscribe();

    const req = http.expectOne('/api/tasks/quick-add');
    expect(req.request.body).toEqual({
      text: 'C tomorrow',
      defaultProjectId: 'p1',
      timeZone: 'Europe/Berlin',
    });
    req.flush(aTask({ id: 't3', title: 'C' }));
    expect(store.tasks().map((t) => t.title)).toEqual(['A', 'B', 'C']);
    expect(store.addedToOtherProject()).toBeNull();
  });

  it('does not list a task that went to another project, but remembers where it went', () => {
    store.quickAdd('report #work').subscribe();

    http.expectOne('/api/tasks/quick-add').flush(aTask({ id: 't3', projectId: 'p2' }));

    expect(store.tasks().map((t) => t.id)).toEqual(['t1', 't2']);
    expect(store.addedToOtherProject()).toBe('p2');
  });

  it("shows the server's problem detail when quick-add is rejected", () => {
    store.quickAdd('pay rent #hoem').subscribe({ error: () => undefined });

    http
      .expectOne('/api/tasks/quick-add')
      .flush(
        { status: 422, detail: "No project named 'hoem'" },
        { status: 422, statusText: 'Unprocessable Content' },
      );

    expect(store.error()).toBe("No project named 'hoem'");
  });

  it('ignores a late response for a project that is no longer shown', () => {
    store.load('p2');
    store.load('p3');

    http.expectOne('/api/projects/p3/tasks?completed=false').flush([aTask({ title: 'Current' })]);
    http.expectOne('/api/projects/p2/tasks?completed=false').flush([aTask({ title: 'Stale' })]);

    expect(store.tasks().map((t) => t.title)).toEqual(['Current']);
  });
});
