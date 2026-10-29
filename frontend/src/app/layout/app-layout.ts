import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../components/header';
import { FooterComponent } from '../components/footer';
import { ToastContainerComponent } from '../components/toast-container';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, ToastContainerComponent],
  template: `
    <div class="flex flex-col min-h-screen">
      <app-header></app-header>
      <main class="flex-grow pt-16">
        <router-outlet></router-outlet>
      </main>
      <app-footer></app-footer>
      <app-toast-container></app-toast-container>
    </div>
  `,
  styles: []
})
export class AppLayoutComponent { }
