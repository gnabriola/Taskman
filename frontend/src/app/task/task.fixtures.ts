import { Task } from './task.model';

/** Test helper: a task with sensible defaults. */
export function aTask(overrides: Partial<Task> = {}): Task {
  return {
    id: 't1',
    projectId: 'p1',
    title: 'Task',
    description: null,
    dueDate: null,
    dueTime: null,
    priority: 4,
    recurrence: null,
    recurrenceFromCompletion: false,
    recurrenceLabel: null,
    labelIds: [],
    completed: false,
    completedAt: null,
    createdAt: '',
    updatedAt: '',
    ...overrides,
  };
}
