# Upgrading Gingester from a previous version
This document describes changes between Gingester versions that need to be taken into account when updating Gingester.

## From 0.25 to 0.26

### Finish signal handling
Finish signals are now processed and propagated by an upstream worker for transformers that do not have dedicated workers. Previously every transformer had at least one worker processing and propagating finish signals even if the transformer was not explicitly assigned any workers. This change might slowdown some flows that previously benefited from the parallel processing provided by the finish handling workers. To mitigate such slowdowns put the workers back in the flow by explicitly assigning a worker downstream of each of the sync-to transformers in the flow. Another effect of this change is that some things that previously had an undetermined order now have a possibly different determined order. This can show up in broken unit tests that expect the previous undetermined order that just happened to be consistent over multiple runs.

## From 0.24 to 0.25

### Stricter transformer name matching
Transformers can no longer be specified by a name that is shorter than the last N parts of their canonical name, where N is the value of their @Names annotation or 2 if that annotation is not present. E.g. the StdOut transformer can no longer be specified by "Out". Annotating a transformer with @Names(1) makes it available by its class name.

### JsonDef templating and literal CLI arg operator
The JsonDef transformer now supports templating, but to prevent the template from being parsed as a parameters object it needs to be preceded by the new literal-argument operater: @. E.g. `-t JsonDef @ '{hello:"${target}"}'`.

### Minor changes
- PathOpen transformer has been renamed to PathToInputStream
- YmlToJson transformer has been renamed to YamlToJson
- HttpResponseDummy class has been renamed to MockResponse
