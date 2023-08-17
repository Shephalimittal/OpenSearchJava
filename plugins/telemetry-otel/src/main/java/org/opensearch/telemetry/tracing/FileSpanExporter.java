/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.telemetry.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.time.Instant;
import java.util.Collection;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *FileSpanExporter  to export to file
 */
public class FileSpanExporter implements SpanExporter {

    /**
     *FileSpanExporter  to export to file constructor
     */
    public FileSpanExporter() {
    }

    /**
     * Span Exporter type setting.
     */
    public static FileSpanExporter create() {
        return new FileSpanExporter();
    }
    @Override
    public CompletableResultCode export(Collection<SpanData> spanDataList) {
        try {
            String filePath1 = System.getProperty("tracer.exporter.filepath");
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath1+"spans.txt", true)));
            for (SpanData spanData : spanDataList) {
                writer.println(spanData);
            }
            writer.flush();
            /*
            String filePath2 = "/home/ec2-user/opensearch-3.0.0-SNAPSHOT/logs/tmpfs/";
            String filePath1 = "/home/ec2-user/opensearch-3.0.0-SNAPSHOT/logs/";
            long startTime1 = System.currentTimeMillis();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath1+"spans.txt", true)));
            for (SpanData spanData : spanDataList) {
                writer.println(spanData);
            }
            writer.flush();
            long endTime1 = System.currentTimeMillis();
            long startTime2 = System.currentTimeMillis();
            PrintWriter writer2 = new PrintWriter(new BufferedWriter(new FileWriter(filePath2+"spans.txt", true)));
            for (SpanData spanData : spanDataList) {
                writer2.println(spanData);
            }
            writer2.flush();
            long endTime2 = System.currentTimeMillis();

            PrintWriter writer3 = new PrintWriter(new BufferedWriter(new FileWriter(filePath1+"disk2.txt", true)));
            writer3.println(endTime1-startTime1);
            PrintWriter writer4 = new PrintWriter(new BufferedWriter(new FileWriter(filePath1+"tmpfs2.txt", true)));
            writer4.println(endTime2-startTime2);
            writer3.flush();
            writer4.flush();
            */

            return CompletableResultCode.ofSuccess();
        } catch (Exception e) {
            return CompletableResultCode.ofFailure();
        }
    }

    private String formatSpanData(SpanData spanData) {
        // Implement your own logic to format the SpanData as per SPA format
        StringBuilder builder = new StringBuilder();
        builder.append("Span ID: ").append(spanData.getSpanId()).append(System.lineSeparator());
        builder.append("Trace ID: ").append(spanData.getTraceId()).append(System.lineSeparator());
        builder.append("Name: ").append(spanData.getName()).append(System.lineSeparator());
        builder.append("Parent Span ID: ").append(spanData.getParentSpanId()).append(System.lineSeparator());
        builder.append("Start Time: ").append(spanData.getStartEpochNanos()).append(System.lineSeparator());
        builder.append("End Time: ").append(spanData.getEndEpochNanos()).append(System.lineSeparator());
        //builder.append("Attributes: ").append(formatAttributes(spanData.getAttributes())).append(System.lineSeparator());
        return builder.toString();
    }

    private String formatAttributes(Attributes attributes) {
        // Implement your own logic to format the attributes as per SPA format
        StringBuilder builder = new StringBuilder();
        attributes.forEach((key, value) -> builder.append(key).append("=").append(value).append(","));
        return builder.toString();
    }

    @Override
    public CompletableResultCode flush() {
        // No-op, as flushing to file is done immediately in the export method
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        // No-op, as there are no resources to clean up
        return CompletableResultCode.ofSuccess();
    }
}
