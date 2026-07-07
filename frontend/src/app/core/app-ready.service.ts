import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AppReadyService {
  private ready = false;

  markReady(): void {
    this.ready = true;
  }

  isReady(): boolean {
    return this.ready;
  }
}
