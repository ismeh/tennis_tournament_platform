export type NotificationChannel = 'APP' | 'EMAIL' | 'BOTH' | 'NONE';

export type NotificationType = 'MATCH_SCHEDULED' | 'MATCH_RESULT' | 'TOURNAMENT_UPDATE' | 'GENERAL';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  linkUrl?: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationUnreadCount {
  unreadCount: number;
}

export interface NotificationChannelPreference {
  channel: NotificationChannel;
}
