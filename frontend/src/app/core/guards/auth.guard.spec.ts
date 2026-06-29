import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from '../auth/auth.service';

describe('authGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let isLoggedInSubject: BehaviorSubject<boolean>;

  function createMockActivatedRoute(overrides: Partial<ActivatedRouteSnapshot> = {}): ActivatedRouteSnapshot {
    return {
      url: [], params: {}, queryParams: {}, fragment: null, data: {},
      outlet: 'primary', component: null, routeConfig: null, title: undefined,
      firstChild: null, children: [], pathFromRoot: [],
      paramMap: {} as any, queryParamMap: {} as any, toString: () => '',
      root: {} as any, parent: null,
      ...overrides
    } as unknown as ActivatedRouteSnapshot;
  }

  function createMockRouterState(url: string): RouterStateSnapshot {
    return { url } as RouterStateSnapshot;
  }

  beforeEach(() => {
    isLoggedInSubject = new BehaviorSubject<boolean>(false);

    authService = jasmine.createSpyObj('AuthService', [], {
      isLoggedIn$: isLoggedInSubject.asObservable()
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        {
          provide: Router,
          useValue: {
            url: '/torneos/123',
            createUrlTree: jasmine.createSpy('createUrlTree').and.callFake(
              (commands: any[], extras?: any) => ({ commands, queryParams: extras?.queryParams } as any)
            )
          }
        }
      ]
    });
  });

  it('returns true when user is logged in', async () => {
    isLoggedInSubject.next(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(createMockActivatedRoute(), createMockRouterState('/torneos'))
    );
    const resolved = await firstValueFrom(result as any);
    expect(resolved).toBe(true);
  });

  it('redirects to /login with returnUrl when user is not logged in', async () => {
    isLoggedInSubject.next(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(createMockActivatedRoute(), createMockRouterState('/torneos/123'))
    );
    const resolved = await firstValueFrom(result as any);
    expect((resolved as any).commands).toEqual(['/login']);
    expect((resolved as any).queryParams).toEqual({ returnUrl: '/torneos/123' });
  });
});
