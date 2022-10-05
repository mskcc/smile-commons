# SMILE Commons

Centralized configurations for checkstyle plugin and dependency management. 

## OpenTelemetryUtils

This common library contains utilities to supoprt [Distributed Tracing](https://lightstep.com/opentelemetry/tracing) - [Context Propagation](https://lightstep.com/opentelemetry/context-propagation) via OpenTelemetry.

### Basic Usage for application

```java
// required imports
import org.mskcc.smile.commons.OpenTelemetryUtils;
import org.mskcc.smile.commons.OpenTelemetryUtils.TraceMetadata;

// inject
@Autowired
OpenTelemetryUtils openTelemetryUtils;

// OpenTelemetry Tracer 
private static final Tracer tracer = GlobalOpenTelemetry.get().getTracer("org.mskcc.cmo.Classname");

// Upstream service propagating context to downstream service via a Nats Message
Span testJetstreamPubSpan = tracer.spanBuilder("testJetStreamPubSpan").startSpan();
Scope scope = testJetstreamPubSpan.makeCurrent();
Span.current().addEvent("testJetstreamPubEvent: publishing to jetstream topic" + JETSTREAM_PUBLISH_TOPIC);
TraceMetadata tmd = openTelemetryUtils.getTraceMetadata();
messagingGateway.publishWithTrace(JETSTREAM_PUBLISH_TOPIC, "<this is test message body>", tmd);
testJetstreamPubSpan.end();

// Downstream service receiving context via onMessage subscription and continuing tracing using the same span
@Override
public void onMessage(Message msg, Object message)
{
    try {
        // In production code, the following statement should check if TraceMetadata exists before usin it.
        String traceId = msg.getHeaders().get(TraceMetadata.getHeaderKey()).get(0);
        TraceMetadata tmd = new TraceMetadata(traceId);
        Span testJetstreamSubOnMessageSpan = openTelemetryUtils.getSpan(tmd, "testJetStreamSubOnMessageSpan");

        Scope scope = testJetstreamSubOnMessageSpan.makeCurrent();
        String receivedMessageContent = new String(msg.getData(), StandardCharsets.UTF_8);
        Attributes eventAttributes = Attributes.of(AttributeKey.stringKey("receivedMessageSubject"), msg.getSubject(),
                                                   AttributeKey.stringKey("receivedMessageBody"), receivedMessageContent);
        Span.current().addEvent("testJetstreamSubOnMessageEvent: messageReceived", eventAttributes);
        testJetstreamSubOnMessageSpan.end();
    }
    catch (Exception e) {
        System.out.println("Exception: " + e.getMessage());
    }
}
```
