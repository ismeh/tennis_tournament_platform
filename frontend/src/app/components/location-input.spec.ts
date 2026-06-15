import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LocationInputComponent } from './location-input';
import { PlaceService } from '../data/services/place.service';
import { of } from 'rxjs';

describe('LocationInputComponent', () => {
  let fixture: ComponentFixture<LocationInputComponent>;
  let component: LocationInputComponent;
  let placeServiceSpy: jasmine.SpyObj<PlaceService>;

  beforeEach(async () => {
    placeServiceSpy = jasmine.createSpyObj('PlaceService', ['search']);
    placeServiceSpy.search.and.returnValue(of([
      {
        placeId: 'id1',
        name: 'Club de Tenis',
        formattedAddress: 'Calle de Tenis, 1',
        latitude: 40.0,
        longitude: -3.0,
        mapsUrl: 'http://maps'
      }
    ]));

    await TestBed.configureTestingModule({
      imports: [LocationInputComponent],
      providers: [
        { provide: PlaceService, useValue: placeServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LocationInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should search place suggestions with debounce on input', fakeAsync(() => {
    const inputElement = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    inputElement.value = 'Club';
    inputElement.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // Before tick, no search should have been called due to 300ms debounce
    expect(placeServiceSpy.search).not.toHaveBeenCalled();

    tick(300);

    expect(placeServiceSpy.search).toHaveBeenCalledWith('Club', undefined, undefined);
    expect(component.suggestions().length).toBe(1);
    expect(component.suggestions()[0].name).toBe('Club de Tenis');
  }));

  it('should select suggestion and emit event', () => {
    const suggestion = {
      placeId: 'id1',
      name: 'Club de Tenis',
      formattedAddress: 'Calle de Tenis, 1',
      latitude: 40.0,
      longitude: -3.0,
      mapsUrl: 'http://maps'
    };

    spyOn(component.locationSelected, 'emit');

    component.selectSuggestion(suggestion);

    expect(component.value).toBe('Club de Tenis');
    expect(component.locationSelected.emit).toHaveBeenCalledWith(suggestion);
  });
});
