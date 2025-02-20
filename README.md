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
## Updating Java Code Generated from Protobuf Files

Java classes inside `./src/main/java/org/mskcc/smile/commons/generated/` are manually generated.

To update existing or create new classes, follow these steps:
- Obtain or construct the `.proto` file
- Ensure that the correct `protoc` version is installed (see more in notes below)
- Run `$ protoc [path to .proto file] --java_out=[directory to write the Java generated code to]`
- Ensure the newly generated Java classes are saved at `./src/main/java/org/mskcc/smile/commons/generated/`
- Update `pom.xml` if there are changes to the Protobuf Java version

### Version of `protoc` to Use
Per the [Protobuf docs](https://protobuf.dev/support/version-support/#java):
> The protoc version can be inferred from the Protobuf Java minor version number. Example: Protobuf Java version 3.**25**.x uses protoc version **25**.x.".

Based on this, follow these steps to determine the correct `protoc` version to use:
- Note the Java version of the Java application needing the Protobuf Java classes (e.g. Java 21)
- Find the Protobuf Java version that is compatible with the Java application (e.g. Protobuf Java 4.29.3 supports Java 21)
- Determine the protoc version needed from the Protobuf Java minor version number (e.g. Protobuf Java 4.**29**.3 uses protoc **29**.x)
