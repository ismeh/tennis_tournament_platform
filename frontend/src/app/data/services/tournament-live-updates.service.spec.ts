import { TestBed } from '@angular/core/testing';
import { AppSettings } from '../../shared/constants';
import { TournamentLiveUpdatesService } from './tournament-live-updates.service';

class MockEventSource {
  static instances: MockEventSource[] = [];
  readonly listeners = new Map<string, EventListener[]>();
  closed = false;

  constructor(readonly url: string) {
    MockEventSource.instances.push(this);
  }

  addEventListener(type: string, listener: EventListener): void {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), listener]);
  }

  removeEventListener(type: string, listener: EventListener): void {
    this.listeners.set(type, (this.listeners.get(type) ?? []).filter(current => current !== listener));
  }

  close(): void {
    this.closed = true;
  }

  emit(type: string, data: unknown): void {
    for (const listener of this.listeners.get(type) ?? []) {
      listener({ data: JSON.stringify(data) } as MessageEvent<string>);
    }
  }
}

describe('TournamentLiveUpdatesService', () => {
  let service: TournamentLiveUpdatesService;
  let originalEventSource: typeof EventSource;

  beforeEach(() => {
    originalEventSource = window.EventSource;
    MockEventSource.instances = [];
    Object.defineProperty(window, 'EventSource', {
      configurable: true,
      value: MockEventSource
    });

    TestBed.configureTestingModule({
      providers: [TournamentLiveUpdatesService]
    });

    service = TestBed.inject(TournamentLiveUpdatesService);
  });

  afterEach(() => {
    Object.defineProperty(window, 'EventSource', {
      configurable: true,
      value: originalEventSource
    });
  });

  it('should connect to the tournament updates endpoint and emit parsed match updates', () => {
    const receivedEvents: unknown[] = [];
    const subscription = service.watchTournament('tournament-id').subscribe(event => receivedEvents.push(event));

    expect(MockEventSource.instances.length).toBe(1);
    expect(MockEventSource.instances[0].url).toBe(`${AppSettings.API_URL}/tournaments/tournament-id/updates`);

    MockEventSource.instances[0].emit('match-result-updated', {
      type: 'MATCH_RESULT_UPDATED',
      tournamentId: 'tournament-id',
      matchId: 'match-id',
      occurredAt: '2026-06-11T10:00:00'
    });

    expect(receivedEvents).toEqual([
      {
        type: 'MATCH_RESULT_UPDATED',
        tournamentId: 'tournament-id',
        matchId: 'match-id',
        occurredAt: '2026-06-11T10:00:00'
      }
    ]);

    subscription.unsubscribe();
    expect(MockEventSource.instances[0].closed).toBeTrue();
  });
});
