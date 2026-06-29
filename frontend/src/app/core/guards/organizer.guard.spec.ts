import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { organizerGuard } from './organizer.guard';
import { AuthService } from '../auth/auth.service';

describe('organizerGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let roleSubject: BehaviorSubject<string | null>;

  function createMockActivatedRoute(): ActivatedRouteSnapshot {
    return {
      url: [], params: {}, queryParams: {}, fragment: null, data: {},
      outlet: 'primary', component: null, routeConfig: null, title: undefined,
      firstChild: null, children: [], pathFromRoot: [],
      paramMap: {} as any, queryParamMap: {} as any, toString: () => '',
      root: {} as any, parent: null
    } as unknown as ActivatedRouteSnapshot;
  }

  function createMockRouterState(url: string): RouterStateSnapshot {
    return { url } as RouterStateSnapshot;
  }

  beforeEach(() => {
    roleSubject = new BehaviorSubject<string | null>(null);

    authService = jasmine.createSpyObj('AuthService', [], {
      role$: roleSubject.asObservable()
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        {
          provide: Router,
          useValue: {
            createUrlTree: jasmine.createSpy('createUrlTree').and.callFake(
              (commands: any[]) => ({ commands } as any)
            )
          }
        }
      ]
    });
  });

  it('returns true when role is ORGANIZER', async () => {
    roleSubject.next('ORGANIZER');

    const result = TestBed.runInInjectionContext(() =>
      organizerGuard(createMockActivatedRoute(), createMockRouterState('/'))
    );
    const resolved = await firstValueFrom(result as any);
    expect(resolved).toBe(true);
  });

  it('redirects to / when role is PLAYER', async () => {
    roleSubject.next('PLAYER');

    const result = TestBed.runInInjectionContext(() =>
      organizerGuard(createMockActivatedRoute(), createMockRouterState('/'))
    );
    const resolved = await firstValueFrom(result as any);
    expect((resolved as any).commands).toEqual(['/']);
  });

  it('redirects to / when role is UMPIRE', async () => {
    roleSubject.next('UMPIRE');

    const result = TestBed.runInInjectionContext(() =>
      organizerGuard(createMockActivatedRoute(), createMockRouterState('/'))
    );
    const resolved = await firstValueFrom(result as any);
    expect((resolved as any).commands).toEqual(['/']);
  });

  it('redirects to / when role is null', async () => {
    roleSubject.next(null);

    const result = TestBed.runInInjectionContext(() =>
      organizerGuard(createMockActivatedRoute(), createMockRouterState('/'))
    );
    const resolved = await firstValueFrom(result as any);
    expect((resolved as any).commands).toEqual(['/']);
  });
});
