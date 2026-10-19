import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Note {
  id: string;
  text: string;
  createdAt: string;
}

/** Zugriff auf /api/notes. */
@Injectable({ providedIn: 'root' })
export class NoteService {
  private readonly http = inject(HttpClient);

  list(): Observable<Note[]> {
    return this.http.get<Note[]>('/api/notes');
  }

  create(text: string): Observable<Note> {
    return this.http.post<Note>('/api/notes', { text });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/notes/${encodeURIComponent(id)}`);
  }
}
