package org.mskcc.smile.commons.impl;

import io.grpc.Metadata;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.mskcc.smile.commons.OpenTelemetryUtils;
import org.springframework.stereotype.Component;

@Component
public class OpenTelemetryUtilsImpl implements OpenTelemetryUtils {

    private static final TextMapPropagator textFormat =
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    private static final Tracer tracer =
        GlobalOpenTelemetry.get().getTracer("org.mskcc.smile.commons.impl.OpenTelemetryImpl");

    private static final TextMapGetter<Metadata> getter =
        new TextMapGetter<Metadata>() {
            @Override
            public Iterable<String> keys(Metadata carrier) {
                return carrier.keys();
            }

            @Override
            public String get(Metadata carrier, String key) {
                Metadata.Key<String> k = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                if (carrier.containsKey(k)) {
                    return carrier.get(k);
                }
                return "";
            }
        };

    TextMapSetter<Metadata> setter =
        (carrier, key, value) ->
        carrier.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);

    @Override
    public TraceMetadata getTraceMetadata() {
        Metadata headers = new Metadata();
        textFormat.inject(Context.current(), headers, setter);
        String traceId = getter.get(headers, TraceMetadata.getHeaderKey());
        return new TraceMetadata(traceId);
    }

    @Override
    public Span getSpan(TraceMetadata traceMetadata, String spanName) {
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of(TraceMetadata.getHeaderKey(),
                                    Metadata.ASCII_STRING_MARSHALLER), traceMetadata.getMetadata());
        Context extractedContext = textFormat.extract(Context.current(), headers, getter);
        return tracer.spanBuilder(spanName).setParent(extractedContext).startSpan();
    }
}
