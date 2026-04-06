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
  providerOrganisationId?: string | null;
}