package com.tfm.tennis_platform.domain.models;

/**
 * Outcome of a single Web Push delivery attempt.
 *
 * {@link Status#EXPIRED} means the push service no longer knows the subscription (HTTP 404/410)
 * and the caller must remove it. {@link Status#SKIPPED} means push is not configured.
 */
public record PushDeliveryResult(Status status, Integer statusCode, String errorMessage) {

    public enum Status {
        DELIVERED,
        EXPIRED,
        FAILED,
        SKIPPED
    }

    public static PushDeliveryResult delivered() {
        return new PushDeliveryResult(Status.DELIVERED, 201, null);
    }

    public static PushDeliveryResult expired(Integer statusCode) {
        return new PushDeliveryResult(Status.EXPIRED, statusCode, null);
    }

    public static PushDeliveryResult failed(Integer statusCode, String errorMessage) {
        return new PushDeliveryResult(Status.FAILED, statusCode, errorMessage);
    }

    public static PushDeliveryResult skipped() {
        return new PushDeliveryResult(Status.SKIPPED, null, null);
    }

    public boolean isDelivered() {
        return status == Status.DELIVERED;
    }

    public boolean isExpired() {
        return status == Status.EXPIRED;
    }
}
