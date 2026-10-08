import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Task } from './task.model';
import { TaskStore } from './task.store';

function task(id: string, title: string): Task {
  return {
    id,
    projectId: 'p1',
    title,
    description: null,
    completed: false,
    completedAt: null,
    createdAt: '',
    updatedAt: '',
  };
}

describe('TaskStore', () => {
  let store: TaskStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TaskStore, provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(TaskStore);
    http = TestBed.inject(HttpTestingController);

    store.load('p1');
    http
      .expectOne('/api/projects/p1/tasks?completed=false')
      .flush([task('t1', 'A'), task('t2', 'B')]);
  });

  afterEach(() => http.verify());

  it('removes a completed task before the server answers', () => {
    store.complete(store.tasks()[0]);

    expect(store.tasks().map((t) => t.id)).toEqual(['t2']);
    http.expectOne('/api/tasks/t1/complete').flush({ ...task('t1', 'A'), completed: true });
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

  it('appends an added task', () => {
    store.add('C');

    http.expectOne('/api/projects/p1/tasks').flush(task('t3', 'C'));
    expect(store.tasks().map((t) => t.title)).toEqual(['A', 'B', 'C']);
  });

  it('ignores a late response for a project that is no longer shown', () => {
    store.load('p2');
    store.load('p3');

    http.expectOne('/api/projects/p3/tasks?completed=false').flush([task('t9', 'Current')]);
    http.expectOne('/api/projects/p2/tasks?completed=false').flush([task('t8', 'Stale')]);

    expect(store.tasks().map((t) => t.title)).toEqual(['Current']);
  });
});
