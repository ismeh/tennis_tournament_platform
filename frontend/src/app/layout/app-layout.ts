import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../components/header';
import { FooterComponent } from '../components/footer';
import { ToastContainerComponent } from '../components/toast-container';
import { ConnectionStatusBannerComponent } from '../components/connection-status-banner';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, ToastContainerComponent, ConnectionStatusBannerComponent],
  template: `
    <div class="flex flex-col min-h-screen">
      <app-header></app-header>
      <main class="flex-grow pt-16">
        <router-outlet></router-outlet>
      </main>
      <app-footer></app-footer>
      <app-toast-container></app-toast-container>
      <app-connection-status-banner></app-connection-status-banner>
    </div>
  `,
  styles: []
})
export class AppLayoutComponent { }
