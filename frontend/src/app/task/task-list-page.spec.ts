import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TaskListPage } from './task-list-page';

describe('TaskListPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TaskListPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  async function render() {
    const fixture = TestBed.createComponent(TaskListPage);
    fixture.componentRef.setInput('projectId', 'p1');
    await fixture.whenStable();
    http.expectOne('/api/projects/p1/tasks?completed=false').flush([
      {
        id: 't1',
        projectId: 'p1',
        title: 'Pay rent',
        description: null,
        completed: false,
        completedAt: null,
        createdAt: '',
        updatedAt: '',
      },
    ]);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('lists open tasks of the project', async () => {
    const page = await render();

    expect(page.querySelector('ul')?.textContent).toContain('Pay rent');
  });

  it('adds a task from the form', async () => {
    const page = await render();
    const input = page.querySelector<HTMLInputElement>('input[aria-label="New task title"]')!;

    input.value = '  Buy milk ';
    page.querySelector('form')!.dispatchEvent(new Event('submit'));

    const req = http.expectOne('/api/projects/p1/tasks');
    expect(req.request.body).toEqual({ title: 'Buy milk' });
    expect(input.value).toBe('');
  });

  it('completes a task when its checkbox is ticked', async () => {
    const page = await render();

    page.querySelector<HTMLInputElement>('input[aria-label="Complete Pay rent"]')!.click();

    expect(http.expectOne('/api/tasks/t1/complete').request.method).toBe('POST');
  });
});
