/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.telemetry.tracing;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 *FileSpanExporterProvider  to export to file
 */
public class FileSpanExporterProvider implements ConfigurableSpanExporterProvider {

    /**
     *FileSpanExporterProvider  to export to file
     */
    public FileSpanExporterProvider() {}
    @Override
    public SpanExporter createExporter(ConfigProperties config) {
        return new FileSpanExporter();
    }

    @Override
    public String getName() {
        return "fileexporter";
    }
}
