# Sync Options

#### `errorCallBack`
a callback that is called whenever an error event occurs during the sync process. It contains information about the 
error message and the exception.

#### `warningCallBack` 
a callback that is called whenever a warning event occurs during the sync process. It contains information about the 
warning message.

#### `beforeUpdateCallback`
during the sync process if a target resource and a resource draft are matched, this callback can be used to intercept 
the **_update_** request just before it is sent to CTP. It contains information about the resource draft, the target
resource and the update actions that were calculated after comparing both.  

#### `beforeCreateCallback`
during the sync process if a resource draft should be created, this callback can be used to intercept 
the **_create_** request just before it sent to CTP.  It contains information about the resource draft that should be
created.

#### `batchSize`
a number that could be used to set the batch size with which resources are fetched and processed,
as resources are obtained from the target CTP project in batches for better performance. The algorithm accumulates up to
`batchSize` resources from the input list, then fetches the corresponding resources from the target CTP project
in a single request. Playing with this option can slightly improve or reduce processing speed.

#### `syncFilter` (Only for Product Sync Options)
 represents either a blacklist or a whitelist for filtering certain update action groups. 
  - __Blacklisting__ an update action group means that everything in products will be synced except for any group 
  in the blacklist. A typical use case is to blacklist prices when syncing products. In other words, syncing everything 
  in products except prices. 
  
  - __Whitelisting__ an update action group means that the groups in this whitelist will be the *only* group synced in 
  products. One use case could be to whitelist prices when syncing products. In other words, syncing prices only in 
  products and nothing else.
  
  - The list of action groups allowed to be blacklisted or whitelisted on products can be found [here](/src/main/java/com/commercetools/sync/products/ActionGroup.java). 

#### `ensureChannels` (Only for Product and Inventory Sync Options)
a flag which represents a strategy to handle syncing inventory entries with missing channels.
Having an inventory entry or a product, with a missing channel reference, could be processed in either of the following ways:
- If `ensureChannels` is set to `false` this inventory entry/product won't be synced and the `errorCallback` will be triggered.
- If `ensureChannels` is set to `true` the sync will attempt to create the missing channel with the given key. 
If it fails to create the supply channel, the inventory entry/product won't sync and `errorCallback` will be triggered.
- If not provided, it is set to `false` by default.


### Examples

##### 1. Using `errorCallBack` and `warningCallBack` for logging
```java

 final Logger logger = LoggerFactory.getLogger(MySync.class);
 final ProductSyncOptions productsyncOptions = ProductSyncOptionsBuilder.of(sphereClient)
                                                                        .errorCallBack(logger::error)
                                                                        .warningCallBack(logger::warn)
                                                                        .build();
 ```
##### 2. [Using `beforeUpdateCallback` for syncing a single locale](/src/main/java/com/commercetools/sync/products/templates/beforeupdatecallback/KeepOtherVariantsSync.java).

##### 3. [Using `beforeUpdateCallback` for keeping other variants](/src/main/java/com/commercetools/sync/products/templates/beforeupdatecallback/SyncSingleLocale.java).

##### 4. [Using `syncFilter` for blacklisting product categories while syncing products](/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncFilterIT.java#L142-L143).

##### 5. [Using `syncFilter` for whitelisting product names while syncing products](/src/integration-test/java/com/commercetools/sync/integration/externalsource/products/ProductSyncFilterIT.java#L173).
 
