import { HttpErrorResponse } from '@angular/common/http';

import { problemDetail } from './problem-detail';

describe('problemDetail', () => {
  const response = (error: unknown) => new HttpErrorResponse({ error, status: 400 });

  it('prefers the first field error', () => {
    const error = response({
      detail: 'Request validation failed',
      errors: [{ field: 'name', message: 'must not be blank' }],
    });
    expect(problemDetail(error, 'fallback')).toBe('name must not be blank');
  });

  it('uses detail when there are no field errors', () => {
    expect(problemDetail(response({ detail: "No project named 'x'" }), 'fallback')).toBe(
      "No project named 'x'",
    );
  });

  it('falls back for other errors', () => {
    expect(problemDetail(response('oops'), 'fallback')).toBe('fallback');
    expect(problemDetail(new Error('network'), 'fallback')).toBe('fallback');
  });
});
