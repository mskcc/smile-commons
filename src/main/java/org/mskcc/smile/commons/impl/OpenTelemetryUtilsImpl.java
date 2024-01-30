package org.mskcc.smile.commons.impl;

import org.mskcc.smile.commons.OpenTelemetryUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

@Component
public class OpenTelemetryUtilsImpl implements OpenTelemetryUtils {

    private static final TextMapGetter<Map<String, String>> getter =
        new TextMapGetter<Map<String, String>>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }

            @Nullable
            @Override
            public String get(Map<String, String> carrier, String key) {
                return carrier.get(key);
            }
        };

    @Override
    public Map<String, String> getTraceMetadata(Context context) {
        B3Propagator b3Propagator = B3Propagator.injectingMultiHeaders();
        TextMapSetter<Map<String, String>> setter = Map::put;
        Map<String, String> carrier = new HashMap<>();
        b3Propagator.inject(context, carrier, setter);
        return carrier;
    }

    @Override
    public Span getSpanFromTraceMetadata(String spanName, Tracer tracer, Map<String, String> traceMetadata) {
        B3Propagator b3Propagator = B3Propagator.injectingMultiHeaders();
        Context propContext = b3Propagator.extract(Context.current(), traceMetadata, getter);
        return tracer.spanBuilder(spanName).setParent(propContext).startSpan();
    }
}
