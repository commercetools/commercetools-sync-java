# SYNC\_OPTIONS

Additional optional configuration for the sync can be configured on the `ProductTypeSyncOptionsBuilder` instance, according to your need:

## `errorCallBack`

a callback that is called whenever an error event occurs during the sync process.

## `warningCallBack`

a callback that is called whenever a warning event occurs during the sync process.

## `beforeUpdateCallback`

a filter function which can be applied on a generated list of update actions. It allows the user to intercept product type _**update**_ actions just before they are sent to CTP API.

## `beforeCreateCallback`

a filter function which can be applied on a product type draft before a request to create it on CTP is issued. It allows the user to intercept product type _**create**_ requests to modify the draft before the create request is sent to CTP API.

## `batchSize`

a number that could be used to set the batch size with which product types are fetched and processed with, as product types are obtained from the target CTP project in batches for better performance. The algorithm accumulates up to `batchSize` product types from the input list, then fetches the corresponding product types from the target CTP project in a single request. Playing with this option can slightly improve or reduce processing speed. \(The default value is `50`\).

Example of options usage, that sets the error and warning callbacks to output the message to the log error and warning streams, would look as follows:

```java
final Logger logger = LoggerFactory.getLogger(MySync.class);
final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(sphereClient)
                                                                                   .errorCallBack(logger::error)
                                                                                   .warningCallBack(logger::warn)
                                                                                   .build();
```

