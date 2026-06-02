package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProPlayerImportProperties.class)
class ProPlayerImportConfiguration {
}
