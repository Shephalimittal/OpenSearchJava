/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import io.opentelemetry.sdk.trace.data.SpanData;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchIntegTestCase;
import org.opensearch.tracing.exporter.FileSpanExporter;
import org.opensearch.tracing.validator.SpanDataValidator;

import java.util.List;

import static org.opensearch.index.query.QueryBuilders.queryStringQuery;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST)
public class RTCTracingDisabledSanityIT extends OpenSearchIntegTestCase {
    public void testSanityChecksWhenTracingDisabled() throws Exception{
        // TRACING IS DISABLED
        client().admin().cluster()
            .prepareUpdateSettings()
            .setPersistentSettings(
                Settings.builder().putNull("tracer.level")
                    .build()
            )
            .get();

        //Create Index and ingest data
        String indexName = "test-index-1";
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

        assertFalse(validator.AssertSpanDataNotNull());

    }
}
