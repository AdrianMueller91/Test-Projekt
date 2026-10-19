import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';
import { LoginPage } from './login/login-page';
import { NotesPage } from './notes/notes-page';

export const routes: Routes = [
  { path: 'login', component: LoginPage, title: 'Anmelden · Test-Projekt' },
  { path: '', component: NotesPage, canActivate: [authGuard], title: 'Notizen · Test-Projekt' },
  { path: '**', redirectTo: '' },
];
