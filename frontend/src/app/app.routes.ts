import { Routes } from '@angular/router';
import { HomeComponent } from './features/home';
import { PlaceholderPage } from './features/placeholder';
import { LoginComponent } from './features/auth/login';
import { RegisterComponent } from './features/auth/register';
import { ProfileComponent } from './features/profile/profile';
import { CreateTournamentComponent } from './features/tournaments/create';
import { TournamentDetailComponent } from './features/tournaments/detail';
import { TournamentsListComponent } from './features/tournaments/list';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
  },
  {
    path: 'torneos',
    component: TournamentsListComponent,
    data: { title: 'Torneos' }
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
    component: PlaceholderPage,
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
    path: 'perfil',
    component: ProfileComponent,
    data: { title: 'Completar perfil' }
  },
  {
    path: '**',
    redirectTo: ''
  }
];
