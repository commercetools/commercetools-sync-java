# Benchmarks

## Setup

1. Benchmarks are run by JUnit as a separate source set just like main, test and integration-test. The benchmarks can be 
found [here](/src/benchmark/java/com/commercetools/sync/benchmark).

2. Every time a commit is made in any branch, the benchmarks are run and the run shows whether the benchmarks exceed the
pre-defined threshold, so that we know if the corresponding commit influences the performance.

3. Benchmark results are written as JSON to 
[benchmarks.json](https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json) only when a 
new release is made. The JSON file is saved in the [gh-pages branch](https://github.com/commercetools/commercetools-sync-java/tree/gh-pages) 
of the repo.

4. The average results in [benchmarks.json](https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json)
are used to display the results in the form of this [graph](https://commercetools.github.io/commercetools-sync-java/benchmarks/). 


## Results

Results as json can be found [here](https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json).
Results as a chart can be found [here](https://commercetools.github.io/commercetools-sync-java/benchmarks/).
