export type ClubRole = 'OWNER' | 'ADMIN' | 'COACH' | 'MEMBER';

export type ClubPermission =
  | 'EDIT_CLUB_PAGE'
  | 'VIEW_MEMBERS'
  | 'MANAGE_MEMBERS'
  | 'MANAGE_PERMISSIONS'
  | 'VIEW_RANKINGS'
  | 'MANAGE_TOURNAMENTS';

export interface ClubResponse {
  id: string;
  name: string;
  slug?: string | null;
  tagline?: string | null;
  logoUrl?: string | null;
  primaryColor?: string | null;
  city?: string | null;
  country?: string | null;
  memberCount?: number | null;
}

export interface ClubPageResponse {
  id: string;
  name: string;
  slug: string;
  tagline: string | null;
  description: string | null;
  logoUrl: string | null;
  bannerUrl: string | null;
  primaryColor: string | null;
  secondaryColor: string | null;
  city: string | null;
  country: string | null;
  contactEmail: string | null;
  phone: string | null;
  website: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  xUrl: string | null;
  foundedYear: number | null;
  publicPageEnabled: boolean;
  memberCount: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface ClubProfileRequest {
  name: string;
  slug?: string | null;
  tagline?: string | null;
  description?: string | null;
  logoUrl?: string | null;
  bannerUrl?: string | null;
  primaryColor?: string | null;
  secondaryColor?: string | null;
  city?: string | null;
  country?: string | null;
  contactEmail?: string | null;
  phone?: string | null;
  website?: string | null;
  instagramUrl?: string | null;
  facebookUrl?: string | null;
  xUrl?: string | null;
  foundedYear?: number | null;
  publicPageEnabled?: boolean | null;
}

export interface ClubMembershipResponse {
  membershipId: string;
  clubId: string;
  name: string;
  slug: string;
  logoUrl: string | null;
  primaryColor: string | null;
  city: string | null;
  country: string | null;
  role: ClubRole;
  permissions: ClubPermission[];
  memberCount: number;
}

export interface ClubMemberResponse {
  membershipId: string;
  memberId: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: ClubRole;
  customPermissions: ClubPermission[];
  effectivePermissions: ClubPermission[];
  joinedAt: string;
}

export interface ClubManagementResponse {
  club: ClubPageResponse;
  membership: ClubMembershipResponse;
  members: ClubMemberResponse[];
}

export interface ClubRankingResponse {
  position: number;
  personId: string;
  firstName: string;
  lastName: string | null;
  gender: string | null;
  license: string | null;
  points: number;
  victories: number;
  tournaments: number;
}

export interface ClubRolePermissionsResponse {
  role: ClubRole;
  permissions: ClubPermission[];
}

export interface ClubTournamentSummary {
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

export const CLUB_PERMISSION_LABELS: Record<ClubPermission, string> = {
  EDIT_CLUB_PAGE: 'Editar página',
  VIEW_MEMBERS: 'Ver miembros',
  MANAGE_MEMBERS: 'Gestionar miembros',
  MANAGE_PERMISSIONS: 'Gestionar permisos',
  VIEW_RANKINGS: 'Ver rankings',
  MANAGE_TOURNAMENTS: 'Organizar torneos',
};

export const CLUB_ROLE_LABELS: Record<ClubRole, string> = {
  OWNER: 'Propietario',
  ADMIN: 'Administrador',
  COACH: 'Entrenador',
  MEMBER: 'Miembro',
};
