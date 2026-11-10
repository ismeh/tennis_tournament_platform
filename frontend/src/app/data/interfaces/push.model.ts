export interface PushPublicKeyResponse {
  publicKey: string | null;
  enabled: boolean;
}

export interface PushSubscriptionKeys {
  p256dh: string;
  auth: string;
}

export interface PushSubscriptionRequest {
  endpoint: string;
  keys: PushSubscriptionKeys;
  userAgent?: string;
}

export interface PushTestResponse {
  delivered: number;
}
