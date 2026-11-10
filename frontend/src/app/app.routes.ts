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
import { ManageCategoriesComponent } from './features/categories/manage-categories';
import { AccountSettingsComponent } from './features/account/account-settings';
import { PrivacyPolicyComponent } from './features/privacy/privacy-policy';
import { TermsConditionsComponent } from './features/terms/terms-conditions';
import { ContactComponent } from './features/contact/contact';
import { ClubListComponent } from './features/clubs/club-list';
import { ClubDetailComponent } from './features/clubs/club-detail';
import { ClubCreateComponent } from './features/clubs/club-create';
import { MyClubsComponent } from './features/clubs/my-clubs';
import { ClubManageComponent } from './features/clubs/club-manage';
import { authGuard } from './core/guards/auth.guard';
import { organizerGuard } from './core/guards/organizer.guard';

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
    canActivate: [organizerGuard],
    data: { title: 'Crear Torneo' }
  },
  {
    path: 'torneos/:id',
    component: TournamentDetailComponent,
    data: { title: 'Detalle Torneo' }
  },
  {
    path: 'mis-categorias',
    component: ManageCategoriesComponent,
    canActivate: [organizerGuard],
    data: { title: 'Mis Categorías' }
  },
  {
    path: 'clubes',
    component: ClubListComponent,
    data: { title: 'Clubes' }
  },
  {
    path: 'clubes/crear',
    component: ClubCreateComponent,
    canActivate: [authGuard],
    data: { title: 'Crear club' }
  },
  {
    path: 'clubes/:clubId/gestion',
    component: ClubManageComponent,
    canActivate: [authGuard],
    data: { title: 'Gestión del club' }
  },
  {
    path: 'clubes/:slug',
    component: ClubDetailComponent,
    data: { title: 'Club' }
  },
  {
    path: 'mis-clubes',
    component: MyClubsComponent,
    canActivate: [authGuard],
    data: { title: 'Mis clubes' }
  },
  {
    path: 'como-funciona',
    component: HowItWorksComponent,
    data: { title: 'Cómo Funciona' }
  },
  {
    path: 'politica-privacidad',
    component: PrivacyPolicyComponent,
    data: { title: 'Política de Privacidad' }
  },
  {
    path: 'terminos-condiciones',
    component: TermsConditionsComponent,
    data: { title: 'Términos y Condiciones' }
  },
  {
    path: 'contacto',
    component: ContactComponent,
    data: { title: 'Contacto' }
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
    canActivate: [authGuard],
    data: { title: 'Completar perfil' }
  },
  {
    path: 'ajustes-cuenta',
    component: AccountSettingsComponent,
    canActivate: [authGuard],
    data: { title: 'Ajustes de cuenta' }
  },
  {
    path: '**',
    redirectTo: ''
  }
];
