package com.starise.ipn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "io.ci")
@Configuration("schemaConfig")
public class SchemaConfig {
    private int defaultLimit;
    private int yearsToKeepLog;
    private String enableDebug;
}
