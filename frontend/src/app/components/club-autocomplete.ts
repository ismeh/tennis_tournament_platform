import { Component, EventEmitter, forwardRef, inject, Input, OnDestroy, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subject, of, takeUntil } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { ClubService } from '../data/services/club.service';
import { ClubResponse } from '../data/interfaces/club.model';

@Component({
  selector: 'app-club-autocomplete',
  standalone: true,
  imports: [CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ClubAutocompleteComponent),
      multi: true
    }
  ],
  template: `
    <div class="relative w-full">
      <input
        type="text"
        [value]="value"
        (input)="onInput($event)"
        (focus)="onFocus()"
        (blur)="onBlur()"
        class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
        [placeholder]="placeholder"
        [disabled]="disabled"
      />

      @if ((suggestions().length > 0 || (value.trim().length >= 2 && !hasExactMatch())) && showDropdown()) {
        <div class="absolute z-50 mt-1 w-full overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-lg max-h-60 overflow-y-auto">
          @for (club of suggestions(); track club.id) {
            <button
              type="button"
              (mousedown)="selectClub(club)"
              class="w-full px-3 py-2 text-left text-sm hover:bg-primary-50 transition-colors border-b border-neutral-100 last:border-0"
            >
              <span class="font-medium text-neutral-800">{{ club.name }}</span>
            </button>
          }
          @if (value.trim().length >= 2 && !hasExactMatch()) {
            <button
              type="button"
              (mousedown)="createClub()"
              class="w-full px-3 py-2 text-left text-sm hover:bg-accent-50 transition-colors text-accent-700 font-medium"
            >
              + Crear "{{ value.trim() }}"
            </button>
          }
        </div>
      }
    </div>
  `
})
export class ClubAutocompleteComponent implements OnInit, OnDestroy, ControlValueAccessor {
  private readonly clubService = inject(ClubService);

  @Input() placeholder = 'Buscar club...';
  @Output() readonly clubSelected = new EventEmitter<{ clubId: string | null; clubName: string }>();

  value = '';
  disabled = false;
  readonly suggestions = signal<ClubResponse[]>([]);
  readonly showDropdown = signal(false);

  private readonly query$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();
  private requestId = 0;

  onChange: any = () => {};
  onTouched: any = () => {};

  ngOnInit(): void {
    this.query$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        const trimmed = query.trim();
        if (trimmed.length < 2) {
          return of([]);
        }
        return this.clubService.searchClubs(trimmed);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (results) => {
        this.suggestions.set(results);
      },
      error: () => {
        this.suggestions.set([]);
      }
    });
  }

  hasExactMatch(): boolean {
    const trimmed = this.value.trim().toLowerCase();
    return this.suggestions().some(c => c.name.toLowerCase() === trimmed);
  }

  onInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.value = val;
    this.onChange(val);
    this.query$.next(val);
  }

  onFocus(): void {
    this.showDropdown.set(true);
    if (this.value.trim().length >= 2) {
      this.query$.next(this.value);
    }
  }

  onBlur(): void {
    setTimeout(() => {
      this.showDropdown.set(false);
      this.onTouched();
    }, 200);
  }

  selectClub(club: ClubResponse): void {
    this.value = club.name;
    this.onChange(club.name);
    this.suggestions.set([]);
    this.showDropdown.set(false);
    this.clubSelected.emit({ clubId: club.id, clubName: club.name });
  }

  createClub(): void {
    const name = this.value.trim();
    this.suggestions.set([]);
    this.showDropdown.set(false);
    this.clubSelected.emit({ clubId: null, clubName: name });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(value: any): void {
    this.value = value || '';
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
