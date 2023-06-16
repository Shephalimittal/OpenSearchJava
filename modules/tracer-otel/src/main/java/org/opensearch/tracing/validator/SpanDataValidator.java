/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing.validator;

import io.opentelemetry.sdk.trace.data.SpanData;

import java.util.*;

public class SpanDataValidator {
    List<SpanData> SpanDataItems = new ArrayList<>();

    public SpanDataValidator(List<SpanData> spanDataItems) {
        this.SpanDataItems = spanDataItems;
    }

    public boolean AssertSpanDataNotNull(){
        return this.SpanDataItems.size()>0;
    }

    public boolean AssertAllSpansHaveUniqueId() {
        Set<String> uniqueIds = new HashSet<>();
        for (SpanData i : this.SpanDataItems) {
            uniqueIds.add(i.getSpanId());
        }
        return Integer.compare(uniqueIds.size(), this.SpanDataItems.size()) == 0;
    }

    public boolean AssertAllSpansAreEndedProperly() {
        for (SpanData i : this.SpanDataItems) {
            if (!i.hasEnded()) {
                return false;
            }
        }
        return true;
    }

    public boolean AssertThereIsOnlyOneParentSpan() {
        int totalParentSpans = 0;
        for (SpanData i : this.SpanDataItems) {
            System.out.println("Printing spans"+i);
            if (this.IsRootParentSpan(i.getParentSpanId())){
                totalParentSpans++;
            }
        }
        return totalParentSpans == 1;
    }

    public boolean AssertNumberOfTracesAreEqualToNumberOfRequests(int n) {
        Set<String> uniqueTraceIds = new HashSet<>();
        for (SpanData i : this.SpanDataItems) {
            uniqueTraceIds.add(i.getTraceId());
        }
        System.out.println("Unique trace ids are: ");
        System.out.println(uniqueTraceIds.size());
        System.out.println(Integer.compare(uniqueTraceIds.size(), n) == 0);
        return Integer.compare(uniqueTraceIds.size(), n) == 0;
    }

    public boolean AssertAllSpansAreInOrder() {
        //Create Map, add all entries
        HashMap<String, SpanData> map=new HashMap<String, SpanData>();
        for (SpanData i : this.SpanDataItems) {
            map.put(i.getSpanId(), i);
        }

        for (SpanData i : this.SpanDataItems) {
            if (this.IsRootParentSpan(i.getParentSpanId())){
                continue;
            }
            long spanStartEpochNanos  = i.getStartEpochNanos();
            long spanEndEpochNanos = i.getEndEpochNanos();

            SpanData parentSpanData = map.get(i.getParentSpanId());
            long parentSpanStartEpochNanos = parentSpanData.getStartEpochNanos();
            long parentSpanEndEpochNanos = parentSpanData.getEndEpochNanos();

            if ((parentSpanStartEpochNanos >= spanStartEpochNanos) || (parentSpanEndEpochNanos <= spanEndEpochNanos)) {
                return false;
            }
        }
        return true;
    }

    private boolean IsRootParentSpan(String spanId){
        return spanId.startsWith("00000");
    }
}
