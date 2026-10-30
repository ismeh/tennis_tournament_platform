import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppSettings } from '../../shared/constants';
import { NotificationItem } from '../interfaces/notification.model';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${AppSettings.API_URL}/notifications`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(NotificationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should fetch notifications and update signal', () => {
    const mockItems: NotificationItem[] = [
      {
        id: 'n1',
        title: 'Partido',
        message: 'Programado hoy',
        type: 'MATCH_SCHEDULED',
        linkUrl: '/torneos/1',
        read: false,
        createdAt: '2026-10-30T10:00:00'
      }
    ];

    service.fetchNotifications().subscribe((items) => {
      expect(items).toEqual(mockItems);
    });

    const req = httpTesting.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockItems);

    expect(service.notifications()).toEqual(mockItems);
  });

  it('should fetch unread count and update signal', () => {
    service.fetchUnreadCount().subscribe((res) => {
      expect(res.unreadCount).toBe(4);
    });

    const req = httpTesting.expectOne(`${baseUrl}/unread-count`);
    expect(req.request.method).toBe('GET');
    req.flush({ unreadCount: 4 });

    expect(service.unreadCount()).toBe(4);
  });

  it('should mark single notification as read and decrement unread count', () => {
    service.notifications.set([
      {
        id: 'n1',
        title: 'Partido',
        message: 'Programado hoy',
        type: 'MATCH_SCHEDULED',
        read: false,
        createdAt: '2026-10-30T10:00:00'
      }
    ]);
    service.unreadCount.set(2);

    service.markAsRead('n1').subscribe();

    const req = httpTesting.expectOne(`${baseUrl}/n1/read`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);

    expect(service.notifications()[0].read).toBeTrue();
    expect(service.unreadCount()).toBe(1);
  });

  it('should mark all notifications as read and reset unread count', () => {
    service.notifications.set([
      {
        id: 'n1',
        title: 'A',
        message: 'M',
        type: 'GENERAL',
        read: false,
        createdAt: '2026-10-30T10:00:00'
      }
    ]);
    service.unreadCount.set(5);

    service.markAllAsRead().subscribe();

    const req = httpTesting.expectOne(`${baseUrl}/read-all`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);

    expect(service.notifications()[0].read).toBeTrue();
    expect(service.unreadCount()).toBe(0);
  });

  it('should get and update preferences', () => {
    service.getPreference().subscribe((res) => {
      expect(res.channel).toBe('BOTH');
    });

    const reqGet = httpTesting.expectOne(`${baseUrl}/preferences`);
    expect(reqGet.request.method).toBe('GET');
    reqGet.flush({ channel: 'BOTH' });

    service.updatePreference('EMAIL').subscribe((res) => {
      expect(res.channel).toBe('EMAIL');
    });

    const reqPut = httpTesting.expectOne(`${baseUrl}/preferences`);
    expect(reqPut.request.method).toBe('PUT');
    expect(reqPut.request.body).toEqual({ channel: 'EMAIL' });
    reqPut.flush({ channel: 'EMAIL' });
  });
});
