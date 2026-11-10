import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SwPush } from '@angular/service-worker';
import { of } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { PushNotificationService } from './push-notification.service';

class MockSwPush {
  readonly isEnabled = true;
  subscription = of<PushSubscription | null>(null);
  requestSubscription = jasmine.createSpy('requestSubscription').and.resolveTo(buildSubscription());
  unsubscribe = jasmine.createSpy('unsubscribe').and.resolveTo(undefined);
}

function buildSubscription(): PushSubscription {
  return {
    endpoint: 'https://push.example/endpoint',
    expirationTime: null,
    options: {} as PushSubscriptionOptions,
    getKey: () => null,
    toJSON: () => ({
      endpoint: 'https://push.example/endpoint',
      expirationTime: null,
      keys: { p256dh: 'p256dh-key', auth: 'auth-key' },
    }),
    unsubscribe: () => Promise.resolve(true),
  } as unknown as PushSubscription;
}

describe('PushNotificationService', () => {
  let service: PushNotificationService;
  let httpTesting: HttpTestingController;
  let swPush: MockSwPush;
  const baseUrl = `${AppSettings.API_URL}/push`;

  beforeEach(() => {
    swPush = new MockSwPush();
    TestBed.configureTestingModule({
      providers: [
        PushNotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SwPush, useValue: swPush },
      ],
    });
    service = TestBed.inject(PushNotificationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should expose support in a browser with a service worker', () => {
    expect(service.supported()).toBeTrue();
  });

  it('should read the public key and reflect an existing browser subscription', async () => {
    service.initialize();

    const config = httpTesting.expectOne(`${baseUrl}/public-key`);
    expect(config.request.method).toBe('GET');
    config.flush({ publicKey: 'vapid-public-key', enabled: true });

    await Promise.resolve();
    httpTesting.expectNone(`${baseUrl}/subscriptions`);
    expect(service.enabled()).toBeTrue();
    expect(service.isSubscribed()).toBeFalse();
  });

  it('should request a subscription and send it to the backend', async () => {
    service.enable();

    httpTesting
      .expectOne(`${baseUrl}/public-key`)
      .flush({ publicKey: 'vapid-public-key', enabled: true });

    const register = await waitForRequest(httpTesting, `${baseUrl}/subscriptions`);
    expect(register.request.method).toBe('POST');
    expect(register.request.body.endpoint).toBe('https://push.example/endpoint');
    expect(register.request.body.keys).toEqual({ p256dh: 'p256dh-key', auth: 'auth-key' });
    register.flush(null);

    expect(swPush.requestSubscription).toHaveBeenCalledWith({
      serverPublicKey: 'vapid-public-key',
    });
  });

  it('should delete the backend subscription and unsubscribe in the browser', async () => {
    swPush.subscription = of<PushSubscription | null>(buildSubscription());

    service.disable();

    const removal = await waitForRequest(
      httpTesting,
      `${baseUrl}/subscriptions?endpoint=https://push.example/endpoint`,
    );
    expect(removal.request.method).toBe('DELETE');
    removal.flush(null);
    await Promise.resolve();
    await Promise.resolve();

    expect(swPush.unsubscribe).toHaveBeenCalled();
  });

  it('should post a test notification', () => {
    service.sendTest().subscribe((response) => {
      expect(response.delivered).toBe(1);
    });

    const request = httpTesting.expectOne(`${baseUrl}/test`);
    expect(request.request.method).toBe('POST');
    request.flush({ delivered: 1 });
  });
});

async function waitForRequest(
  httpTesting: HttpTestingController,
  url: string,
  attempts = 10,
): Promise<TestRequest> {
  for (let i = 0; i < attempts; i++) {
    const matches = httpTesting.match(url);
    if (matches.length > 0) {
      return matches[0];
    }
    await Promise.resolve();
  }
  return httpTesting.expectOne(url);
}
