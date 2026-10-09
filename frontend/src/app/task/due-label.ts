export interface DueLabel {
  text: string;
  overdue: boolean;
}

const DAY_MS = 24 * 60 * 60 * 1000;
const weekday = new Intl.DateTimeFormat('en-US', { weekday: 'long', timeZone: 'UTC' });
const monthDay = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  timeZone: 'UTC',
});

/**
 * Human-readable due date relative to `now` (in the browser's time zone):
 * "Today", "Tomorrow 17:00", "Friday", "Oct 31", "Mar 15, 2027".
 */
export function dueLabel(
  dueDate: string | null,
  dueTime: string | null,
  now: Date,
): DueLabel | null {
  if (!dueDate) {
    return null;
  }
  const [year, month, day] = dueDate.split('-').map(Number);
  // Compare calendar days as UTC midnights, so daylight-saving changes cannot skew the difference.
  const due = Date.UTC(year, month - 1, day);
  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  const days = Math.round((due - today) / DAY_MS);

  let text: string;
  if (days === 0) {
    text = 'Today';
  } else if (days === 1) {
    text = 'Tomorrow';
  } else if (days === -1) {
    text = 'Yesterday';
  } else if (days > 1 && days < 7) {
    text = weekday.format(due);
  } else if (year === now.getFullYear()) {
    text = monthDay.format(due);
  } else {
    text = `${monthDay.format(due)}, ${year}`;
  }

  const time = dueTime?.slice(0, 5) ?? null;
  if (time) {
    text += ` ${time}`;
  }
  const nowTime = `${pad(now.getHours())}:${pad(now.getMinutes())}`;
  const overdue = days < 0 || (days === 0 && time !== null && time < nowTime);
  return { text, overdue };
}

function pad(value: number): string {
  return value.toString().padStart(2, '0');
}
