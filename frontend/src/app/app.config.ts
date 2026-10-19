import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeDe from '@angular/common/locales/de';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { unauthorizedInterceptor } from './core/unauthorized.interceptor';

registerLocaleData(localeDe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // XSRF-Schutz ist in provideHttpClient standardmäßig aktiv (Cookie XSRF-TOKEN → Header X-XSRF-TOKEN)
    provideHttpClient(withInterceptors([unauthorizedInterceptor])),
    { provide: LOCALE_ID, useValue: 'de' },
  ],
};
