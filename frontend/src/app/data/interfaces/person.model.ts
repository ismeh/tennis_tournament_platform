export type ParticipantSource = 'EXISTING_PERSON' | 'MANUAL' | 'PROFESSIONAL';

export interface PersonSearchResponse {
  id: string;
  tennisId: string | null;
  firstName: string;
  lastName: string | null;
  nationality: string | null;
  birthDate: string | null;
  gender: string | null;
}
