/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.threadpool.ThreadPool;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

/**
 *
 * The default tracer implementation. This class implements the basic logic for span lifecycle and its state management.
 * It also handles tracing context propagation between spans.
 *
 * It internally uses OpenTelemetry tracer.
 *
 */
public class DefaultTracer implements Tracer {

    public static final String CURRENT_SPAN = "current_span";

    private static final Logger logger = LogManager.getLogger(DefaultTracer.class);
    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";
    private static final String SPAN_NAME = "span_name";
    private static final String PARENT_SPAN_ID = "p_span_id";
    private static final String THREAD_NAME = "th_name";
    private static final String PARENT_SPAN_NAME = "p_span_name";
    private static final String ROOT_SPAN = "RootSpan";

    private final ThreadPool threadPool;
    private final TracerSettings tracerSettings;
    private final io.opentelemetry.api.trace.Tracer otelTracer;
    private final OpenTelemetry openTelemetry;

    public DefaultTracer(OpenTelemetry openTelemetry, ThreadPool threadPool, TracerSettings tracerSettings) {
        this.openTelemetry = openTelemetry;
        this.otelTracer = openTelemetry.getTracer("os-tracer");
        this.threadPool = threadPool;
        this.tracerSettings = tracerSettings;
    }

    @Override
    public void startSpan(String spanName, Level level) {
        Span span = createSpan(spanName, getCurrentSpan(), level);
        setCurrentSpanInContext(span);
        setSpanAttributes(span);
    }

    @Override
    public void endSpan() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.endSpan();
            setCurrentSpanInContext(currentSpan.getParentSpan());
        }
    }

    @Override
    public void addAttribute(String key, String value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addAttribute(key, value);
        }
    }

    @Override
    public void addAttribute(String key, long value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addAttribute(key, value);
        }
    }

    @Override
    public void addAttribute(String key, double value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addAttribute(key, value);
        }
    }

    @Override
    public void addAttribute(String key, boolean value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addAttribute(key, value);
        }
    }

    @Override
    public void addEvent(String event) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addEvent(event);
        }
    }

    @Override
    public void close() {
        if (openTelemetry instanceof Closeable) {
            try {
                ((Closeable) openTelemetry).close();
            } catch (IOException e) {
                logger.warn("Error while closing tracer", e);
            }
        }
    }

    @Override
    public Span getCurrentSpan() {
        Optional<Span> optionalSpanFromContext = spanFromThreadContext();
        return optionalSpanFromContext.orElse(spanFromHeader());
    }

    private Span spanFromHeader() {
        Context context = TracerUtils.extractTracerContextFromHeader(threadPool.getThreadContext().getHeaders());
        if (context != null) {
            io.opentelemetry.api.trace.Span span = io.opentelemetry.api.trace.Span.fromContext(context);
            return new OSSpan(ROOT_SPAN, span, null, Level.ROOT);
        }
        return null;
    }

    private Optional<Span> spanFromThreadContext() {
        ThreadContext threadContext = threadPool.getThreadContext();
        SpanHolder spanHolder = threadContext.getTransient(CURRENT_SPAN);

        return (spanHolder == null) ? Optional.empty() : Optional.ofNullable(spanHolder.getSpan());
    }

    private Span createSpan(String spanName, Span parentSpan, Level level) {
        return isLevelEnabled(level) ? createOSSpan(spanName, parentSpan, level) : createNoopSpan(spanName, parentSpan, level);
    }

    private Span createOSSpan(String spanName, Span parentSpan, Level level) {
        OSSpan parentOSSpan = getLastValidSpanInChain(parentSpan);
        io.opentelemetry.api.trace.Span otelSpan = createOtelSpan(spanName, parentOSSpan);
        Span span = new OSSpan(spanName, otelSpan, parentSpan, level);
        logger.debug(
            "Starting OtelSpan spanId:{} name:{}: traceId:{}",
            otelSpan.getSpanContext().getSpanId(),
            span.getSpanName(),
            otelSpan.getSpanContext().getTraceId()
        );
        return span;
    }

    private NoopSpan createNoopSpan(String spanName, Span parentSpan, Level level) {
        logger.debug("Starting Noop span name:{}", spanName);
        return new NoopSpan(spanName, parentSpan, level);
    }

    private OSSpan getLastValidSpanInChain(Span parentSpan) {
        while (parentSpan instanceof NoopSpan) {
            parentSpan = parentSpan.getParentSpan();
        }
        return (OSSpan) parentSpan;
    }

    // visible for testing
    io.opentelemetry.api.trace.Span createOtelSpan(String spanName, OSSpan parentOSSpan) {
        return parentOSSpan == null
            ? otelTracer.spanBuilder(spanName).startSpan()
            : otelTracer.spanBuilder(spanName).setParent(Context.current().with(parentOSSpan.getOtelSpan())).startSpan();
    }

    private boolean isLevelEnabled(Level level) {
        Level configuredLevel = tracerSettings.getTracerLevel();
        return level.isHigher(configuredLevel);
    }

    private void setCurrentSpanInContext(Span span) {
        if (span == null) {
            return;
        }
        ThreadContext threadContext = threadPool.getThreadContext();
        SpanHolder spanHolder = threadContext.getTransient(CURRENT_SPAN);
        if (spanHolder == null) {
            threadContext.putTransient(CURRENT_SPAN, new SpanHolder(span));
        } else {
            spanHolder.setSpan(span);
        }
    }

    private void setSpanAttributes(Span span) {
        if (span instanceof OSSpan) {
            addDefaultAttributes((OSSpan) span);
        }
    }

    private void addDefaultAttributes(OSSpan osSpan) {
        if (osSpan != null && osSpan.getSpanContext() != null) {
            osSpan.addAttribute(SPAN_ID, osSpan.getSpanContext().getSpanId());
            osSpan.addAttribute(TRACE_ID, osSpan.getSpanContext().getTraceId());
            osSpan.addAttribute(SPAN_NAME, osSpan.getSpanName());
            osSpan.addAttribute(THREAD_NAME, Thread.currentThread().getName());
            if (osSpan.getParentSpan() != null && osSpan.getParentSpan() instanceof OSSpan) {
                osSpan.addAttribute(PARENT_SPAN_ID, ((OSSpan) osSpan.getParentSpan()).getSpanContext().getSpanId());
                osSpan.addAttribute(PARENT_SPAN_NAME, osSpan.getParentSpan().getSpanName());
            }
        }
    }

}
