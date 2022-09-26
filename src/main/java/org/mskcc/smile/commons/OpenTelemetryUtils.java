package org.mskcc.smile.commons;

import io.opentelemetry.api.trace.Span;

public interface OpenTelemetryUtils {

    public class TraceMetadata {
        private static final String TRACE_ID_HEADER_KEY = "traceparent";

        private String traceMetadata;

        public TraceMetadata(String md) {
            traceMetadata = md;
        }

        public static String getHeaderKey() {
            return TRACE_ID_HEADER_KEY;
        }
        
        public String getMetadata() {
            return traceMetadata;
        }
    }

    TraceMetadata getTraceMetadata();
    Span getSpan(TraceMetadata traceMetadata, String spanName);
}

