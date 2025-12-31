import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home';
import { PlaceholderComponent } from './pages/placeholder';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
  },
  {
    path: 'torneos',
    component: PlaceholderComponent,
    data: { title: 'Torneos' }
  },
  {
    path: 'como-funciona',
    component: PlaceholderComponent,
    data: { title: 'Cómo Funciona' }
  },
  {
    path: 'contacto',
    component: PlaceholderComponent,
    data: { title: 'Contáctenos' }
  },
  {
    path: 'iniciar-sesion',
    component: PlaceholderComponent,
    data: { title: 'Iniciar Sesión' }
  },
  {
    path: 'registrarse',
    component: PlaceholderComponent,
    data: { title: 'Registrarse' }
  },
  {
    path: '**',
    redirectTo: ''
  }
];
