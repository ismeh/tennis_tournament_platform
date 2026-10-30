import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import {
  NotificationChannel,
  NotificationChannelPreference,
  NotificationItem,
  NotificationUnreadCount
} from '../interfaces/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${AppSettings.API_URL}/notifications`;

  readonly unreadCount = signal<number>(0);
  readonly notifications = signal<NotificationItem[]>([]);

  fetchNotifications(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(this.baseUrl).pipe(
      tap((items) => this.notifications.set(items))
    );
  }

  fetchUnreadCount(): Observable<NotificationUnreadCount> {
    return this.http.get<NotificationUnreadCount>(`${this.baseUrl}/unread-count`).pipe(
      tap((res) => this.unreadCount.set(res.unreadCount))
    );
  }

  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/read`, {}).pipe(
      tap(() => {
        this.notifications.update((list) =>
          list.map((n) => (n.id === id ? { ...n, read: true } : n))
        );
        this.unreadCount.update((count) => Math.max(0, count - 1));
      })
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/read-all`, {}).pipe(
      tap(() => {
        this.notifications.update((list) => list.map((n) => ({ ...n, read: true })));
        this.unreadCount.set(0);
      })
    );
  }

  getPreference(): Observable<NotificationChannelPreference> {
    return this.http.get<NotificationChannelPreference>(`${this.baseUrl}/preferences`);
  }

  updatePreference(channel: NotificationChannel): Observable<NotificationChannelPreference> {
    return this.http.put<NotificationChannelPreference>(`${this.baseUrl}/preferences`, { channel });
  }
}
