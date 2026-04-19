export type TournamentSurfaceCategory = 'CLAY' | 'HARD' | 'GRASS' | 'CARPET';

export const TOURNAMENT_SURFACE_CATEGORY_LABELS: Record<TournamentSurfaceCategory, string> = {
  CLAY: 'Tierra batida',
  HARD: 'Pista dura',
  GRASS: 'Cesped',
  CARPET: 'Moqueta'
};

export function getTournamentSurfaceCategoryLabel(surface: TournamentSurfaceCategory): string {
  return TOURNAMENT_SURFACE_CATEGORY_LABELS[surface] ?? surface;
}

export type TournamentStatus = 'DRAFT' | 'OPEN' | 'ACTIVE' | 'CLOSED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface TournamentCreateRequest {
  formalName: string;
  playStartDate: string;
  playEndDate: string;
  inscriptionStartDate: string;
  inscriptionEndDate: string;
  surfaceCategory: TournamentSurfaceCategory;
  maxPlayers: number;
  location: string;
}

export interface TournamentProviderSummary {
  id: string;
}

export interface TournamentResponse {
  id: string;
  formalName: string;
  playStartDate: string;
  playEndDate: string;
  inscriptionStartDate: string;
  inscriptionEndDate: string;
  surfaceCategory: TournamentSurfaceCategory;
  maxPlayers: number;
  location: string;
  status: TournamentStatus;
  providerOrganisationId?: string | TournamentProviderSummary | null;
  events?: TournamentEventCategoryGender[];
}

export type TournamentEventGender = 'MALE' | 'FEMALE' | 'MIXED';

export const TOURNAMENT_EVENT_GENDER_LABELS: Record<TournamentEventGender, string> = {
  MALE: 'Masculino',
  FEMALE: 'Femenino',
  MIXED: 'Mixto'
};

export function getTournamentEventGenderLabel(gender: TournamentEventGender): string {
  return TOURNAMENT_EVENT_GENDER_LABELS[gender] ?? gender;
}

export interface TournamentEventCatalogItem {
  id: number;
  category: string;
  description: string;
}

export interface TournamentEventSelection {
  categoryId: number;
  eventCategory: string;
  genders: TournamentEventGender[];
}

export interface TournamentEventCategoryGender {
  categoryId: number;
  gender: string;
}

export interface TournamentEventsConfigRequest {
  events: TournamentEventCategoryGender[];
}

export interface TournamentStatusUpdateRequest {
  status: TournamentStatus;
}