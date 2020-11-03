# Shopping List Sync

Module used for importing/syncing Shopping Lists into a commercetools project. 
It also provides utilities for generating update actions based on the comparison of a [ShoppingList](https://docs.commercetools.com/api/projects/shoppingLists#shoppinglist) 
against a [ShoppingListDraft](https://docs.commercetools.com/api/projects/shoppingLists#shoppinglistdraft).

## Usage

### Sync list of Shopping List Drafts

#### Prerequisites
1. The sync expects a list of `ShoppingList`s that have their `key` fields set to be matched with shopping lists in the 
target CTP project. The customers in the target project need to have the `key` fields set, otherwise they won't be 
matched.

2. Every shopping list may have a reference to their [Type](https://docs.commercetools.com/api/projects/shoppingLists#set-custom-type) of their custom fields. 
The `Type` references should be expanded with a key.
Any reference that is not expanded will have its id in place and not replaced by the key will be considered as existing 
resources on the target commercetools project and the library will issue an update/create an API request without reference
resolution.

     - When syncing from a source commercetools project, you can use [`mapToShoppingListDraft`](https://commercetools.github.io/commercetools-sync-java/v/2.4.0/com/commercetools/sync/shoppinglists/utils/ShoppingListReferenceResolutionUtils.html#mapToShoppingListDrafts-java.util.List-)
    method that maps from a `ShoppingList` to `ShoppingListDraft` to make them ready for reference resolution by the sync:

    ````java
    final List<ShoppingListDraft> shoppingListDrafts = ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts(shoppingLists);
    ````

4. Create a `sphereClient` [as described here](IMPORTANT_USAGE_TIPS.md#sphereclient-creation).

5. After the `sphereClient` is set up, a `CustomerSyncOptions` should be built as follows:
````java
// instantiating a CustomerSyncOptions
final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder.of(sphereClient).build();
````

[More information about Sync Options](SYNC_OPTIONS.md).