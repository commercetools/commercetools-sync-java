# Cleanup of old unresolved reference custom objects

Keeping the old custom objects around forever can negatively influence the performance of your project and the time it takes to restore it from a backup.  
Deleting unused data ensures the best performance for your project. 

The cleanup of unresolved references custom objects queries the custom objects with `container="commercetools-sync-java.UnresolvedReferencesService.productDrafts" and container="commercetools-sync-java.UnresolvedTransitionsService.stateDrafts""` and their `lastModifiedAt` values. For example, you could configure the `deleteDaysAfterLastModification` parameter to 30 days to delete the custom objects if they haven't been modified in the last 30 days.

````java
final Cleanup.Statistics statistics =
            Cleanup.of(sphereClient)
                   .errorCallback(throwable -> logger.error("Unexcepted error.", throwable))
                   .deleteUnresolvedReferenceCustomObjects(30)
                   .join();
````

The result of the previous code snippet is a `Statistics` object that contains all the statistics of the cleanup process; which includes a report message, the total number of deleted, failed, and the processing time of the cleanup in milliseconds.

````java
statistics.getReportMessage(); 
/*Summary: 100 custom objects were deleted in total (1 failed to delete).*/
````

Additionally, an error callback with option `errorCallback` could be used,
 that is called whenever an error event occurs during the sync process.
