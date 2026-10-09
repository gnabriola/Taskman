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

  it('sends quick-add text with the default project and time zone', () => {
    api.quickAdd('pay rent tomorrow', 'p1', 'Europe/Berlin').subscribe();

    const req = http.expectOne('/api/tasks/quick-add');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      text: 'pay rent tomorrow',
      defaultProjectId: 'p1',
      timeZone: 'Europe/Berlin',
    });
  });

  it('completes a task, sending the time zone for recurring tasks', () => {
    api.complete('t1', 'Europe/Berlin').subscribe();

    const req = http.expectOne('/api/tasks/t1/complete');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ timeZone: 'Europe/Berlin' });
  });
});
