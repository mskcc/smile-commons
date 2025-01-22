# Versioning
Our `smile-server` is currently on Java 21, and the corresponding Java Protobuf version that is current and compatible is 4.26.1.

Per the [Protobuf docs](https://protobuf.dev/support/version-support/#java), "The protoc version can be inferred from the Protobuf Java minor version number. Example: Protobuf Java version 3.**25**.x uses protoc version **25**.x.". Thus, because our Protobuf Java is 4.**26**.1, we used protoc **26**.1 to generate Smile.java.
