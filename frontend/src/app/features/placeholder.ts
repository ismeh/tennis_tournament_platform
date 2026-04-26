import { Component, OnInit } from '@angular/core';
import { PlaceholderComponent } from "../components/placeholder-component";

@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [PlaceholderComponent],
  template: `
    <app-placeholder-component></app-placeholder-component>
  `,
  styles: []
})

export class PlaceholderPage {
}
