import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { InvitationPreview, InvitationService } from '../../core/services/invitation.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';

@Component({
  selector: 'app-accept-invitation',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 px-4 py-12">
      <div class="w-full max-w-md">

        @if (loading()) {
          <div class="flex flex-col items-center gap-4 text-white">
            <div class="h-12 w-12 animate-spin rounded-full border-4 border-white/20 border-t-white"></div>
            <p class="text-slate-300 text-sm">Cargando invitación...</p>
          </div>
        }

        @if (!loading() && errorMessage()) {
          <div class="rounded-2xl bg-red-500/10 border border-red-500/30 p-8 text-center backdrop-blur-sm">
            <div class="text-5xl mb-4">❌</div>
            <h1 class="text-xl font-bold text-white mb-2">Enlace no válido</h1>
            <p class="text-red-300 text-sm mb-6">{{ errorMessage() }}</p>
            <a routerLink="/" class="inline-block rounded-xl bg-white/10 hover:bg-white/20 text-white px-6 py-2 text-sm font-medium transition-colors">
              Ir al inicio
            </a>
          </div>
        }

        @if (!loading() && !errorMessage() && preview()) {
          @if (preview()!.claimed) {
            <div class="rounded-2xl bg-amber-500/10 border border-amber-500/30 p-8 text-center backdrop-blur-sm">
              <div class="text-5xl mb-4">🔒</div>
              <h1 class="text-xl font-bold text-white mb-2">Invitación ya utilizada</h1>
              <p class="text-amber-300 text-sm mb-6">Este enlace ya fue usado para vincular una cuenta.</p>
              <a routerLink="/" class="inline-block rounded-xl bg-white/10 hover:bg-white/20 text-white px-6 py-2 text-sm font-medium transition-colors">
                Ir al inicio
              </a>
            </div>
          } @else if (preview()!.expired) {
            <div class="rounded-2xl bg-orange-500/10 border border-orange-500/30 p-8 text-center backdrop-blur-sm">
              <div class="text-5xl mb-4">⏰</div>
              <h1 class="text-xl font-bold text-white mb-2">Invitación caducada</h1>
              <p class="text-orange-300 text-sm mb-6">Este enlace ha expirado. Pide al organizador que genere uno nuevo.</p>
              <a routerLink="/" class="inline-block rounded-xl bg-white/10 hover:bg-white/20 text-white px-6 py-2 text-sm font-medium transition-colors">
                Ir al inicio
              </a>
            </div>
          } @else {
            <div class="rounded-2xl bg-white/5 border border-white/10 p-8 backdrop-blur-sm shadow-2xl">
              <div class="text-center mb-8">
                <div class="text-5xl mb-3">🎾</div>
                <p class="text-slate-400 text-sm uppercase tracking-widest font-medium mb-1">Invitación al torneo</p>
                <h1 class="text-2xl font-bold text-white leading-tight">{{ preview()!.tournamentName }}</h1>
              </div>

              <div class="rounded-xl bg-white/5 border border-white/10 p-4 mb-8 text-center">
                <p class="text-xs text-slate-400 uppercase tracking-wide mb-1">Participas como</p>
                <p class="text-lg font-semibold text-white">{{ preview()!.playerDisplayName }}</p>
              </div>

              <p class="text-slate-400 text-sm text-center mb-6">
                Crea tu cuenta para aparecer oficialmente en el torneo con tu perfil.
              </p>

              <a
                [routerLink]="['/register']"
                (click)="onRegisterClick()"
                class="block w-full rounded-xl bg-emerald-500 hover:bg-emerald-400 text-white text-center px-6 py-3 font-semibold text-sm transition-colors shadow-lg shadow-emerald-500/25 mb-3">
                Crear mi cuenta
              </a>

              <a
                routerLink="/"
                class="block w-full rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 text-center px-6 py-3 text-sm transition-colors">
                Cancelar
              </a>
            </div>
          }
        }

      </div>
    </section>
  `
})
export class AcceptInvitationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly invitationService = inject(InvitationService);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly preview = signal<InvitationPreview | null>(null);

  private token: string | null = null;

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');

    if (!this.token) {
      this.loading.set(false);
      this.errorMessage.set('El enlace de invitación no es válido o está incompleto.');
      return;
    }

    this.invitationService.previewInvitation(this.token).subscribe({
      next: (data) => {
        this.preview.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(getApiErrorMessage(err, 'El enlace de invitación no es válido.'));
      }
    });
  }

  onRegisterClick(): void {
    if (this.token) {
      this.invitationService.storePendingToken(this.token);
    }
  }
}
