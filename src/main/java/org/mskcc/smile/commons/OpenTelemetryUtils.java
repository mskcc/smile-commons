package org.mskcc.smile.commons;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.Map;

public interface OpenTelemetryUtils {

    Map<String, String> getTraceMetadata(Context context);
    Span getSpanFromTraceMetadata(String spanName, Tracer tracer, Map<String, String> traceMetadata);
}

