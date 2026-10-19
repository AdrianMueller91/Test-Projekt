import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from './auth.service';

/** Lässt nur angemeldete Benutzer durch, sonst geht es zur Login-Seite. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.check().pipe(map((ok) => ok || router.createUrlTree(['/login'])));
};
