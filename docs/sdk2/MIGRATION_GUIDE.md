<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Migration Guide](#migration-guide)
  - [Migrate syncers of supported resources](#migrate-syncers-of-supported-resources)
    - [Categories](/docs/sdk2/usage/CATEGORY_SYNC.md#migration-guide), 
    - [Products](/docs/sdk2/usage/PRODUCT_SYNC.md#migration-guide)
    - [InventoryEntries](/docs/sdk2/usage/INVENTORY_SYNC.md#migration-guide)
    - [ProductTypes](/docs/sdk2/usage/PRODUCT_TYPE_SYNC.md#migration-guide)
    - [Types](/docs/sdk2/usage/TYPE_SYNC.md#migration-guide)
    - [CartDiscounts](/docs/sdk2/usage/CART_DISCOUNT_SYNC.md#migration-guide)
    - [States](/docs/sdk2/usage/STATE_SYNC.md#migration-guide)
    - [TaxCategories](/docs/sdk2/usage/TAX_CATEGORY_SYNC.md#migration-guide)
    - [CustomObjects](/docs/sdk2/usage/CUSTOM_OBJECT_SYNC.md#migration-guide)
    - [Customers](/docs/sdk2/usage/CUSTOMER_SYNC.md#migration-guide)
    - [ShoppingLists](/docs/sdk2/usage/SHOPPING_LIST_SYNC.md#migration-guide)
<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Migration Guide

The commercetools sync library uses the [JVM-SDK-V2](http://commercetools.github.io/commercetools-sdk-java-v2), therefore ensure you [Install JVM SDK](https://docs.commercetools.com/sdk/java-sdk-getting-started#install-the-java-sdk) module `commercetools-sdk-java-api` with
any HTTP client module. The default one is `commercetools-http-client`.

## Common Changes

Some utility methods aren't available in this version. Please make sure to replace these. Here's a list of changes:

- Removed utility methods:
```java 
// CollectionUtils
public static <T> Set<T> emptyIfNull(@Nullable final Set<T> set)
```
- Changed scope of utility method:
```java
// CompletableFutureUtils
private static <T, S, U extends Collection<CompletableFuture<S>>> U mapValuesToFutures(
      @Nonnull final Stream<T> values,
      @Nonnull final Function<T, CompletionStage<S>> mapper,
      @Nonnull final Collector<CompletableFuture<S>, ?, U> collector)
```
- Removed helper class:
[ChannelCustomActionBuilder](https://github.com/commercetools/commercetools-sync-java/blob/v1.0.0-M14/src/main/java/com/commercetools/sync/channels/helpers/ChannelCustomActionBuilder.java)
```java
// Included these methods
  public UpdateAction<Channel> buildRemoveCustomTypeAction(
      @Nullable final Integer variantId, @Nullable final String objectId);

  public UpdateAction<Channel> buildSetCustomTypeAction(
      @Nullable final Integer variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, JsonNode> customFieldsJsonMap);

  public UpdateAction<Channel> buildSetCustomFieldAction(
      @Nullable final Integer variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final JsonNode customFieldValue);
```
## Migrate syncers of supported resources

- [Categories](/docs/sdk2/usage/CATEGORY_SYNC.md#migration-guide), 
- [Products](/docs/sdk2/usage/PRODUCT_SYNC.md#migration-guide)
- [InventoryEntries](/docs/sdk2/usage/INVENTORY_SYNC.md#migration-guide)
- [ProductTypes](/docs/sdk2/usage/PRODUCT_TYPE_SYNC.md#migration-guide)
- [Types](/docs/sdk2/usage/TYPE_SYNC.md#migration-guide)
- [CartDiscounts](/docs/sdk2/usage/CART_DISCOUNT_SYNC.md#migration-guide)
- [States](/docs/sdk2/usage/STATE_SYNC.md#migration-guide)
- [TaxCategories](/docs/sdk2/usage/TAX_CATEGORY_SYNC.md#migration-guide)
- [CustomObjects](/docs/sdk2/usage/CUSTOM_OBJECT_SYNC.md#migration-guide)
- [Customers](/docs/sdk2/usage/CUSTOMER_SYNC.md#migration-guide)
- [ShoppingLists](/docs/sdk2/usage/SHOPPING_LIST_SYNC.md#migration-guide)


 
