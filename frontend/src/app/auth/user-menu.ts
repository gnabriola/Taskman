import { Component, inject } from '@angular/core';

import { AuthStore } from './auth.store';

/** Bottom of the sidebar: who is logged in, and a logout button. */
@Component({
  selector: 'app-user-menu',
  template: `
    @if (auth.user(); as user) {
      <div class="flex items-center justify-between gap-2 border-t border-gray-200 p-4 text-sm">
        <span class="truncate text-gray-700" [title]="user.email">{{ user.displayName }}</span>
        <button type="button" (click)="logout()" class="text-gray-500 hover:text-gray-900">
          Log out
        </button>
      </div>
    }
  `,
})
export class UserMenu {
  protected readonly auth = inject(AuthStore);

  protected logout(): void {
    // A full page load afterwards guarantees no data of this user stays in memory.
    this.auth.logout().subscribe(() => window.location.assign('/login'));
  }
}
