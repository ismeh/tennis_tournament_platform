package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "application.pro-players.import")
public class ProPlayerImportProperties {

    private boolean enabled;
    private boolean runOnStartup;
    private String cron = "0 0 4 * * MON";
    private int batchSize = 1000;
    private List<Source> sources = new ArrayList<>(List.of(
        new Source("male", "classpath:db/seed/pro_players_male.csv"),
        new Source("female", "classpath:db/seed/pro_players_female.csv")
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public static class Source {

        private String name;
        private String location;

        public Source() {
        }

        public Source(String name, String location) {
            this.name = name;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}
