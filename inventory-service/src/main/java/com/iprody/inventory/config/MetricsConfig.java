package com.iprody.inventory.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MetricsConfig {

    @Value("${spring.application.name:inventory-service}")
    private String applicationName;

    @Value("${management.metrics.export.logging.step:1}")
    private Integer loggingStep;

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", applicationName);
    }

    @Bean
    public LoggingMeterRegistry customLoggingMeterRegistry() {
        LoggingRegistryConfig config = new LoggingRegistryConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Duration step() {
                return Duration.ofMinutes(loggingStep);
            }
        };

        final String logFilePath = "logs/technical-metrics-" + applicationName + ".log";

        final List<String> metricsBatch = new ArrayList<>();

        return new LoggingMeterRegistry(config, Clock.SYSTEM, out -> {
            String cleanLine = out.replace("throughput=", "tp=");
            metricsBatch.add(cleanLine);

            if (out.contains("jvm.gc.pause") || out.contains("system.load")) {
                try {
                    Path path = Paths.get(logFilePath);
                    if (path.getParent() != null) {
                        Files.createDirectories(path.getParent());
                    }

                    try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
                        writeMetricsTable(writer, metricsBatch);
                    }
                } catch (IOException e) {
                    LoggerFactory.getLogger(MetricsConfig.class).error("Failed to write metrics table", e);
                } finally {
                    metricsBatch.clear();
                }
            }
        });
    }

    private void writeMetricsTable(PrintWriter writer, List<String> metrics) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        writer.println("\n+" + "-".repeat(110) + "+");
        writer.printf("| METRICS REPORT FOR %-90s |\n", timestamp);
        writer.println("+" + "-".repeat(110) + "+");
        writer.printf("| %-55s | %-50s |\n", "METRIC NAME & TAGS", "VALUE / DETAILS");
        writer.println("+" + "-".repeat(110) + "+");

        for (String metric : metrics) {
            String[] parts = metric.split(" value=| delta_count=");
            if (parts.length == 2) {
                String nameAndTags = parts[0].trim();
                String values = parts[1].trim();

                int braceIndex = nameAndTags.indexOf('{');
                if (braceIndex != -1) {
                    nameAndTags = nameAndTags.substring(0, braceIndex).trim();
                }

                if (nameAndTags.length() > 55) {
                    nameAndTags = nameAndTags.substring(0, 52) + "...";
                }
                if (values.length() > 50) {
                    values = values.substring(0, 47) + "...";
                }

                writer.printf("| %-55s | %-50s |\n", nameAndTags, values);
            }
        }
        writer.println("+" + "-".repeat(110) + "+");
    }

    @Bean
    public MeterFilter filterOnlyTechnicalMetrics() {
        return MeterFilter.denyUnless(id ->
                id.getName().startsWith("jvm.") ||
                        id.getName().startsWith("system.") ||
                        id.getName().startsWith("process.") ||
                        id.getName().startsWith("disk.")
        );
    }
}
