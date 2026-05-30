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
  tournamentStartTime: string;
  inscriptionStartDate: string;
  inscriptionEndDate: string;
  surfaceCategory: TournamentSurfaceCategory;
  maxPlayers: number;
  location: string;
  courtCount: number;
}

export interface TournamentProviderSummary {
  id: string;
}

export interface TournamentResponse {
  id: string;
  formalName: string;
  playStartDate: string;
  playEndDate: string;
  tournamentStartTime?: string | null;
  inscriptionStartDate: string;
  inscriptionEndDate: string;
  surfaceCategory: TournamentSurfaceCategory;
  maxPlayers: number;
  location: string;
  status: TournamentStatus;
  providerOrganisationId?: string | TournamentProviderSummary | null;
  events?: TournamentEventResponse[];
}

export interface CourtResponse {
  id: string;
  tournamentId: string;
  name: string;
  active: boolean;
}

export interface CourtCreateRequest {
  name: string;
}

export interface CourtUpdateRequest {
  name: string;
}

export interface TournamentEventResponse {
  eventId: string;
  categoryId: number;
  gender: string;
  stages?: StageResponse[];
}

export interface StageResponse {
  id: string;
  eventId: string;
  stageType: string;
  order: number;
  description: string;
  draws?: DrawResponse[];
}

export interface DrawResponse {
  id: string;
  stageId: string;
  drawType: string;
  label: string;
  matches?: MatchResponse[];
}

export interface MatchResponse {
  id: string;
  firstInscriptionId: string;
  secondInscriptionId: string;
  winnerId?: string | null;
  roundNumber: number;
  scheduledAt?: string | null;
  scheduleTimeType?: MatchScheduleTimeType | null;
  courtId?: string | null;
  court?: string | null;
  result?: string | null;
}

export type MatchScheduleTimeType = 'EXACT' | 'NOT_BEFORE';

export interface MatchScheduleRequest {
  courtId: string;
  scheduledAt: string;
  scheduleTimeType: MatchScheduleTimeType;
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

export type TournamentStageType = 'SINGLE_ELIMINATION' | 'ROUND_ROBIN' | 'DOUBLE_ELIMINATION' | 'CONSOLATION';

export const TOURNAMENT_STAGE_TYPE_LABELS: Record<TournamentStageType, string> = {
  SINGLE_ELIMINATION: 'Eliminatoria simple',
  ROUND_ROBIN: 'Round Robin',
  DOUBLE_ELIMINATION: 'Doble eliminación',
  CONSOLATION: 'Consolación'
};

export function getTournamentStageTypeLabel(stageType: TournamentStageType): string {
  return TOURNAMENT_STAGE_TYPE_LABELS[stageType] ?? stageType;
}

export interface TournamentEventCatalogItem {
  id: number;
  category: string;
  description: string;
}

export interface TournamentEventGenderEventId {
  gender: TournamentEventGender;
  eventId: string | null;
}

export interface TournamentEventSelection {
  categoryId: number;
  eventCategory: string;
  eventsByGender: TournamentEventGenderEventId[];
  genders: TournamentEventGender[];
  stages: TournamentEventStageSelection[];
}

export interface TournamentEventStageSelection {
  stageType: TournamentStageType;
}

export interface TournamentEventCategoryGender {
  id?: string | null;
  eventId?: string;
  categoryId: number;
  gender: string;
  stages: TournamentStageType[];
}

export interface TournamentEventsConfigRequest {
  events: TournamentEventCategoryGender[];
}

export interface EventInscriptionRequest {
  categoryId: number;
  partnerId?: string | null;
}

export type ManualParticipantSource = 'EXISTING_PERSON' | 'MANUAL' | 'PROFESSIONAL';

export interface ManualEventInscriptionRequest {
  playerSource: ManualParticipantSource;
  personId?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  gender?: string | null;
  birthDate?: string | null;
  nationality?: string | null;
  tennisId?: string | null;
}

export interface EventInscriptionResponse {
  id: string;
  tournamentId: string;
  eventId: string;
  categoryId: number;
  memberId: string;
  partnerId?: string | null;
  registeredAt: string;
}

export interface TournamentInscriptionsResponse {
  tournamentId: string;
  selectedEventId?: string | null;
  events: TournamentInscriptionEvent[];
  categoryCounts: TournamentInscriptionCategoryCount[];
  inscriptions: TournamentInscriptionPlayer[];
}

export interface TournamentInscriptionEvent {
  eventId: string;
  categoryId: number;
  category: string;
  eventName: string;
  eventGender: string;
}

export interface TournamentInscriptionCategoryCount {
  categoryId: number;
  category: string;
  totalPlayers: number;
  genders: TournamentInscriptionGenderCount[];
}

export interface TournamentInscriptionGenderCount {
  gender: string;
  totalPlayers: number;
}

export interface TournamentInscriptionPlayer {
  inscriptionId: string;
  eventId: string;
  categoryId: number;
  category: string;
  eventName: string;
  eventGender: string;
  personId?: string | null;
  playerSource?: string | null;
  tennisId?: string | null;
  firstName: string;
  lastName: string;
  gender: string;
}

export interface TournamentStatusUpdateRequest {
  status: TournamentStatus;
}
