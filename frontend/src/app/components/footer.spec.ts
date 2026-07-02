import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FooterComponent } from './footer';
import { AppSettings } from '../shared/constants';

describe('FooterComponent', () => {
  let component: FooterComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [
        provideRouter([{ path: '**', component: FooterComponent }]),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose AppSettings', () => {
    expect(component.AppSettings).toBe(AppSettings);
  });

  it('should have PROJECT_NAME in AppSettings', () => {
    expect(component.AppSettings.PROJECT_NAME).toBeDefined();
    expect(typeof component.AppSettings.PROJECT_NAME).toBe('string');
  });
});
