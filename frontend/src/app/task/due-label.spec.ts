import { dueLabel } from './due-label';

describe('dueLabel', () => {
  // Wednesday, Oct 7 2026, 10:00 in the browser's local time zone
  const now = new Date(2026, 9, 7, 10, 0);

  it.each([
    // dueDate,     dueTime,    text,                  overdue
    ['2026-10-07', null, 'Today', false],
    ['2026-10-07', '17:00:00', 'Today 17:00', false],
    ['2026-10-07', '08:00:00', 'Today 08:00', true],
    ['2026-10-08', null, 'Tomorrow', false],
    ['2026-10-06', null, 'Yesterday', true],
    ['2026-10-09', '09:30:00', 'Friday 09:30', false],
    ['2026-10-13', null, 'Tuesday', false],
    ['2026-10-14', null, 'Oct 14', false],
    ['2026-10-01', null, 'Oct 1', true],
    ['2027-03-15', null, 'Mar 15, 2027', false],
  ] as const)('%s %s → "%s" (overdue: %s)', (dueDate, dueTime, text, overdue) => {
    expect(dueLabel(dueDate, dueTime, now)).toEqual({ text, overdue });
  });

  it('returns null without a due date', () => {
    expect(dueLabel(null, null, now)).toBeNull();
  });
});
