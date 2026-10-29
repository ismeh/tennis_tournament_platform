import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ClubAutocompleteComponent } from './club-autocomplete';
import { ClubService } from '../data/services/club.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

describe('ClubAutocompleteComponent', () => {
  let component: ClubAutocompleteComponent;
  let fixture: ComponentFixture<ClubAutocompleteComponent>;
  let clubService: jasmine.SpyObj<ClubService>;

  const mockClubs = [
    { id: 'c1', name: 'Club de Tenis Madrid', active: true, tennisCenterId: 'tc1', address: 'Calle Mayor 1', location: null },
    { id: 'c2', name: 'Club Tenis Barcelona', active: true, tennisCenterId: 'tc2', address: 'Carrer Valencia 2', location: null },
  ];

  beforeEach(async () => {
    clubService = jasmine.createSpyObj('ClubService', ['searchClubs']);
    clubService.searchClubs.and.returnValue(of(mockClubs));

    await TestBed.configureTestingModule({
      imports: [ClubAutocompleteComponent, FormsModule],
      providers: [{ provide: ClubService, useValue: clubService }],
    }).compileComponents();

    fixture = TestBed.createComponent(ClubAutocompleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ControlValueAccessor', () => {
    it('should write value to component', () => {
      component.writeValue('Test Club');
      expect(component.value).toBe('Test Club');
    });

    it('should write empty string for null value', () => {
      component.writeValue(null as any);
      expect(component.value).toBe('');
    });

    it('should call onChange when value changes via input', fakeAsync(() => {
      const onChange = jasmine.createSpy('onChange');
      component.registerOnChange(onChange);

      const input = fixture.debugElement.query(By.css('input')).nativeElement;
      input.value = 'Club';
      input.dispatchEvent(new Event('input'));
      tick(300);

      expect(onChange).toHaveBeenCalledWith('Club');
    }));

    it('should call onTouched on blur', fakeAsync(() => {
      const onTouched = jasmine.createSpy('onTouched');
      component.registerOnTouched(onTouched);

      const input = fixture.debugElement.query(By.css('input')).nativeElement;
      input.dispatchEvent(new Event('blur'));
      tick(300);

      expect(onTouched).toHaveBeenCalled();
    }));

    it('should set disabled state', () => {
      component.setDisabledState(true);
      expect(component.disabled).toBeTrue();

      component.setDisabledState(false);
      expect(component.disabled).toBeFalse();
    });
  });

  describe('clubSelected output', () => {
    it('should emit clubSelected when a club is selected', fakeAsync(() => {
      spyOn(component.clubSelected, 'emit');

      component.value = 'Club d';
      component.onInput({ target: { value: 'Club d' } } as any);
      tick(300);
      fixture.detectChanges();

      component.selectClub(mockClubs[0]);

      expect(component.clubSelected.emit).toHaveBeenCalledWith({
        clubId: 'c1',
        clubName: 'Club de Tenis Madrid',
      });
    }));

    it('should emit clubSelected with null clubId when creating a new club', fakeAsync(() => {
      spyOn(component.clubSelected, 'emit');

      component.value = 'Nuevo Club';
      component.onInput({ target: { value: 'Nuevo Club' } } as any);
      tick(300);
      fixture.detectChanges();

      component.createClub();

      expect(component.clubSelected.emit).toHaveBeenCalledWith({
        clubId: null,
        clubName: 'Nuevo Club',
      });
    }));
  });

  describe('hasExactMatch', () => {
    it('should return true when an exact case-insensitive match exists', () => {
      component.suggestions.set(mockClubs);
      component.value = 'club de tenis madrid';
      expect(component.hasExactMatch()).toBeTrue();
    });

    it('should return false when no exact match exists', () => {
      component.suggestions.set(mockClubs);
      component.value = 'Unknown Club';
      expect(component.hasExactMatch()).toBeFalse();
    });

    it('should return false when suggestions are empty', () => {
      component.suggestions.set([]);
      component.value = 'Any Club';
      expect(component.hasExactMatch()).toBeFalse();
    });
  });

  describe('onFocus', () => {
    it('should show dropdown on focus when value has 2+ chars', fakeAsync(() => {
      component.value = 'Club';
      component.onFocus();
      tick(300);

      expect(component.showDropdown()).toBeTrue();
      expect(clubService.searchClubs).toHaveBeenCalledWith('Club');
    }));

    it('should not search when value is empty on focus', () => {
      component.value = '';
      component.onFocus();

      expect(clubService.searchClubs).not.toHaveBeenCalled();
    });
  });

  describe('onBlur', () => {
    it('should hide dropdown after blur with delay', fakeAsync(() => {
      component.showDropdown.set(true);
      component.onBlur();

      expect(component.showDropdown()).toBeTrue();
      tick(300);

      expect(component.showDropdown()).toBeFalse();
    }));
  });

  describe('dropdown visibility', () => {
    it('should show dropdown when suggestions exist and showDropdown is true', fakeAsync(() => {
      component.value = 'Club';
      component.showDropdown.set(true);
      component.onInput({ target: { value: 'Club' } } as any);
      tick(300);
      fixture.detectChanges();

      expect(component.showDropdown()).toBeTrue();
      expect(component.suggestions().length).toBeGreaterThan(0);
    }));

    it('should show create option when no exact match and 2+ chars', fakeAsync(() => {
      component.suggestions.set([]);
      component.value = 'New Club';
      component.showDropdown.set(true);
      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));
      const lastBtn = buttons[buttons.length - 1];
      expect(lastBtn.nativeElement.textContent).toContain('Crear "New Club"');
    }));
  });

  describe('error handling in search', () => {
    it('should clear suggestions on error', fakeAsync(() => {
      clubService.searchClubs.and.returnValue(throwError(() => new Error('Network error')));

      component.value = 'Club';
      component.onInput({ target: { value: 'Club' } } as any);
      tick(300);

      expect(component.suggestions()).toEqual([]);
    }));
  });
});
