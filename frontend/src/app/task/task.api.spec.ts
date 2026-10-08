import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TaskApi } from './task.api';

describe('TaskApi', () => {
  let api: TaskApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(TaskApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests only open tasks of a project', () => {
    api.listOpen('p1').subscribe();

    const req = http.expectOne('/api/projects/p1/tasks?completed=false');
    expect(req.request.method).toBe('GET');
  });

  it('creates a task in a project', () => {
    api.create('p1', 'Pay rent').subscribe();

    const req = http.expectOne('/api/projects/p1/tasks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Pay rent' });
  });

  it('completes a task', () => {
    api.complete('t1').subscribe();

    expect(http.expectOne('/api/tasks/t1/complete').request.method).toBe('POST');
  });
});
