# Gingester

Generic ingest framework.


## To do

Not complete nor in any particular order.

- Gingester unpack package
- Gingester report and optimize methods
- Checkpoints, allow a transformer to request a pause of all generator transformers and wait for all workers to starve
    - This allows the transformer to know the all data it generated up to that point has flowed downstream
    - Can be implemented simply by blocking first link downstream of every generator transformer
- Multi-host processing
    - Have multiple hosts run the exact same configuration
    - A generator like Path.Search can be configured to run on only 1 host but have its downstream link round-robin the Paths across all configured hosts