import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Note, NoteService } from './note.service';

@Component({
  selector: 'app-notes-page',
  imports: [ReactiveFormsModule, DatePipe],
  template: `
    <section class="card">
      <h2>Neue Notiz</h2>
      <form [formGroup]="form" (ngSubmit)="add()" class="new-note">
        <input formControlName="text" placeholder="Was gibt es Neues?" maxlength="500" />
        <button type="submit" [disabled]="form.invalid || busy()">Speichern</button>
      </form>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
    </section>

    <section class="card">
      <h2>Notizen</h2>
      @for (note of notes(); track note.id) {
        <article class="note">
          <div>
            <p>{{ note.text }}</p>
            <small>{{ note.createdAt | date: 'dd.MM.yyyy HH:mm' }}</small>
          </div>
          <button type="button" class="secondary" (click)="remove(note)" [attr.aria-label]="'Notiz löschen'">
            Löschen
          </button>
        </article>
      } @empty {
        <p class="muted">Noch keine Notizen. Die erste landet in Postgres.</p>
      }
    </section>
  `,
  styles: `
    .new-note { display: flex; gap: 0.5rem; }
    .new-note input { flex: 1; }
    .note { display: flex; justify-content: space-between; gap: 1rem; padding: 0.75rem 0; border-top: 1px solid var(--line); }
    .note:first-of-type { border-top: none; }
    .note p { margin: 0 0 0.25rem; white-space: pre-wrap; overflow-wrap: anywhere; }
    small, .muted { color: var(--muted); }
  `,
})
export class NotesPage implements OnInit {
  private readonly noteService = inject(NoteService);

  protected readonly notes = signal<Note[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    text: ['', [Validators.required, Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.load();
  }

  protected add(): void {
    this.busy.set(true);
    this.error.set(null);
    this.noteService.create(this.form.getRawValue().text).subscribe({
      next: (note) => {
        this.notes.update((list) => [note, ...list]);
        this.form.reset();
        this.busy.set(false);
      },
      error: () => {
        this.error.set('Speichern fehlgeschlagen.');
        this.busy.set(false);
      },
    });
  }

  protected remove(note: Note): void {
    this.noteService.delete(note.id).subscribe({
      next: () => this.notes.update((list) => list.filter((n) => n.id !== note.id)),
      error: () => this.error.set('Löschen fehlgeschlagen.'),
    });
  }

  private load(): void {
    this.noteService.list().subscribe({
      next: (notes) => this.notes.set(notes),
      error: () => this.error.set('Notizen konnten nicht geladen werden.'),
    });
  }
}
