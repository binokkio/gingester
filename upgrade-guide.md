# Upgrading Gingester from a previous version
This document describes changes between Gingester versions that need to be taken into account when updating Gingester.

## From 0.24 to 0.25

### Stricter transformer name matching
Transformers can no longer be specified by a name that is shorter than the last N parts of their canonical name, where N is the value of their @Names annotation or 2 if that annotation is not present. E.g. the StdOut transformer can no longer be specified by "Out". Annotating a transformer with @Names(1) makes it available by its class name.

### JsonDef templating and literal CLI arg operator
The JsonDef transformer now supports templating, but to prevent the template from being parsed as a parameters object it needs to be preceded by the new literal-argument operater: @. E.g. `-t JsonDef @ '{hello:"${target}"}'`.

### Minor changes
- PathOpen transformer has been renamed to PathToInputStream
- YmlToJson transformer has been renamed to YamlToJson
- HttpResponseDummy class has been renamed to MockResponse
