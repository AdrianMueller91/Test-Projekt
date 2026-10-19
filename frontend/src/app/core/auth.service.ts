import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';

/**
 * Anmeldung per Session-Cookie.
 *
 * Den CSRF-Schutz erledigt Angular selbst: HttpClient liest das Cookie XSRF-TOKEN und sendet es bei
 * POST/DELETE im Header X-XSRF-TOKEN mit. Das Backend setzt das Cookie bei jedem Aufruf.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** Angemeldeter Benutzer; null = nicht angemeldet, undefined = noch nicht geprüft. */
  readonly user = signal<string | null | undefined>(undefined);

  /** Fragt den Server, ob die Session noch gültig ist. */
  check(): Observable<boolean> {
    return this.http.get<{ username: string }>('/api/me').pipe(
      tap((me) => this.user.set(me.username)),
      map(() => true),
      catchError(() => {
        this.user.set(null);
        return of(false);
      }),
    );
  }

  login(username: string, password: string): Observable<void> {
    // Spring Security erwartet klassische Formularfelder, kein JSON
    const body = new HttpParams().set('username', username).set('password', password);
    return this.http.post<void>('/api/login', body).pipe(tap(() => this.user.set(username)));
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/logout', null).pipe(tap(() => this.user.set(null)));
  }
}
