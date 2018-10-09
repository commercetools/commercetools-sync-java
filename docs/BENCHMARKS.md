# Benchmarks

## Setup

1. Benchmarks are run by JUnit as a separate source set just like main, test and integration-test. The benchmarks can be 
found [here](/src/benchmark/java/com/commercetools/sync/benchmark).

2. Every benchmark writes it results as JSON to 
[benchmarks.json](https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json) which is saved in
the gh-pages branch of the repo.

3. Every time a release is made or the build has a git tag, the benchmarks are run and the results are added to 
[benchmarks.json](https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json).

4. Using Travis stages, each benchmark is run 5 times, producing 5 results in which the averages of the results is 
calculated for more accurate results.

5. The average results in [benchmarks.json](https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json)
are used to display the results in the form of this [graph](https://commercetools.github.io/commercetools-sync-java/benchmarks/). 


## Results

Results as json can be found [here](https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json).
Results as a chart can be found [here](https://commercetools.github.io/commercetools-sync-java/benchmarks/).
