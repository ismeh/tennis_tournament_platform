import { Component, EventEmitter, forwardRef, inject, Input, OnDestroy, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subject, of, takeUntil } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { PlaceService, PlaceSuggestion } from '../data/services/place.service';

@Component({
  selector: 'app-location-input',
  standalone: true,
  imports: [CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LocationInputComponent),
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
        class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
        [placeholder]="placeholder"
        [disabled]="disabled"
      />

      @if (suggestions().length > 0 && showDropdown()) {
        <div class="absolute z-50 w-full mt-2 overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-xl max-h-60 overflow-y-auto">
          @for (suggestion of suggestions(); track suggestion.placeId) {
            <button
              type="button"
              (mousedown)="selectSuggestion(suggestion)"
              class="w-full px-4 py-3 text-left hover:bg-primary-50 hover:text-primary-900 transition-colors flex flex-col border-b border-neutral-100 last:border-0"
            >
              <span class="font-semibold text-neutral-800 text-sm">{{ suggestion.name }}</span>
              <span class="text-neutral-500 text-xs mt-0.5">{{ suggestion.formattedAddress }}</span>
            </button>
          }
          <div class="bg-neutral-50 px-4 py-2 text-right text-[11px] font-medium text-neutral-500">
            Google Maps
          </div>
        </div>
      }
    </div>
  `
})
export class LocationInputComponent implements OnInit, OnDestroy, ControlValueAccessor {
  private readonly placeService = inject(PlaceService);

  @Input() placeholder = 'Buscar ubicación...';
  @Output() readonly locationSelected = new EventEmitter<{
    name: string;
    placeId?: string | null;
    formattedAddress?: string | null;
    latitude?: number | null;
    longitude?: number | null;
    mapsUrl?: string | null;
  }>();

  value = '';
  disabled = false;
  readonly suggestions = signal<PlaceSuggestion[]>([]);
  readonly showDropdown = signal(false);

  private readonly query$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();
  private userCoords: { latitude: number; longitude: number } | null = null;

  onChange: any = () => {};
  onTouched: any = () => {};

  ngOnInit(): void {
    if (typeof window !== 'undefined' && window.navigator && window.navigator.geolocation) {
      window.navigator.geolocation.getCurrentPosition(
        (position) => {
          this.userCoords = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          };
        },
        (error) => {
          console.debug('Geolocation request rejected or unavailable:', error.message);
        }
      );
    }

    this.query$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        const trimmed = query.trim();
        if (trimmed.length < 2) {
          return of([]);
        }
        return this.placeService.search(
          trimmed,
          this.userCoords?.latitude,
          this.userCoords?.longitude
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (results) => {
        this.suggestions.set(results);
      },
      error: (err) => {
        console.error('Error fetching place suggestions:', err);
        this.suggestions.set([]);
      }
    });
  }

  onInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.value = val;
    this.onChange(val);
    this.query$.next(val);

    this.locationSelected.emit({
      name: val,
      placeId: null,
      formattedAddress: null,
      latitude: null,
      longitude: null,
      mapsUrl: null
    });
  }

  onFocus(): void {
    this.showDropdown.set(true);
  }

  onBlur(): void {
    setTimeout(() => {
      this.showDropdown.set(false);
      this.onTouched();
    }, 200);
  }

  selectSuggestion(suggestion: PlaceSuggestion): void {
    this.value = suggestion.name;
    this.onChange(suggestion.name);
    this.suggestions.set([]);
    this.showDropdown.set(false);
    this.locationSelected.emit(suggestion);
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
