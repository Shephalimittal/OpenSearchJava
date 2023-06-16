/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchIntegTestCase;
import java.lang.Thread;

import static org.opensearch.index.query.QueryBuilders.queryStringQuery;
import org.opensearch.tracing.exporter.FileSpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.opensearch.plugins.Plugin;
import org.opensearch.tracing.validator.SpanDataValidator;

import java.util.*;
import java.util.Collections;


@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 3, supportsDedicatedMasters=false)
public class RTCMultiNodeTracingEnabledSanityIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(OTelTracerModulePlugin.class);
    }

    public void testSanityChecksWhenTracingEnabled() throws Exception{
        // ENABLE TRACING
        client()
        .admin()
        .cluster()
        .prepareUpdateSettings()
        .setTransientSettings(Settings.builder().put(TracerSettings.TRACER_LEVEL_SETTING.getKey(), "DEBUG"))
        .get();

        //Create Index and ingest data
        String indexName = "test-index-11";
        Settings basicSettings = Settings.builder().put("number_of_shards", 3).put("number_of_replicas", 0).build();
        createIndex(indexName, basicSettings);
        indexRandom(true, client().prepareIndex(indexName).setId("1").setSource("field1", "the quick brown fox jumps"));

        System.out.println(client().admin().cluster().prepareHealth().get());
        System.out.println("Health info printed");

        //Configure correct Exporter for Integration tests
        FileSpanExporter f = new FileSpanExporter();

        //Make the search call;
        client().prepareSearch().setQuery(queryStringQuery("fox")).get();

        //Sleep for about 10s to wait for traces are published
        Thread.sleep(10000);

        List<SpanData> finishedSpanItems = f.getFinishedSpanItems();

        //Initialise SpanData Validator
        SpanDataValidator validator = new SpanDataValidator(finishedSpanItems);

        assertTrue(validator.AssertSpanDataNotNull());

        assertTrue(validator.AssertAllSpansHaveUniqueId());

        assertTrue(validator.AssertAllSpansAreEndedProperly());

        assertTrue(validator.AssertThereIsOnlyOneParentSpan());

        assertTrue(validator.AssertNumberOfTracesAreEqualToNumberOfRequests(1));

        assertTrue(validator.AssertAllSpansAreInOrder());

    }
}
