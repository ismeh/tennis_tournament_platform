export interface ProPlayerSearchResponse {
  id: number;
  license: string | null;
  fullName: string;
  firstName: string;
  lastName: string | null;
  rankingPosition: number | null;
  ageCategory: string | null;
  clubName: string | null;
  birthDate: string | null;
  gender: string | null;
}
