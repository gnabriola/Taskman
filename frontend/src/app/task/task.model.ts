/** 1 is the highest priority, 4 the default. */
export type Priority = 1 | 2 | 3 | 4;

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description: string | null;
  /** ISO date (yyyy-mm-dd), a wall-clock date in the user's time zone. */
  dueDate: string | null;
  /** ISO time (HH:mm:ss); only set together with dueDate. */
  dueTime: string | null;
  priority: Priority;
  /** RRULE subset, e.g. "FREQ=WEEKLY;BYDAY=MO"; null for a one-off task. */
  recurrence: string | null;
  /** Todoist's "every!": the next occurrence counts from the completion date. */
  recurrenceFromCompletion: boolean;
  /** Readable form of the recurrence, e.g. "every Monday". */
  recurrenceLabel: string | null;
  labelIds: string[];
  completed: boolean;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
