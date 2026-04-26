export interface MemberResponse {
  id: string;
  email: string;
  username: string;
  gender: string | null;
  tier: string;
  registeredAt: string;
}

export interface ProfileResponse {
  memberId: string;
  email: string;
  tier: string;
  registeredAt: string;
  personId: string | null;
  firstName: string | null;
  lastName: string | null;
  gender: string | null;
  birthDate: string | null;
  nationality: string | null;
  federationLicense: string | null;
}

export interface ProfileRequest {
  firstName: string;
  lastName?: string | null;
  gender: string;
  birthDate: string;
  nationality?: string | null;
  federationLicense?: string | null;
}