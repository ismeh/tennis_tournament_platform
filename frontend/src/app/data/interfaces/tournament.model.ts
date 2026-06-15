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
  locationLatitude?: number | null;
  locationLongitude?: number | null;
  locationPlaceId?: string | null;
  locationFormattedAddress?: string | null;
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
  locationLatitude?: number | null;
  locationLongitude?: number | null;
  locationPlaceId?: string | null;
  locationFormattedAddress?: string | null;
  status: TournamentStatus;
  providerOrganisationId?: string | TournamentProviderSummary | null;
  events?: TournamentEventResponse[];
  professionalTournament?: boolean | null;
}

export interface TournamentCalendarFilters {
  from?: string | null;
  to?: string | null;
  surface?: TournamentSurfaceCategory | null;
  location?: string | null;
  name?: string | null;
  professionalTournament?: boolean | null;
  status?: TournamentStatus | null;
}

export interface TournamentCalendarResponse {
  id: string;
  formalName: string;
  playStartDate: string;
  playEndDate: string;
  tournamentStartTime?: string | null;
  location: string;
  surfaceCategory: TournamentSurfaceCategory;
  maxPlayers: number;
  status: TournamentStatus;
  professionalTournament?: boolean | null;
}

export interface PlayerMatchCalendarResponse {
  tournamentId: string;
  tournamentName: string;
  eventId: string;
  eventName: string;
  matchId: string;
  roundNumber: number;
  scheduledAt: string;
  scheduleTimeType?: MatchScheduleTimeType | null;
  courtId?: string | null;
  court?: string | null;
  firstInscriptionId?: string | null;
  firstParticipantName: string;
  secondInscriptionId?: string | null;
  secondParticipantName: string;
  result?: string | null;
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
  strategyName: string | null;
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
  firstInscriptionId: string | null;
  secondInscriptionId: string | null;
  winnerId?: string | null;
  roundNumber: number;
  bracketPosition?: number | null;
  scheduledAt?: string | null;
  scheduleTimeType?: MatchScheduleTimeType | null;
  courtId?: string | null;
  court?: string | null;
  result?: string | null;
  professionalMatch?: boolean | null;
  firstWinPoints?: number | null;
  secondWinPoints?: number | null;
}

export type MatchScheduleTimeType = 'EXACT' | 'NOT_BEFORE';

export interface MatchScheduleRequest {
  courtId: string;
  scheduledAt: string;
  scheduleTimeType: MatchScheduleTimeType;
  cascade?: boolean;
}

export type TournamentUpdateType = 'MATCH_RESULT_UPDATED' | 'MATCH_SCHEDULE_UPDATED';

export interface TournamentUpdateEvent {
  type: TournamentUpdateType;
  tournamentId: string;
  matchId: string;
  occurredAt: string;
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

const VALID_STAGE_TYPES: TournamentStageType[] = ['SINGLE_ELIMINATION', 'ROUND_ROBIN', 'DOUBLE_ELIMINATION', 'CONSOLATION'];

export function isValidStageType(value: string): value is TournamentStageType {
  return (VALID_STAGE_TYPES as string[]).includes(value);
}

export const TOURNAMENT_STAGE_TYPE_LABELS: Record<TournamentStageType, string> = {
  SINGLE_ELIMINATION: 'Eliminatoria simple',
  ROUND_ROBIN: 'Round Robin',
  DOUBLE_ELIMINATION: 'Doble eliminación',
  CONSOLATION: 'Consolación'
};

export function getTournamentStageTypeLabel(stageType: TournamentStageType): string {
  return TOURNAMENT_STAGE_TYPE_LABELS[stageType] ?? stageType;
}

const TRANSITION_MATRIX: Record<TournamentStageType, TournamentStageType[]> = {
  ROUND_ROBIN: ['ROUND_ROBIN', 'SINGLE_ELIMINATION', 'DOUBLE_ELIMINATION'],
  SINGLE_ELIMINATION: ['SINGLE_ELIMINATION', 'CONSOLATION'],
  DOUBLE_ELIMINATION: ['ROUND_ROBIN', 'SINGLE_ELIMINATION'],
  CONSOLATION: ['ROUND_ROBIN', 'SINGLE_ELIMINATION']
};

export interface StageValidationError {
  rule: string;
  message: string;
}

export function validateStageSequence(stages: TournamentStageType[]): StageValidationError[] {
  const errors: StageValidationError[] = [];

  if (stages.length === 0) {
    errors.push({ rule: 'EMPTY', message: 'Debe haber al menos una fase definida.' });
    return errors;
  }

  if (stages[0] === 'CONSOLATION') {
    errors.push({ rule: 'R1', message: 'La primera fase no puede ser CONSOLATION. Requiere jugadores eliminados en una fase previa.' });
  }

  for (let i = 1; i < stages.length; i++) {
    const current = stages[i];
    const previous = stages[i - 1];

    if (current === 'CONSOLATION' && previous !== 'SINGLE_ELIMINATION') {
      errors.push({
        rule: 'R2',
        message: `CONSOLATION en la fase ${i + 1} solo es válida si la fase anterior es SINGLE_ELIMINATION (actual: ${previous}).`
      });
    }

    if (current === 'DOUBLE_ELIMINATION' && i + 1 < stages.length && stages[i + 1] === 'CONSOLATION') {
      errors.push({
        rule: 'R3',
        message: `Si la fase ${i + 1} es DOUBLE_ELIMINATION, la fase ${i + 2} no puede ser CONSOLATION.`
      });
    }
  }

  for (let i = 0; i < stages.length - 1; i++) {
    const current = stages[i];
    const next = stages[i + 1];
    const allowed = TRANSITION_MATRIX[current];

    if (!allowed || !allowed.includes(next)) {
      errors.push({
        rule: 'MATRIX',
        message: allowed
          ? `Transición inválida: '${current}' -> '${next}'. Desde ${current} solo se permite: ${allowed.join(', ')}.`
          : `Fase desconocida: '${current}'. No se puede validar la transición.`
      });
    }
  }

  return errors;
}

export function isConsolationDisabled(stages: TournamentStageType[]): boolean {
  if (stages.length === 0) {
    return true;
  }
  const lastStage = stages[stages.length - 1];
  return lastStage === 'DOUBLE_ELIMINATION' || lastStage === 'ROUND_ROBIN';
}

export function getAvailableStageOptions(stages: TournamentStageType[], currentIndex?: number): TournamentStageType[] {
  if (currentIndex === undefined) {
    return VALID_STAGE_TYPES;
  }

  if (currentIndex === 0) {
    return VALID_STAGE_TYPES.filter(t => t !== 'CONSOLATION');
  }

  const previous = stages[currentIndex - 1];
  if (previous === 'SINGLE_ELIMINATION') {
    return VALID_STAGE_TYPES;
  }
  return VALID_STAGE_TYPES.filter(t => t !== 'CONSOLATION');
}

export interface TournamentEventCatalogItem {
  id: number;
  category: string;
  description: string;
  custom: boolean;
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
  proPlayerId?: number | null;
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
