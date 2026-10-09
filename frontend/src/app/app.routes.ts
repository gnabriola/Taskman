import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./project/home-page').then((m) => m.HomePage) },
  {
    path: 'projects/:projectId',
    loadComponent: () => import('./task/task-list-page').then((m) => m.TaskListPage),
  },
  { path: 'filter', loadComponent: () => import('./task/filter-page').then((m) => m.FilterPage) },
  { path: '**', redirectTo: '' },
];
