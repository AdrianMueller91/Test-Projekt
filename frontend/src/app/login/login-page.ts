import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  template: `
    <section class="card login">
      <h2>Anmelden</h2>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label>
          Benutzer
          <input formControlName="username" autocomplete="username" required />
        </label>
        <label>
          Passwort
          <input formControlName="password" type="password" autocomplete="current-password" required />
        </label>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
        <button type="submit" [disabled]="form.invalid || busy()">Anmelden</button>
      </form>
    </section>
  `,
  styles: `
    .login { max-width: 22rem; margin: 3rem auto; }
    form { display: grid; gap: 0.75rem; }
    label { display: grid; gap: 0.25rem; font-size: 0.9rem; }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected submit(): void {
    const { username, password } = this.form.getRawValue();
    this.busy.set(true);
    this.error.set(null);
    this.auth.login(username, password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (e: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(
          e.status === 429
            ? 'Zu viele Fehlversuche. Bitte in 15 Minuten erneut versuchen.'
            : 'Benutzer oder Passwort falsch.',
        );
      },
    });
  }
}
