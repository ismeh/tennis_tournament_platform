import { Routes } from '@angular/router';
import { HomeComponent } from './features/home';
import { PlaceholderPage } from './features/placeholder';
import { LoginComponent } from './features/auth/login';
import { RegisterComponent } from './features/auth/register';
import { ConfirmEmailComponent } from './features/auth/confirm-email';
import { CalendarComponent } from './features/calendar/calendar';
import { RankingComponent } from './features/ranking/ranking';
import { ProfileComponent } from './features/profile/profile';
import { CreateTournamentComponent } from './features/tournaments/create';
import { TournamentDetailComponent } from './features/tournaments/detail';
import { HowItWorksComponent } from './features/how-it-works';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
  },
  {
    path: 'torneos',
    component: CalendarComponent,
    data: { title: 'Torneos' }
  },
  {
    path: 'calendario',
    redirectTo: 'torneos',
    pathMatch: 'full'
  },
  {
    path: 'ranking',
    component: RankingComponent,
    data: { title: 'Ranking' }
  },
  {
    path: 'torneos/crear',
    component: CreateTournamentComponent,
    data: { title: 'Crear Torneo' }
  },
  {
    path: 'torneos/:id',
    component: TournamentDetailComponent,
    data: { title: 'Detalle Torneo' }
  },
  {
    path: 'como-funciona',
    component: HowItWorksComponent,
    data: { title: 'Cómo Funciona' }
  },
  {
    path: 'contacto',
    component: PlaceholderPage,
    data: { title: 'Contáctenos' }
  },
  {
    path: 'login',
    component: LoginComponent,
    data: { title: 'Iniciar Sesión' }
  },
  {
    path: 'register',
    component: RegisterComponent,
    data: { title: 'Registrarse' }
  },
  {
    path: 'confirmar-email',
    component: ConfirmEmailComponent,
    data: { title: 'Confirmar email' }
  },
  {
    path: 'perfil',
    component: ProfileComponent,
    data: { title: 'Completar perfil' }
  },
  {
    path: '**',
    redirectTo: ''
  }
];
