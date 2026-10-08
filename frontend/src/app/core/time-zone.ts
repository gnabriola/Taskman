import { InjectionToken } from '@angular/core';

/** The user's IANA time zone, e.g. "Europe/Berlin". A token so tests can pin it. */
export const TIME_ZONE = new InjectionToken<string>('TIME_ZONE', {
  providedIn: 'root',
  factory: () => Intl.DateTimeFormat().resolvedOptions().timeZone,
});
