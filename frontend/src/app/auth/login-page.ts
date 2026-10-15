import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { problemDetail } from '../core/problem-detail';
import { AuthStore } from './auth.store';

@Component({
  selector: 'app-login-page',
  imports: [RouterLink],
  template: `
    <section class="mx-auto mt-24 flex max-w-sm flex-col gap-6 p-8">
      <h1 class="text-2xl font-semibold">Log in to Taskman</h1>
      <form (submit)="submit($event)" class="flex flex-col gap-3">
        <label class="flex flex-col gap-1 text-sm">
          Email
          <input name="email" type="email" required autocomplete="email" [class]="inputClass" />
        </label>
        <label class="flex flex-col gap-1 text-sm">
          Password
          <input
            name="password"
            type="password"
            required
            autocomplete="current-password"
            [class]="inputClass"
          />
        </label>
        @if (error(); as message) {
          <p role="alert" class="text-sm text-red-600">{{ message }}</p>
        }
        <button type="submit" [disabled]="busy()" [class]="buttonClass">Log in</button>
      </form>
      <p class="text-sm text-gray-600">
        No account yet? <a routerLink="/register" class="underline">Create one</a>
      </p>
    </section>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly inputClass =
    'rounded border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none';
  protected readonly buttonClass =
    'rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50';

  protected submit(event: Event): void {
    event.preventDefault();
    const form = new FormData(event.target as HTMLFormElement);
    this.busy.set(true);
    this.auth.login(String(form.get('email')), String(form.get('password'))).subscribe({
      next: () => this.router.navigate(['/']),
      error: (error: unknown) => {
        this.error.set(problemDetail(error, 'Could not log in.'));
        this.busy.set(false);
      },
    });
  }
}
