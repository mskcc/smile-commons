# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SMILE Commons is a shared Java 21 / Spring Boot 3.3.3 library (`org.mskcc.cmo:smile-commons`,
packaged as a jar and consumed via JitPack) used by the other SMILE services (`smile-server`,
`smile-label-generator`, etc.). It provides:

- **`JsonComparator`** — the metadata consistency checker used to decide whether incoming
  request/sample/cohort JSON contains real updates versus what is already persisted.
- **`OpenTelemetryUtils`** — distributed-tracing helpers for propagating trace context across
  NATS messages (usage example in `README.md`).
- **Enums** (`commons/enums/`) — controlled vocabularies (`SampleType`, `SpecimenType`,
  `CmoSampleClass`, `NucleicAcid`, …) shared across services.
- **Generated Protobuf classes** (`commons/generated/Smile.java`) — e.g. `TempoSampleUpdateMessage`,
  `TempoCohortUpdate`.
- **Go protobuf types** under `types/` (`go.mod` module `github.com/mskcc/smile-commons`) for
  IGO (`types/igo/v1`) and CVR (`types/mpath/v1`) messages.
- **Centralized Checkstyle config** (`src/main/resources/checkstyle.xml`) mirrored by consumer repos.

All Java code lives under `org.mskcc.smile.commons`; implementations are in `impl/` and annotated
`@Component` so consumers pick them up by component scanning.

## Build, Test, and Lint

```bash
mvn clean install                 # build + tests (Checkstyle `check` runs in the validate phase)
mvn clean install -DskipTests
mvn test -Dtest=JsonComparatorTest
mvn test -Dtest=JsonComparatorTest#testCompareSampleStatus
mvn checkstyle:checkstyle         # writes checkstyle_report.txt
bash test/scripts/generate_checkstyle_report.sh "$(pwd)"   # CI gate: fails if report is non-empty
```

CI (CircleCI) runs `mvn clean install`, then Checkstyle, then the report script — any Checkstyle
finding fails the build. Style is Google Java Style with a 110-char line limit; test sources are
included in Checkstyle.

## Protobuf Code Generation

Java classes in `src/main/java/org/mskcc/smile/commons/generated/` are generated **manually** with
`protoc` and committed. The `protoc` major version must match the Protobuf Java *minor* version in
`pom.xml` (currently `protobuf-java` 4.29.3 → `protoc` 29.x):

```bash
protoc path/to/file.proto --java_out=src/main/java
```

Update the `protobuf-java` version in `pom.xml` if it changes. Consumers depend on these classes
for NATS payloads, so changes are cross-repo breaking changes.

## JsonComparator Architecture

`JsonComparatorImpl` is the most behavior-sensitive code in this repo. Public entry points and
their callers:

| Method | comparisonType | Used by |
|---|---|---|
| `isConsistent(ref, tar)` | `"new"` (all non-ignored fields) | smile-server `RequestServiceImpl`/`SampleServiceImpl` when update is *not* from LIMS; smile-label-generator `CmoLabelGeneratorServiceImpl` |
| `isConsistentByIgoProperties(ref, tar)` | `"igo"` (only `IGO_ACCEPTED_FIELDS`) | smile-server when the update comes from LIMS (`fromLims = true`) |
| `isConsistentGenericComparison(ref, tar)` | n/a (drops `GENERIC_IGNORED_FIELDS` + null/empty) | smile-server `CohortServiceImpl` (cohort complete) |
| `isConsistent(ref, tar, ignoredFields, type)` | caller-supplied | no external callers |

Comparison pipeline for `isConsistent`:
1. **Standardize** top-level property names with `STD_IGO_REQUEST_JSON_PROPS_MAP` (IGO/LIMS names
   → SMILE names, e.g. `requestId`→`igoRequestId`, `deliveryDate`→`igoDeliveryDate` coerced to
   long). Sample nodes inside `samples[]` use `STD_IGO_SAMPLE_JSON_PROPS_MAP` instead
   (`igoId`→`primaryId`, `cmoSampleClass`→`sampleType`, …). Note: a `SampleMetadata` compared
   directly (no `samples` wrapper) goes through the *request* map.
2. **Filter** (`filterJsonNode`): drop `ignoredFields`, drop null/blank/`[]` values, and in `"igo"`
   mode drop anything not in `IGO_ACCEPTED_FIELDS`. Values are tested with `asText()`, which is
   `""` for any object/array, so nested object/array fields are dropped from this pass.
3. **Compare** with JSONAssert `STRICT` (`isMatchingJsons`).
4. **Side pass** for `qcReports`, `libraries`, `status` (`isConsistentSampleMetadata` /
   `isMatchingJsonByFieldName`): these fields are re-wrapped as strings so they survive filtering,
   then children are filtered and compared; `libraries[].runs` is compared recursively by index.
5. If either JSON has `samples`, samples are standardized, filtered, sorted by primary id and
   compared, then the side pass runs per sample matched by `primaryId`/`igoId`.

Fixtures: `src/test/resources/data/mocked_request_data_details.txt` is a tab-delimited index
(`identifier`, `filepath`, `description`) loaded by `MockDataConfig` into the
`mockedJsonDataMap` bean; add new JSON fixtures under `data/` and register them there. Since
consumers depend on exact true/false semantics, any change to field lists or filtering should be
covered by a fixture-based test in `JsonComparatorTest`.
