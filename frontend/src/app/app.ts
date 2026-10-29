import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppLayoutComponent } from './layout/app-layout';

@Component({
  selector: 'app-root',
  imports: [AppLayoutComponent],
  template: '<app-layout></app-layout>',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('tfm_front');
}
