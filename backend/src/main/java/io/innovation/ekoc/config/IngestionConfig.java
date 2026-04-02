package io.innovation.ekoc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ingestion")
public class IngestionConfig {

    private int chunkSize = 1000;
    private int chunkOverlap = 200;
    private List<String> supportedTypes;
}
