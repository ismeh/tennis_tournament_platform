import { Routes } from '@angular/router';
import { HomeComponent } from './features/home';
import { PlaceholderPage } from './features/placeholder';
import { LoginComponent } from './features/auth/login';
import { RegisterComponent } from './features/auth/register';
import { CreateTournamentComponent } from './features/tournaments/create';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
  },
  {
    path: 'torneos',
    component: CreateTournamentComponent,
    data: { title: 'Crear Torneo' }
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
    path: '**',
    redirectTo: ''
  }
];
