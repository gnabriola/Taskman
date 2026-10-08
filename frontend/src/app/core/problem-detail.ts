import { HttpErrorResponse } from '@angular/common/http';

/** The `detail` of an RFC 9457 problem response, or `fallback` if there is none. */
export function problemDetail(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const body: unknown = error.error;
    if (
      typeof body === 'object' &&
      body !== null &&
      'detail' in body &&
      typeof body.detail === 'string'
    ) {
      return body.detail;
    }
  }
  return fallback;
}
