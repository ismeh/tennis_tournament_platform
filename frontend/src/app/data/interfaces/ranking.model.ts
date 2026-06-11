export type RankingMode = 'professionals' | 'tournament';

export type RankingGender = 'MALE' | 'FEMALE' | 'MIXED';

export type RankingSortDirection = 'asc' | 'desc';

export interface ProfessionalRankingResponse {
  position: number | null;
  playerId: number;
  license: string | null;
  fullName: string;
  firstName: string;
  lastName: string | null;
  gender: string | null;
  category: string | null;
  clubName: string | null;
  birthDate: string | null;
  points: number | null;
}

export interface TournamentRankingResponse {
  position: number;
  participantId: string;
  license: string | null;
  firstName: string;
  lastName: string | null;
  gender: string | null;
  victories: number;
}

export interface RankingTournamentResponse {
  id: string;
  formalName: string;
  playStartDate: string;
  playEndDate: string;
  tournamentStartTime?: string | null;
  inscriptionStartDate: string;
  inscriptionEndDate: string;
  surfaceCategory: string;
  maxPlayers: number;
  location: string;
  status: string;
  professionalTournament?: boolean | null;
}

export interface RankingFilters {
  gender?: RankingGender | null;
  category?: string | null;
  categoryId?: number | null;
  page?: number | null;
  size?: number | null;
  sortBy?: string | null;
  sortDirection?: RankingSortDirection | null;
}

export interface RankingPageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  sortBy: string;
  sortDirection: RankingSortDirection;
}
