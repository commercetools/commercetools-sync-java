# Cleanup of old unresolved reference custom objects

For some references such as state transitions and attribute references with product references, the library keeps tracks of unresolved drafts and persists them in a commercetools custom object and create/update them accordingly whenever the referenced drafts exist in the target project.

It also means that created data might be never resolved, in this case keeping the old custom objects around forever can negatively influence the performance of your project and the time it takes to restore it from a backup.  So deleting unused data ensures the best performance for your project. 

For the cleanup, commercetools-sync-java library exposes a ready to use method so that all custom objects created by the library can be cleaned up on demand.  

The cleanup can be defined with the parameter `deleteDaysAfterLastModification` (in days) object creation. For example, if you configure the `deleteDaysAfterLastModification` parameter to 30 days, cleanup method will delete the custom objects which haven't been modified in the last 30 days.
Additionally, an error callback could be configured to consume the error, that is called `errorCallback` whenever an error event occurs during the sync process.

````java
final CleanupUnresolvedReferenceCustomObjects.Statistics statistics =
            CleanupUnresolvedReferenceCustomObjects.of(sphereClient)
                   .errorCallback(throwable -> logger.error("Unexcepted error.", throwable))
                   .cleanup(30)
                   .join();
````

The result of the previous code snippet is a `Statistics` object that contains all the statistics of the cleanup process. That includes a report message, the total number of deleted and failed custom objects.

````java
statistics.getReportMessage();  // Summary: 100 custom objects were deleted in total (1 failed to delete).
statistics.getTotalDeleted(); // Returns 100
statistics.getTotalFailed(); // Returns 1
````
