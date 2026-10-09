import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TIME_ZONE } from '../core/time-zone';
import { LabelStore } from '../label/label.store';
import { aTask } from './task.fixtures';
import { TaskListPage } from './task-list-page';

describe('TaskListPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TaskListPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TIME_ZONE, useValue: 'UTC' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  async function render() {
    const fixture = TestBed.createComponent(TaskListPage);
    fixture.componentRef.setInput('projectId', 'p1');
    await fixture.whenStable();
    http
      .expectOne('/api/projects/p1/tasks?completed=false')
      .flush([aTask({ id: 't1', title: 'Pay rent', priority: 1 })]);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;
    const input = page.querySelector<HTMLInputElement>('input[aria-label="New task"]')!;
    const submit = async (text: string) => {
      input.value = text;
      page.querySelector('form')!.dispatchEvent(new Event('submit'));
      await fixture.whenStable();
    };
    return { fixture, page, input, submit };
  }

  it('lists open tasks with their priority', async () => {
    const { page } = await render();

    expect(page.querySelector('ul')?.textContent).toContain('Pay rent');
    const badge = [...page.querySelectorAll('li span')].find((s) => s.textContent?.trim() === 'P1');
    expect(badge?.classList).toContain('bg-red-100');
  });

  it('quick-adds a task and clears the input on success', async () => {
    const { fixture, page, input, submit } = await render();

    await submit('  buy milk tomorrow ');
    const req = http.expectOne('/api/tasks/quick-add');
    expect(req.request.body.text).toBe('buy milk tomorrow');
    req.flush(aTask({ id: 't2', title: 'buy milk' }));
    await fixture.whenStable();

    expect(input.value).toBe('');
    expect(page.querySelector('ul')?.textContent).toContain('buy milk');
  });

  it('keeps the text and shows the reason when quick-add is rejected', async () => {
    const { fixture, page, input, submit } = await render();

    await submit('pay rent #hoem');
    http
      .expectOne('/api/tasks/quick-add')
      .flush(
        { detail: "No project named 'hoem'" },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(input.value).toBe('pay rent #hoem');
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("No project named 'hoem'");
  });

  it('shows label names as chips', async () => {
    TestBed.inject(LabelStore).load();
    http
      .expectOne('/api/labels')
      .flush([{ id: 'l1', name: 'errands', createdAt: '', updatedAt: '' }]);
    const fixture = TestBed.createComponent(TaskListPage);
    fixture.componentRef.setInput('projectId', 'p1');
    await fixture.whenStable();
    http
      .expectOne('/api/projects/p1/tasks?completed=false')
      .flush([aTask({ title: 'Buy milk', labelIds: ['l1'] })]);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('ul')?.textContent).toContain(
      '@errands',
    );
  });

  it('reloads labels when quick-add created a new one', async () => {
    const { fixture, submit } = await render();

    await submit('buy milk @errands');
    http
      .expectOne('/api/tasks/quick-add')
      .flush(aTask({ id: 't2', title: 'buy milk', labelIds: ['new-label'] }));
    await fixture.whenStable();

    http.expectOne('/api/labels').flush([]);
  });

  it('completes a task when its checkbox is ticked', async () => {
    const { page } = await render();

    page.querySelector<HTMLInputElement>('input[aria-label="Complete Pay rent"]')!.click();

    expect(http.expectOne('/api/tasks/t1/complete').request.method).toBe('POST');
  });

  it('shows the recurrence and unticks a recurring task after completing it', async () => {
    const fixture = TestBed.createComponent(TaskListPage);
    fixture.componentRef.setInput('projectId', 'p1');
    await fixture.whenStable();
    const recurring = aTask({
      id: 't1',
      title: 'Gym',
      dueDate: '2030-01-07',
      recurrence: 'FREQ=WEEKLY;BYDAY=MO,FR',
      recurrenceLabel: 'every Mon, Fri',
    });
    http.expectOne('/api/projects/p1/tasks?completed=false').flush([recurring]);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('ul')?.textContent).toContain('every Mon, Fri');

    const checkbox = page.querySelector<HTMLInputElement>('input[aria-label="Complete Gym"]')!;
    checkbox.click();
    expect(checkbox.checked).toBe(true);
    http.expectOne('/api/tasks/t1/complete').flush({ ...recurring, dueDate: '2030-01-11' });
    await fixture.whenStable();

    expect(page.querySelector('ul')?.textContent).toContain('Gym');
    expect(checkbox.checked).toBe(false);
  });
});
