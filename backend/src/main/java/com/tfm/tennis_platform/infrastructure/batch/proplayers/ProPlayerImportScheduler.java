package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "application.pro-players.import", name = "enabled", havingValue = "true")
class ProPlayerImportScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProPlayerImportScheduler.class);

    private final ProPlayerImportService importService;
    private final ProPlayerImportProperties properties;

    ProPlayerImportScheduler(ProPlayerImportService importService, ProPlayerImportProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void importOnStartup() {
        if (properties.isRunOnStartup()) {
            runImport();
        }
    }

    @Scheduled(cron = "${application.pro-players.import.cron:0 0 4 * * MON}")
    void importOnSchedule() {
        runImport();
    }

    private void runImport() {
        try {
            importService.importConfiguredSources();
        } catch (RuntimeException exception) {
            LOGGER.error("Pro players import failed", exception);
        }
    }
}
