package com.puntomatch.infrastructure.batch.proplayers;

public record ProPlayerImportResult(
    boolean imported,
    int rowCount,
    String checksum
) {
}
