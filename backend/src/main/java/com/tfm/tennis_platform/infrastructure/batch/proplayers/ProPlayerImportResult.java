package com.tfm.tennis_platform.infrastructure.batch.proplayers;

public record ProPlayerImportResult(
    boolean imported,
    int rowCount,
    String checksum
) {
}
