import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Läuft die Session ab, antwortet das Backend mit 401. Dann zurück zur Login-Seite.
 * Login und Session-Prüfung behandeln ihre 401 selbst und sind ausgenommen.
 */
export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const auth = inject(AuthService);
  return next(req).pipe(
    catchError((error: unknown) => {
      const ownHandling = req.url === '/api/login' || req.url === '/api/me';
      if (error instanceof HttpErrorResponse && error.status === 401 && !ownHandling) {
        auth.user.set(null);
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
