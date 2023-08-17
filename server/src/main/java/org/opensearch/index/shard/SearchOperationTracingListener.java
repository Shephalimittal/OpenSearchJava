/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.shard;

import org.opensearch.search.internal.SearchContext;
import org.opensearch.telemetry.tracing.SpanScope;
import org.opensearch.telemetry.tracing.Tracer;

/**
 * Replication group for a shard. Used by a primary shard to coordinate replication and recoveries.
 *
 * @opensearch.internal
 */
public class SearchOperationTracingListener implements SearchOperationListener {

    private final Tracer tracer;

    public SearchOperationTracingListener(Tracer tracer) {
        this.tracer = tracer;
    }

    private SpanScope spanScope;

    @Override
    public void onPreQueryPhase(SearchContext searchContext) {
        this.spanScope = tracer.startSpan("queryPhase_" + searchContext.shardTarget().getFullyQualifiedIndexName());
    }

    @Override
    public void onQueryPhase(SearchContext searchContext, long tookInNanos) {
        this.spanScope.close();
        //tracerFactory.getTracer().endSpan();
    }

    @Override
    public void onPreFetchPhase(SearchContext searchContext) {
        this.spanScope = tracer.startSpan("fetchPhase_" + searchContext.shardTarget().getFullyQualifiedIndexName());
    }

    @Override
    public void onFetchPhase(SearchContext searchContext, long tookInNanos) {
        this.spanScope.close();
        //tracerFactory.getTracer().endSpan();
    }
}
