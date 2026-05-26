import { Component } from '@angular/core';
import { AppSettings } from '../shared/constants';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `

  `,
  styles: []
})
export class LoginComponent {
  AppSettings: typeof AppSettings = AppSettings;
}
