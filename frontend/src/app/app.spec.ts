import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { AuthService } from './core/auth.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('zeigt den Titel', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Test-Projekt');
  });
});

describe('AuthService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('meldet sich mit Formularfeldern an, wie Spring Security es erwartet', () => {
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);

    auth.login('adrian', 'geheim&passwort').subscribe();

    const req = http.expectOne('/api/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.toString()).toBe('username=adrian&password=geheim%26passwort');
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.user()).toBe('adrian');
    http.verify();
  });

  it('setzt den Benutzer auf null, wenn die Session fehlt', () => {
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);

    let result: boolean | undefined;
    auth.check().subscribe((ok) => (result = ok));
    http.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(result).toBe(false);
    expect(auth.user()).toBeNull();
  });
});
