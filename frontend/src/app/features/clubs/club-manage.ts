import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import {
  CLUB_PERMISSION_LABELS,
  CLUB_ROLE_LABELS,
  ClubManagementResponse,
  ClubMemberResponse,
  ClubPermission,
  ClubProfileRequest,
  ClubRankingResponse,
  ClubRole,
  ClubRolePermissionsResponse,
  ClubTournamentSummary,
} from '../../data/interfaces/club.model';
import { ClubService } from '../../data/services/club.service';

type ClubTab = 'page' | 'members' | 'permissions' | 'ranking' | 'tournaments';

@Component({
  selector: 'app-club-manage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="min-h-[calc(100vh-4rem)] bg-neutral-50 py-8 sm:py-10">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        @if (isLoading()) {
          <div class="flex justify-center py-24">
            <div
              class="h-8 w-8 animate-spin rounded-full border-2 border-primary-500 border-t-transparent"
            ></div>
          </div>
        } @else if (errorMessage() && !management()) {
          <div class="rounded-2xl border border-red-200 bg-red-50 p-8 text-center text-red-700">
            <p class="font-semibold">{{ errorMessage() }}</p>
            <a
              routerLink="/mis-clubes"
              class="mt-4 inline-flex rounded-lg bg-white px-4 py-2 text-sm font-semibold"
              >Volver a mis clubes</a
            >
          </div>
        } @else if (management(); as current) {
          <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex items-center gap-4">
              <div
                class="flex h-16 w-16 items-center justify-center overflow-hidden rounded-2xl bg-white shadow-sm"
              >
                @if (current.club.logoUrl) {
                  <img
                    [src]="current.club.logoUrl"
                    [alt]="current.club.name"
                    class="h-full w-full object-cover"
                  />
                } @else {
                  <span class="text-2xl font-black text-neutral-700">{{
                    getInitial(current.club.name)
                  }}</span>
                }
              </div>
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">
                  Gestión del club
                </p>
                <h1 class="mt-1 text-2xl font-black text-neutral-950 sm:text-3xl">
                  {{ current.club.name }}
                </h1>
                <p class="mt-1 text-sm text-neutral-600">
                  {{ getRoleLabel(current.membership.role) }} ·
                  {{ current.club.memberCount }} miembros
                </p>
              </div>
            </div>
            <div class="flex flex-wrap gap-2">
              <a
                [routerLink]="['/clubes', current.club.slug]"
                class="rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
                >Ver página pública</a
              >
              <a
                routerLink="/mis-clubes"
                class="rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
                >Mis clubes</a
              >
            </div>
          </header>

          <nav
            class="mt-6 flex gap-1 overflow-x-auto rounded-xl border border-neutral-200 bg-white p-1 shadow-sm"
          >
            @for (tab of tabs; track tab.id) {
              <button
                type="button"
                class="whitespace-nowrap rounded-lg px-4 py-2.5 text-sm font-semibold transition"
                [class.bg-primary-500]="activeTab() === tab.id"
                [class.text-white]="activeTab() === tab.id"
                [class.text-neutral-600]="activeTab() !== tab.id"
                (click)="selectTab(tab.id)"
              >
                {{ tab.label }}
              </button>
            }
          </nav>

          @if (statusMessage()) {
            <div
              class="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
            >
              {{ statusMessage() }}
            </div>
          }
          @if (errorMessage()) {
            <div
              class="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {{ errorMessage() }}
            </div>
          }

          @switch (activeTab()) {
            @case ('page') {
              <div class="mt-5 rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h2 class="text-xl font-bold text-neutral-950">Personalizar página</h2>
                    <p class="mt-1 text-sm text-neutral-600">
                      Diseña la imagen pública del club, su información y redes sociales.
                    </p>
                  </div>
                  @if (!can('EDIT_CLUB_PAGE')) {
                    <span
                      class="rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-800"
                      >Sin permiso de edición</span
                    >
                  }
                </div>

                <form
                  class="mt-6 grid gap-5 sm:grid-cols-2"
                  [formGroup]="form"
                  (ngSubmit)="savePage()"
                >
                  <label class="block sm:col-span-2">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Nombre del club *</span
                    >
                    <input
                      formControlName="name"
                      type="text"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Dirección pública</span
                    >
                    <input
                      formControlName="slug"
                      type="text"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Eslogan</span>
                    <input
                      formControlName="tagline"
                      type="text"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block sm:col-span-2">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Descripción</span>
                    <textarea
                      formControlName="description"
                      rows="4"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    ></textarea>
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Ciudad</span>
                    <input
                      formControlName="city"
                      type="text"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">País</span>
                    <input
                      formControlName="country"
                      type="text"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Logo (URL)</span>
                    <input
                      formControlName="logoUrl"
                      type="url"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Portada (URL)</span
                    >
                    <input
                      formControlName="bannerUrl"
                      type="url"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Color principal</span
                    >
                    <div class="flex items-center gap-2">
                      <span
                        class="h-10 w-10 shrink-0 rounded border border-neutral-300"
                        [style.background]="form.controls.primaryColor.value"
                      ></span>
                      <input
                        formControlName="primaryColor"
                        type="text"
                        class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-primary-500"
                        [disabled]="!can('EDIT_CLUB_PAGE')"
                      />
                    </div>
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Color secundario</span
                    >
                    <div class="flex items-center gap-2">
                      <span
                        class="h-10 w-10 shrink-0 rounded border border-neutral-300"
                        [style.background]="form.controls.secondaryColor.value"
                      ></span>
                      <input
                        formControlName="secondaryColor"
                        type="text"
                        class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-primary-500"
                        [disabled]="!can('EDIT_CLUB_PAGE')"
                      />
                    </div>
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Email de contacto</span
                    >
                    <input
                      formControlName="contactEmail"
                      type="email"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Teléfono</span>
                    <input
                      formControlName="phone"
                      type="text"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Web</span>
                    <input
                      formControlName="website"
                      type="url"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Año de fundación</span
                    >
                    <input
                      formControlName="foundedYear"
                      type="number"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Instagram (URL)</span
                    >
                    <input
                      formControlName="instagramUrl"
                      type="url"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700"
                      >Facebook (URL)</span
                    >
                    <input
                      formControlName="facebookUrl"
                      type="url"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">X (URL)</span>
                    <input
                      formControlName="xUrl"
                      type="url"
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                  </label>
                  <label
                    class="flex items-center gap-3 rounded-lg border border-neutral-200 bg-neutral-50 px-4 py-3 sm:col-span-2"
                  >
                    <input
                      formControlName="publicPageEnabled"
                      type="checkbox"
                      class="h-4 w-4 rounded border-neutral-300 text-primary-600"
                      [disabled]="!can('EDIT_CLUB_PAGE')"
                    />
                    <span class="text-sm font-medium text-neutral-700"
                      >Página pública visible en el directorio de clubes</span
                    >
                  </label>

                  <div class="flex gap-3 sm:col-span-2">
                    <button
                      type="submit"
                      class="rounded-xl bg-primary-500 px-5 py-2.5 text-sm font-bold text-white hover:bg-primary-600 disabled:opacity-60"
                      [disabled]="!can('EDIT_CLUB_PAGE') || isSavingPage()"
                    >
                      {{ isSavingPage() ? 'Guardando...' : 'Guardar página' }}
                    </button>
                  </div>
                </form>
              </div>
            }

            @case ('members') {
              <div class="mt-5 grid gap-5 lg:grid-cols-[1fr_20rem]">
                <div class="rounded-2xl border border-neutral-200 bg-white shadow-sm">
                  <div class="border-b border-neutral-200 px-6 py-5">
                    <h2 class="text-xl font-bold text-neutral-950">Miembros del club</h2>
                    <p class="mt-1 text-sm text-neutral-600">
                      Roles, permisos y accesos al panel de gestión.
                    </p>
                  </div>

                  <div class="divide-y divide-neutral-100">
                    @for (member of current.members; track member.membershipId) {
                      <div class="px-6 py-4">
                        <div
                          class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
                        >
                          <div class="flex items-center gap-3">
                            <div
                              class="flex h-10 w-10 items-center justify-center rounded-full bg-neutral-100 text-sm font-bold text-neutral-600"
                            >
                              {{ getInitial(getMemberName(member)) }}
                            </div>
                            <div>
                              <p class="font-semibold text-neutral-950">
                                {{ getMemberName(member) }}
                              </p>
                              <p class="text-xs text-neutral-500">{{ member.email }}</p>
                            </div>
                          </div>
                          <div class="flex flex-wrap items-center gap-2">
                            @if (member.role === 'OWNER' || !can('MANAGE_PERMISSIONS')) {
                              <span
                                class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-bold text-neutral-700"
                                >{{ getRoleLabel(member.role) }}</span
                              >
                            } @else {
                              <select
                                class="h-9 rounded-lg border border-neutral-300 bg-white px-2 text-sm text-neutral-900 outline-none focus:border-primary-500"
                                [value]="member.role"
                                (change)="changeRole(member, $event)"
                              >
                                @for (role of assignableRoles; track role) {
                                  <option [value]="role">{{ getRoleLabel(role) }}</option>
                                }
                              </select>
                            }
                            @if (member.role !== 'OWNER' && can('MANAGE_PERMISSIONS')) {
                              <button
                                type="button"
                                class="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 hover:bg-neutral-50"
                                (click)="togglePermissions(member)"
                              >
                                Permisos
                              </button>
                            }
                            @if (member.role !== 'OWNER' && can('MANAGE_MEMBERS')) {
                              <button
                                type="button"
                                class="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-50"
                                (click)="removeMember(member)"
                              >
                                Eliminar
                              </button>
                            }
                          </div>
                        </div>

                        @if (expandedMemberId() === member.membershipId) {
                          <div class="mt-4 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                            <p
                              class="text-xs font-semibold uppercase tracking-wide text-neutral-500"
                            >
                              Permisos adicionales
                            </p>
                            <div class="mt-3 grid gap-2 sm:grid-cols-2">
                              @for (permission of allPermissions; track permission) {
                                <label class="flex items-center gap-2 text-sm text-neutral-700">
                                  <input
                                    type="checkbox"
                                    class="h-4 w-4 rounded border-neutral-300 text-primary-600"
                                    [checked]="isPermissionSelected(member, permission)"
                                    (change)="togglePermission(member, permission, $event)"
                                  />
                                  {{ getPermissionLabel(permission) }}
                                </label>
                              }
                            </div>
                            <button
                              type="button"
                              class="mt-4 rounded-lg bg-neutral-900 px-4 py-2 text-xs font-bold text-white hover:bg-neutral-800"
                              (click)="savePermissions(member)"
                            >
                              Guardar permisos
                            </button>
                          </div>
                        }
                      </div>
                    }
                  </div>
                </div>

                <aside class="space-y-5">
                  @if (can('MANAGE_MEMBERS')) {
                    <div class="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                      <h3 class="text-lg font-bold text-neutral-950">Añadir miembro</h3>
                      <p class="mt-1 text-xs text-neutral-600">
                        La persona debe tener una cuenta en PuntoMatch.
                      </p>
                      <div class="mt-4 space-y-3">
                        <input
                          type="email"
                          placeholder="email@ejemplo.com"
                          class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-primary-500"
                          [value]="newMemberEmail()"
                          (input)="onNewMemberEmail($event)"
                        />
                        <select
                          class="h-10 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none focus:border-primary-500"
                          [value]="newMemberRole()"
                          (change)="onNewMemberRole($event)"
                        >
                          @for (role of assignableRoles; track role) {
                            <option [value]="role">{{ getRoleLabel(role) }}</option>
                          }
                        </select>
                        <button
                          type="button"
                          class="w-full rounded-lg bg-primary-500 px-4 py-2.5 text-sm font-bold text-white hover:bg-primary-600 disabled:opacity-60"
                          [disabled]="isAddingMember()"
                          (click)="addMember()"
                        >
                          {{ isAddingMember() ? 'Añadiendo...' : 'Añadir al club' }}
                        </button>
                      </div>
                    </div>
                  }

                  <div class="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                    <h3 class="text-lg font-bold text-neutral-950">Roles del club</h3>
                    <ul class="mt-3 space-y-2 text-sm text-neutral-600">
                      <li><strong class="text-neutral-900">Propietario:</strong> control total.</li>
                      <li>
                        <strong class="text-neutral-900">Administrador:</strong> página, miembros y
                        torneos.
                      </li>
                      <li>
                        <strong class="text-neutral-900">Entrenador:</strong> consulta miembros y
                        ranking.
                      </li>
                      <li>
                        <strong class="text-neutral-900">Miembro:</strong> consulta el ranking del
                        club.
                      </li>
                    </ul>
                  </div>
                </aside>
              </div>
            }

            @case ('permissions') {
              <div class="mt-5 rounded-2xl border border-neutral-200 bg-white shadow-sm">
                <div class="border-b border-neutral-200 px-6 py-5">
                  <h2 class="text-xl font-bold text-neutral-950">Matriz de permisos</h2>
                  <p class="mt-1 text-sm text-neutral-600">
                    Permisos incluidos por cada rol. Los permisos adicionales se asignan por
                    miembro.
                  </p>
                </div>
                <div class="overflow-x-auto">
                  <table class="min-w-full divide-y divide-neutral-200 text-sm">
                    <thead
                      class="bg-neutral-50 text-left text-xs font-semibold uppercase tracking-widest text-neutral-500"
                    >
                      <tr>
                        <th class="px-6 py-3">Permiso</th>
                        @for (role of roles; track role) {
                          <th class="px-4 py-3 text-center">{{ getRoleLabel(role) }}</th>
                        }
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-neutral-100">
                      @for (permission of allPermissions; track permission) {
                        <tr>
                          <td class="px-6 py-3 font-medium text-neutral-800">
                            {{ getPermissionLabel(permission) }}
                          </td>
                          @for (role of roles; track role) {
                            <td class="px-4 py-3 text-center">
                              @if (roleHasPermission(role, permission)) {
                                <span class="text-emerald-600">✓</span>
                              } @else {
                                <span class="text-neutral-300">—</span>
                              }
                            </td>
                          }
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>
            }

            @case ('ranking') {
              <div class="mt-5 rounded-2xl border border-neutral-200 bg-white shadow-sm">
                <div
                  class="flex flex-col gap-3 border-b border-neutral-200 px-6 py-5 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <h2 class="text-xl font-bold text-neutral-950">Ranking del club</h2>
                    <p class="mt-1 text-sm text-neutral-600">
                      Puntos y victorias de los jugadores del club.
                    </p>
                  </div>
                  <select
                    class="h-10 rounded-lg border border-neutral-300 bg-white px-3 text-sm"
                    [value]="rankingGender()"
                    (change)="onRankingGenderChange($event)"
                  >
                    <option value="">Todos</option>
                    <option value="MALE">Masculino</option>
                    <option value="FEMALE">Femenino</option>
                    <option value="MIXED">Mixto</option>
                  </select>
                </div>
                @if (ranking().length === 0) {
                  <div class="p-8 text-center text-sm text-neutral-600">
                    Aún no hay resultados para este club.
                  </div>
                } @else {
                  <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-neutral-200 text-sm">
                      <thead
                        class="bg-neutral-50 text-left text-xs font-semibold uppercase tracking-widest text-neutral-500"
                      >
                        <tr>
                          <th class="px-6 py-3">#</th>
                          <th class="px-6 py-3">Jugador</th>
                          <th class="px-6 py-3 text-right">Puntos</th>
                          <th class="px-6 py-3 text-right">Victorias</th>
                          <th class="px-6 py-3 text-right">Torneos</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-neutral-100">
                        @for (player of ranking(); track player.personId) {
                          <tr>
                            <td class="px-6 py-4 font-black text-neutral-950">
                              {{ player.position }}
                            </td>
                            <td class="px-6 py-4 font-semibold text-neutral-900">
                              {{ getPlayerName(player) }}
                            </td>
                            <td class="px-6 py-4 text-right">{{ player.points }}</td>
                            <td class="px-6 py-4 text-right">{{ player.victories }}</td>
                            <td class="px-6 py-4 text-right">{{ player.tournaments }}</td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                }
              </div>
            }

            @case ('tournaments') {
              <div class="mt-5 rounded-2xl border border-neutral-200 bg-white shadow-sm">
                <div class="border-b border-neutral-200 px-6 py-5">
                  <h2 class="text-xl font-bold text-neutral-950">Torneos del club</h2>
                  <p class="mt-1 text-sm text-neutral-600">
                    Torneos vinculados a esta organización.
                  </p>
                </div>
                @if (tournaments().length === 0) {
                  <div class="p-8 text-center text-sm text-neutral-600">
                    No hay torneos vinculados al club.
                  </div>
                } @else {
                  <ul class="divide-y divide-neutral-100">
                    @for (tournament of tournaments(); track tournament.id) {
                      <li class="flex items-center justify-between gap-4 px-6 py-4">
                        <div>
                          <p class="font-semibold text-neutral-950">{{ tournament.formalName }}</p>
                          <p class="mt-1 text-xs text-neutral-500">
                            {{ tournament.playStartDate }} · {{ tournament.location }}
                          </p>
                        </div>
                        <a
                          [routerLink]="['/torneos', tournament.id]"
                          class="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 hover:bg-neutral-50"
                          >Abrir</a
                        >
                      </li>
                    }
                  </ul>
                }
              </div>
            }
          }
        }
      </div>
    </section>
  `,
})
export class ClubManageComponent implements OnInit {
  private readonly clubService = inject(ClubService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly tabs: { id: ClubTab; label: string }[] = [
    { id: 'page', label: 'Página' },
    { id: 'members', label: 'Miembros' },
    { id: 'permissions', label: 'Permisos' },
    { id: 'ranking', label: 'Ranking' },
    { id: 'tournaments', label: 'Torneos' },
  ];

  readonly allPermissions: ClubPermission[] = [
    'EDIT_CLUB_PAGE',
    'VIEW_MEMBERS',
    'MANAGE_MEMBERS',
    'MANAGE_PERMISSIONS',
    'VIEW_RANKINGS',
    'MANAGE_TOURNAMENTS',
  ];
  readonly roles: ClubRole[] = ['OWNER', 'ADMIN', 'COACH', 'MEMBER'];
  readonly assignableRoles: ClubRole[] = ['ADMIN', 'COACH', 'MEMBER'];

  readonly management = signal<ClubManagementResponse | null>(null);
  readonly catalog = signal<ClubRolePermissionsResponse[]>([]);
  readonly ranking = signal<ClubRankingResponse[]>([]);
  readonly tournaments = signal<ClubTournamentSummary[]>([]);
  readonly activeTab = signal<ClubTab>('page');
  readonly expandedMemberId = signal<string | null>(null);
  readonly permissionDraft = signal<Record<string, ClubPermission[]>>({});
  readonly newMemberEmail = signal('');
  readonly newMemberRole = signal<ClubRole>('MEMBER');
  readonly rankingGender = signal('');
  readonly isLoading = signal(true);
  readonly isSavingPage = signal(false);
  readonly isAddingMember = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly statusMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    slug: [''],
    tagline: [''],
    description: [''],
    city: [''],
    country: [''],
    logoUrl: [''],
    bannerUrl: [''],
    primaryColor: ['#16a34a'],
    secondaryColor: ['#0284c7'],
    contactEmail: [''],
    phone: [''],
    website: [''],
    foundedYear: [null as number | null],
    instagramUrl: [''],
    facebookUrl: [''],
    xUrl: [''],
    publicPageEnabled: [true],
  });

  ngOnInit(): void {
    const clubId = this.route.snapshot.paramMap.get('clubId') ?? '';
    this.clubService.getPermissionCatalog().subscribe({
      next: (catalog) => this.catalog.set(catalog),
      error: () => this.catalog.set([]),
    });
    this.loadManagement(clubId);
  }

  selectTab(tab: ClubTab): void {
    this.activeTab.set(tab);
    this.statusMessage.set(null);
    this.errorMessage.set(null);
    if (tab === 'ranking') {
      this.loadRanking();
    }
    if (tab === 'tournaments') {
      this.loadTournaments();
    }
  }

  can(permission: ClubPermission): boolean {
    return this.management()?.membership.permissions.includes(permission) ?? false;
  }

  savePage(): void {
    const current = this.management();
    if (!current || this.form.invalid || this.isSavingPage()) {
      return;
    }

    const value = this.form.getRawValue();
    const payload: ClubProfileRequest = {
      name: value.name,
      slug: value.slug || null,
      tagline: value.tagline || null,
      description: value.description || null,
      city: value.city || null,
      country: value.country || null,
      logoUrl: value.logoUrl || null,
      bannerUrl: value.bannerUrl || null,
      primaryColor: value.primaryColor || null,
      secondaryColor: value.secondaryColor || null,
      contactEmail: value.contactEmail || null,
      phone: value.phone || null,
      website: value.website || null,
      foundedYear: value.foundedYear ?? null,
      instagramUrl: value.instagramUrl || null,
      facebookUrl: value.facebookUrl || null,
      xUrl: value.xUrl || null,
      publicPageEnabled: value.publicPageEnabled,
    };

    this.isSavingPage.set(true);
    this.errorMessage.set(null);
    this.clubService.updatePage(current.club.id, payload).subscribe({
      next: (club) => {
        this.isSavingPage.set(false);
        this.management.set({ ...current, club });
        this.statusMessage.set('Página del club actualizada.');
      },
      error: (error) => {
        this.isSavingPage.set(false);
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo guardar la página del club.'));
      },
    });
  }

  changeRole(member: ClubMemberResponse, event: Event): void {
    const current = this.management();
    if (!current) {
      return;
    }
    const role = (event.target as HTMLSelectElement).value as ClubRole;
    this.clubService.updateMemberRole(current.club.id, member.membershipId, role).subscribe({
      next: (updated) => {
        this.replaceMember(updated);
        this.statusMessage.set('Rol actualizado.');
      },
      error: (error) =>
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cambiar el rol.')),
    });
  }

  togglePermissions(member: ClubMemberResponse): void {
    const isOpen = this.expandedMemberId() === member.membershipId;
    this.expandedMemberId.set(isOpen ? null : member.membershipId);
    if (!isOpen) {
      this.permissionDraft.update((draft) => ({
        ...draft,
        [member.membershipId]: [...member.customPermissions],
      }));
    }
  }

  togglePermission(member: ClubMemberResponse, permission: ClubPermission, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.permissionDraft.update((draft) => {
      const current = new Set(draft[member.membershipId] ?? member.customPermissions);
      if (checked) {
        current.add(permission);
      } else {
        current.delete(permission);
      }
      return { ...draft, [member.membershipId]: [...current] };
    });
  }

  isPermissionSelected(member: ClubMemberResponse, permission: ClubPermission): boolean {
    const draft = this.permissionDraft()[member.membershipId];
    return (draft ?? member.customPermissions).includes(permission);
  }

  savePermissions(member: ClubMemberResponse): void {
    const current = this.management();
    if (!current) {
      return;
    }
    const permissions = this.permissionDraft()[member.membershipId] ?? member.customPermissions;
    this.clubService
      .updateMemberPermissions(current.club.id, member.membershipId, permissions)
      .subscribe({
        next: (updated) => {
          this.replaceMember(updated);
          this.statusMessage.set('Permisos actualizados.');
        },
        error: (error) =>
          this.errorMessage.set(getApiErrorMessage(error, 'No se pudieron guardar los permisos.')),
      });
  }

  removeMember(member: ClubMemberResponse): void {
    const current = this.management();
    if (!current || !confirm(`¿Eliminar a ${this.getMemberName(member)} del club?`)) {
      return;
    }
    this.clubService.removeMember(current.club.id, member.membershipId).subscribe({
      next: () => {
        this.management.set({
          ...current,
          members: current.members.filter((item) => item.membershipId !== member.membershipId),
          club: { ...current.club, memberCount: Math.max(0, current.club.memberCount - 1) },
        });
        this.statusMessage.set('Miembro eliminado del club.');
      },
      error: (error) =>
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo eliminar el miembro.')),
    });
  }

  addMember(): void {
    const current = this.management();
    const email = this.newMemberEmail().trim();
    if (!current || !email || this.isAddingMember()) {
      return;
    }

    this.isAddingMember.set(true);
    this.clubService.addMember(current.club.id, email, this.newMemberRole()).subscribe({
      next: (member) => {
        this.management.set({
          ...current,
          members: [...current.members, member],
          club: { ...current.club, memberCount: current.club.memberCount + 1 },
        });
        this.newMemberEmail.set('');
        this.isAddingMember.set(false);
        this.statusMessage.set('Miembro añadido al club.');
      },
      error: (error) => {
        this.isAddingMember.set(false);
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo añadir el miembro.'));
      },
    });
  }

  onNewMemberEmail(event: Event): void {
    this.newMemberEmail.set((event.target as HTMLInputElement).value);
  }

  onNewMemberRole(event: Event): void {
    this.newMemberRole.set((event.target as HTMLSelectElement).value as ClubRole);
  }

  onRankingGenderChange(event: Event): void {
    this.rankingGender.set((event.target as HTMLSelectElement).value);
    this.loadRanking();
  }

  roleHasPermission(role: ClubRole, permission: ClubPermission): boolean {
    return (
      this.catalog()
        .find((item) => item.role === role)
        ?.permissions.includes(permission) ?? false
    );
  }

  getPermissionLabel(permission: ClubPermission): string {
    return CLUB_PERMISSION_LABELS[permission] ?? permission;
  }

  getRoleLabel(role: ClubRole): string {
    return CLUB_ROLE_LABELS[role] ?? role;
  }

  getMemberName(member: ClubMemberResponse): string {
    return [member.firstName, member.lastName].filter(Boolean).join(' ') || member.email;
  }

  getPlayerName(player: ClubRankingResponse): string {
    return [player.firstName, player.lastName].filter(Boolean).join(' ') || 'Jugador sin nombre';
  }

  getInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || 'C';
  }

  private loadManagement(clubId: string): void {
    this.isLoading.set(true);
    this.clubService.getManagement(clubId).subscribe({
      next: (management) => {
        this.management.set(management);
        this.patchForm(management);
        this.isLoading.set(false);
        this.loadRanking();
        this.loadTournaments();
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar la gestión del club.'));
        this.isLoading.set(false);
      },
    });
  }

  private patchForm(management: ClubManagementResponse): void {
    const club = management.club;
    this.form.patchValue({
      name: club.name,
      slug: club.slug,
      tagline: club.tagline ?? '',
      description: club.description ?? '',
      city: club.city ?? '',
      country: club.country ?? '',
      logoUrl: club.logoUrl ?? '',
      bannerUrl: club.bannerUrl ?? '',
      primaryColor: club.primaryColor ?? '#16a34a',
      secondaryColor: club.secondaryColor ?? '#0284c7',
      contactEmail: club.contactEmail ?? '',
      phone: club.phone ?? '',
      website: club.website ?? '',
      foundedYear: club.foundedYear ?? null,
      instagramUrl: club.instagramUrl ?? '',
      facebookUrl: club.facebookUrl ?? '',
      xUrl: club.xUrl ?? '',
      publicPageEnabled: club.publicPageEnabled,
    });
  }

  private replaceMember(updated: ClubMemberResponse): void {
    const current = this.management();
    if (!current) {
      return;
    }
    this.management.set({
      ...current,
      members: current.members.map((member) =>
        member.membershipId === updated.membershipId ? updated : member,
      ),
    });
  }

  private loadRanking(): void {
    const clubId = this.management()?.club.id;
    if (!clubId) {
      return;
    }
    this.clubService.getManagementRanking(clubId, this.rankingGender() || null).subscribe({
      next: (ranking) => this.ranking.set(ranking),
      error: () => this.ranking.set([]),
    });
  }

  private loadTournaments(): void {
    const clubId = this.management()?.club.id;
    if (!clubId) {
      return;
    }
    this.clubService.getManagementTournaments(clubId).subscribe({
      next: (tournaments) => this.tournaments.set(tournaments),
      error: () => this.tournaments.set([]),
    });
  }
}
