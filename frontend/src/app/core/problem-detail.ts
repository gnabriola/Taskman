import { HttpErrorResponse } from '@angular/common/http';

/**
 * A readable message from an RFC 9457 problem response: the first field error if
 * there is one (e.g. "name must be a single word…"), else `detail`, else `fallback`.
 */
export function problemDetail(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }
  const body: unknown = error.error;
  if (typeof body !== 'object' || body === null) {
    return fallback;
  }
  if ('errors' in body && Array.isArray(body.errors) && body.errors.length > 0) {
    const first: unknown = body.errors[0];
    if (
      typeof first === 'object' &&
      first !== null &&
      'field' in first &&
      'message' in first &&
      typeof first.field === 'string' &&
      typeof first.message === 'string'
    ) {
      return `${first.field} ${first.message}`;
    }
  }
  if ('detail' in body && typeof body.detail === 'string') {
    return body.detail;
  }
  return fallback;
}
