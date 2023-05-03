# Important Usage Tips

#### Customized `ProjectApiRoot` Creation
When creating a customized `ProjectApiRoot` the following remarks should be considered:

- Limit the number of concurrent requests done to CTP. This can be done by adding a QueueMiddleware to the ApiRootBuilder [QueueMiddleware](https://commercetools.github.io/commercetools-sdk-java-v2/javadoc/com/commercetools/docs/meta/ClientTuning.html#limit-requests)
 
- Retry on 5xx errors with a retry strategy. This can be achieved by adding a RetryRequestMiddleware to the ApiRootBuilder [RetryRequestMiddleware](https://commercetools.github.io/commercetools-sdk-java-v2/javadoc/com/commercetools/docs/meta/ClientTuning.html#retry-middleware)
   
If you have no special requirements on the client creation, then you can use the `ClientConfigurationUtils#createClient` 
util which applies the best practices for `ProjectApiRoot` creation.
To understand how to initialize those method arguments, please refer to the [unit test](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/commons/utils/ClientV2ConfigurationUtilsTest.java#L20)

To tune / customize your client please refer to [Client Tuning](https://commercetools.github.io/commercetools-sdk-java-v2/javadoc/com/commercetools/docs/meta/ClientTuning.html)

#### Tuning the Sync Process 
The sync library is not meant to be executed in a parallel fashion. For example:
````java
final ProductSync productSync = new ProductSync(syncOptions);
final CompletableFuture<ProductSyncStatistics> syncFuture1 = productSync.sync(batch1).toCompletableFuture();
final CompletableFuture<ProductSyncStatistics> syncFuture2 = productSync.sync(batch2).toCompletableFuture();
CompletableFuture.allOf(syncFuture1, syncFuture2).join;
````
The aforementioned example demonstrates how the library should **NOT** be used. The library, however, should be instead
used in a sequential fashion:
````java
final ProductSync productSync = new ProductSync(syncOptions);
productSync.sync(batch1)
           .thenCompose(result -> productSync.sync(batch2))
           .toCompletableFuture()
           .join();
````
By design, scaling the sync process should **not** be done by executing the batches themselves in parallel. However, it can be done either by:
 
 - Changing the number of [max parallel requests](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L116) within the `sphereClient` configuration. It defines how many requests the client can execute in parallel.
 - or changing the draft [batch size](https://commercetools.github.io/commercetools-sync-java/v/9.2.1/com/commercetools/sync/commons/BaseSyncOptionsBuilder.html#batchSize-int-). It defines how many drafts can one batch contains.
 
The current overridable default [configuration](https://github.com/commercetools/commercetools-sync-java/tree/master/src/main/java/com/commercetools/sync/commons/utils/ClientConfigurationUtils.java#L45) of the `sphereClient` 
is the recommended good balance for stability and performance for the sync process.

In order to exploit the number of `max parallel requests`, the `batch size` should have a value set that is equal or higher.
