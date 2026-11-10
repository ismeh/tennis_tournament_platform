package com.tfm.tennis_platform.domain.models;

/**
 * Content of a Web Push notification.
 *
 * @param title notification title shown by the operating system
 * @param body  short message
 * @param url   in-app route opened when the user taps the notification
 * @param icon  optional icon path, resolved by the browser against the app origin
 * @param tag   optional tag used to collapse notifications of the same kind
 */
public record PushMessage(String title, String body, String url, String icon, String tag) {
}
