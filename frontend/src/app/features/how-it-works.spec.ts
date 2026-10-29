import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HowItWorksComponent } from './how-it-works';
import { AppSettings } from '../shared/constants';

describe('HowItWorksComponent', () => {
  let component: HowItWorksComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HowItWorksComponent],
      providers: [
        provideRouter([{ path: '**', component: HowItWorksComponent }]),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(HowItWorksComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose AppSettings', () => {
    expect(component.AppSettings).toBe(AppSettings);
  });

  it('should render heading text', () => {
    const fixture = TestBed.createComponent(HowItWorksComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Cómo funciona');
  });

  it('should render project name', () => {
    const fixture = TestBed.createComponent(HowItWorksComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain(AppSettings.PROJECT_NAME);
  });

  it('should render step numbers', () => {
    const fixture = TestBed.createComponent(HowItWorksComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('1');
    expect(compiled.textContent).toContain('2');
    expect(compiled.textContent).toContain('3');
    expect(compiled.textContent).toContain('4');
  });

  it('should render CTA links', () => {
    const fixture = TestBed.createComponent(HowItWorksComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const links = compiled.querySelectorAll('a[routerLink]');
    expect(links.length).toBeGreaterThan(0);
  });
});
