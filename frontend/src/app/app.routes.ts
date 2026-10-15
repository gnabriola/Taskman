import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/register-page').then((m) => m.RegisterPage),
  },
  {
    // Everything else requires a logged-in user.
    path: '',
    canActivateChild: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./project/home-page').then((m) => m.HomePage) },
      {
        path: 'projects/:projectId',
        loadComponent: () => import('./task/task-list-page').then((m) => m.TaskListPage),
      },
      {
        path: 'filter',
        loadComponent: () => import('./task/filter-page').then((m) => m.FilterPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
