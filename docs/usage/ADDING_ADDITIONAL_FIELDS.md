# Adding Additional Fields to the Sync Process

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Overview](#overview)
- [Step-by-Step Guide](#step-by-step-guide)
  - [Step 1: Add the update action builder](#step-1-add-the-update-action-builder)
  - [Step 2: Register the action in SyncUtils](#step-2-register-the-action-in-syncutils)
  - [Step 3: Write unit tests](#step-3-write-unit-tests)
  - [Step 4: Update the supported fields documentation](#step-4-update-the-supported-fields-documentation)
- [Variations by Field Type](#variations-by-field-type)
  - [Simple fields](#simple-fields)
  - [Reference fields](#reference-fields)
    - [Update action builder for references](#update-action-builder-for-references)
    - [Reference resolution](#reference-resolution)
    - [Reference resolution utils (mapping)](#reference-resolution-utils-mapping)
    - [Tests for reference resolution](#tests-for-reference-resolution)
  - [Products with SyncFilter](#products-with-syncfilter)
- [Quick Reference: File Locations](#quick-reference-file-locations)
- [Behavioral Extensions via Hooks](#behavioral-extensions-via-hooks)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview

The commercetools-sync-java library supports specific fields for each resource type. For a full list of supported fields, see the [Supported Resources and Fields](../SUPPORTED_RESOURCES.md) document.

If the field you need is not listed, you can add native support by following the steps in this guide. All changes are made within the existing library files — no external dependencies are needed.

## Step-by-Step Guide

This guide walks through a concrete example: adding support for a hypothetical unsupported field on **CartDiscounts**. CartDiscounts is used here because it has a clean, straightforward structure. The same pattern applies to all resource types.

### Step 1: Add the update action builder

Open the `{Resource}UpdateActionUtils.java` file for the resource you want to extend. For CartDiscounts, this is:

[`src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtils.java)

Add a new static method that compares the field's old and new values and returns an `Optional<{Resource}UpdateAction>`. Use the `buildUpdateAction()` helper from [`CommonTypeUpdateActionUtils`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/commons/utils/CommonTypeUpdateActionUtils.java) for the comparison.

**Example — adding a `store` field to CartDiscounts:**

````java
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;

/**
 * Compares the store values of a {@link CartDiscount} and a {@link CartDiscountDraft} and returns
 * an {@link CartDiscountUpdateAction} as a result in an {@link java.util.Optional}. If both the
 * {@link CartDiscount} and the {@link CartDiscountDraft} have the same store, then no update action
 * is needed and hence an empty {@link java.util.Optional} is returned.
 *
 * @param oldCartDiscount the cart discount which should be updated.
 * @param newCartDiscount the cart discount draft where we get the new store.
 * @return A filled optional with the update action or an empty optional if the stores are identical.
 */
@Nonnull
public static Optional<CartDiscountUpdateAction> buildSetStoresUpdateAction(
    @Nonnull final CartDiscount oldCartDiscount,
    @Nonnull final CartDiscountDraft newCartDiscount) {
  return buildUpdateAction(
      oldCartDiscount.getStores(),
      newCartDiscount.getStores(),
      () -> CartDiscountSetStoresActionBuilder.of()
              .stores(newCartDiscount.getStores())
              .build());
}
````

**Key points:**
- The method name follows the pattern `build{ActionName}UpdateAction`.
- It returns `Optional<{Resource}UpdateAction>` — empty if the values are equal, filled if they differ.
- The `buildUpdateAction()` helper compares using `Objects.equals()` and only invokes the supplier when values differ.
- The update action is built using the SDK's builder: `{ActionType}ActionBuilder.of().{field}(value).build()`.

### Step 2: Register the action in SyncUtils

Open the `{Resource}SyncUtils.java` file. For CartDiscounts:

[`src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountSyncUtils.java)

Add a call to your new builder method inside the `buildActions()` method, within the `filterEmptyOptionals()` call:

````java
final List<CartDiscountUpdateAction> updateActions =
    filterEmptyOptionals(
        CartDiscountUpdateActionUtils.buildChangeValueUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildChangeCartPredicateUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildChangeTargetUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildChangeIsActiveUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildChangeNameUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildSetDescriptionUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildChangeSortOrderUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildChangeRequiresDiscountCodeUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildSetValidDatesUpdateAction(oldCartDiscount, newCartDiscount),
        CartDiscountUpdateActionUtils.buildChangeStackingModeUpdateAction(oldCartDiscount, newCartDiscount),
        // New field:
        CartDiscountUpdateActionUtils.buildSetStoresUpdateAction(oldCartDiscount, newCartDiscount));
````

That single line is all that's needed to wire the new field into the sync process.

### Step 3: Write unit tests

Open the `{Resource}UpdateActionUtilsTest.java` file. For CartDiscounts:

[`src/test/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtilsTest.java)

Write tests covering these cases:

**Test 1 — Different values should generate an update action:**

````java
@Test
void buildSetStoresUpdateAction_WithDifferentStores_ShouldBuildUpdateAction() {
  final CartDiscount oldCartDiscount = mock(CartDiscount.class);
  final List<StoreKeyReference> oldStores = List.of(
      StoreKeyReferenceBuilder.of().key("store-1").build());
  when(oldCartDiscount.getStores()).thenReturn(oldStores);

  final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
  final List<StoreResourceIdentifier> newStores = List.of(
      StoreResourceIdentifierBuilder.of().key("store-2").build());
  when(newCartDiscountDraft.getStores()).thenReturn(newStores);

  final Optional<CartDiscountUpdateAction> result =
      CartDiscountUpdateActionUtils.buildSetStoresUpdateAction(
          oldCartDiscount, newCartDiscountDraft);

  assertThat(result).isPresent();
}
````

**Test 2 — Same values should return empty Optional:**

````java
@Test
void buildSetStoresUpdateAction_WithSameStores_ShouldNotBuildUpdateAction() {
  final CartDiscount oldCartDiscount = mock(CartDiscount.class);
  final List<StoreKeyReference> stores = List.of(
      StoreKeyReferenceBuilder.of().key("store-1").build());
  when(oldCartDiscount.getStores()).thenReturn(stores);

  final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
  when(newCartDiscountDraft.getStores()).thenReturn(stores);

  final Optional<CartDiscountUpdateAction> result =
      CartDiscountUpdateActionUtils.buildSetStoresUpdateAction(
          oldCartDiscount, newCartDiscountDraft);

  assertThat(result).isNotPresent();
}
````

**Test 3 — Null handling:**

````java
@Test
void buildSetStoresUpdateAction_WithBothNull_ShouldNotBuildUpdateAction() {
  final CartDiscount oldCartDiscount = mock(CartDiscount.class);
  when(oldCartDiscount.getStores()).thenReturn(null);

  final CartDiscountDraft newCartDiscountDraft = mock(CartDiscountDraft.class);
  when(newCartDiscountDraft.getStores()).thenReturn(null);

  final Optional<CartDiscountUpdateAction> result =
      CartDiscountUpdateActionUtils.buildSetStoresUpdateAction(
          oldCartDiscount, newCartDiscountDraft);

  assertThat(result).isNotPresent();
}
````

Run the tests with:
```bash
./gradlew test --tests "*CartDiscountUpdateActionUtilsTest"
```

### Step 4: Update the supported fields documentation

Add the new field to the resource's table in [`docs/SUPPORTED_RESOURCES.md`](../SUPPORTED_RESOURCES.md).

Finally, format the code:
```bash
./gradlew spotlessApply
```

And run the full check to make sure everything passes:
```bash
./gradlew check
```

## Variations by Field Type

### Simple fields

String, Boolean, LocalizedString, enum, and date fields all use the same `buildUpdateAction()` helper shown above. This covers the majority of cases.

For fields with default values (e.g., `isActive` defaults to `true`), handle `null` by substituting the default before comparing:

````java
final Boolean isActive = ofNullable(newCartDiscount.getIsActive()).orElse(true);

return buildUpdateAction(
    oldCartDiscount.getIsActive(),
    isActive,
    () -> CartDiscountChangeIsActiveActionBuilder.of().isActive(isActive).build());
````

### Reference fields

Adding a reference field (e.g., a `store`, `customer`, or `parent` category) requires **additional steps** beyond what simple fields need. The old resource holds a `KeyReference` (with a `key`), while the new draft holds a `ResourceIdentifier` (with a `key` or `id`). You must handle this asymmetry in the update action builder and also wire up reference resolution.

The following example is based on [PR #1238](https://github.com/commercetools/commercetools-sync-java/pull/1238), which added the `store` field to ShoppingLists.

#### Update action builder for references

In `{Resource}UpdateActionUtils.java`, extract comparable values (typically keys) from both the old reference and the new resource identifier, then use `buildUpdateAction()` on those extracted values:

````java
@Nonnull
public static Optional<ShoppingListUpdateAction> buildSetStoreUpdateAction(
    @Nonnull final ShoppingList oldShoppingList,
    @Nonnull final ShoppingListDraft newShoppingList) {

  final String oldStoreKey =
      oldShoppingList.getStore() != null ? oldShoppingList.getStore().getKey() : null;
  final String newStoreKey =
      newShoppingList.getStore() != null && newShoppingList.getStore().getKey() != null
          ? newShoppingList.getStore().getKey()
          : (newShoppingList.getStore() != null ? newShoppingList.getStore().getId() : null);

  return buildUpdateAction(
      oldStoreKey,
      newStoreKey,
      () -> ShoppingListSetStoreActionBuilder.of().store(newShoppingList.getStore()).build());
}
````

Then register it in `{Resource}SyncUtils.java` and write unit tests (Steps 2 and 3 above), covering: different keys, same keys, null old, null new, and both null.

#### Reference resolution

When syncing from an external source, references are provided by **key**. When syncing from a commercetools project, they may be provided by **ID**. The `{Resource}ReferenceResolver` handles converting keys to IDs so the API can process them.

In `{Resource}ReferenceResolver.java`, add a resolve method and chain it in `resolveReferences()`:

File: [`src/main/java/com/commercetools/sync/shoppinglists/helpers/ShoppingListReferenceResolver.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/shoppinglists/helpers/ShoppingListReferenceResolver.java)

````java
// Chain the new resolver in the resolveReferences() method:
@Override
public CompletionStage<ShoppingListDraft> resolveReferences(
    @Nonnull final ShoppingListDraft shoppingListDraft) {
  return resolveCustomerReference(ShoppingListDraftBuilder.of(shoppingListDraft))
      .thenCompose(this::resolveStoreReference)   // <-- add this line
      .thenCompose(this::resolveCustomTypeReference)
      .thenCompose(this::resolveLineItemReferences)
      .thenCompose(this::resolveTextLineItemReferences)
      .thenApply(ShoppingListDraftBuilder::build);
}

// Add the resolve method:
@Nonnull
protected CompletionStage<ShoppingListDraftBuilder> resolveStoreReference(
    @Nonnull final ShoppingListDraftBuilder draftBuilder) {

  final StoreResourceIdentifier storeResourceIdentifier = draftBuilder.getStore();
  if (storeResourceIdentifier != null && storeResourceIdentifier.getId() == null) {
    try {
      final String storeKey = getKeyFromResourceIdentifier(storeResourceIdentifier);
      return completedFuture(
          draftBuilder.store(StoreResourceIdentifierBuilder.of().key(storeKey).build()));
    } catch (ReferenceResolutionException referenceResolutionException) {
      return exceptionallyCompletedFuture(
          new ReferenceResolutionException(
              format(FAILED_TO_RESOLVE_STORE_REFERENCE,
                  draftBuilder.getKey(),
                  referenceResolutionException.getMessage())));
    }
  } else if (storeResourceIdentifier != null && storeResourceIdentifier.getId() != null) {
    return completedFuture(
        draftBuilder.store(
            StoreResourceIdentifierBuilder.of().id(storeResourceIdentifier.getId()).build()));
  }
  return completedFuture(draftBuilder);
}
````

#### Reference resolution utils (mapping)

When syncing from a commercetools project, resources are fetched as full objects and must be converted to drafts. The `{Resource}ReferenceResolutionUtils` handles this mapping.

In `{Resource}ReferenceResolutionUtils.java`, add a mapping method and call it from `mapTo{Resource}Draft()`:

File: [`src/main/java/com/commercetools/sync/shoppinglists/utils/ShoppingListReferenceResolutionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/shoppinglists/utils/ShoppingListReferenceResolutionUtils.java)

````java
// In the mapToShoppingListDraft() method, add the store mapping:
return ShoppingListDraftBuilder.of()
    .name(shoppingList.getName())
    .key(shoppingList.getKey())
    .customer(customerResourceIdentifierWithKey)
    .store(mapToStoreResourceIdentifier(shoppingList))  // <-- add this line
    // ... other fields ...
    .build();

// Add the mapping method:
@Nullable
private static StoreResourceIdentifier mapToStoreResourceIdentifier(
    @Nonnull final ShoppingList shoppingList) {
  if (shoppingList.getStore() != null) {
    return StoreResourceIdentifierBuilder.of().key(shoppingList.getStore().getKey()).build();
  }
  return null;
}
````

#### Tests for reference resolution

Write tests for the reference resolution utils in `{Resource}ReferenceResolutionUtilsTest.java`:

````java
@Test
void mapToShoppingListDrafts_WithStoreReference_ShouldReturnDraftsWithStoreKey() {
  final ShoppingList mockShoppingList = mock(ShoppingList.class);
  when(mockShoppingList.getName()).thenReturn(ofEnglish("name"));
  when(mockShoppingList.getKey()).thenReturn("key");

  final StoreKeyReference storeKeyReference =
      StoreKeyReferenceBuilder.of().key("store-key").build();
  when(mockShoppingList.getStore()).thenReturn(storeKeyReference);
  // ... mock other fields as null ...

  final List<ShoppingListDraft> drafts =
      ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts(
          singletonList(mockShoppingList), referenceIdToKeyCache);

  assertThat(drafts).hasSize(1);
  assertThat(drafts.get(0).getStore()).isNotNull();
  assertThat(drafts.get(0).getStore().getKey()).isEqualTo("store-key");
}

@Test
void mapToShoppingListDrafts_WithNullStore_ShouldReturnDraftsWithNullStore() {
  final ShoppingList mockShoppingList = mock(ShoppingList.class);
  when(mockShoppingList.getName()).thenReturn(ofEnglish("name"));
  when(mockShoppingList.getKey()).thenReturn("key");
  when(mockShoppingList.getStore()).thenReturn(null);
  // ... mock other fields as null ...

  final List<ShoppingListDraft> drafts =
      ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts(
          singletonList(mockShoppingList), referenceIdToKeyCache);

  assertThat(drafts).hasSize(1);
  assertThat(drafts.get(0).getStore()).isNull();
}
````

### Products with SyncFilter

Products support filtering update actions by group via `SyncFilter`. When adding a field to Products:

**1. Add an enum value to `ActionGroup`** (if an appropriate group doesn't already exist):

File: [`src/main/java/com/commercetools/sync/products/ActionGroup.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/products/ActionGroup.java)

````java
public enum ActionGroup {
  NAME,
  DESCRIPTION,
  // ... existing values ...
  MY_NEW_FIELD  // Add your new group here
}
````

**2. Wrap the action builder with `buildActionIfPassesFilter`** in `ProductSyncUtils.buildActions()`:

File: [`src/main/java/com/commercetools/sync/products/utils/ProductSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/products/utils/ProductSyncUtils.java)

````java
buildActionIfPassesFilter(
    syncFilter,
    ActionGroup.MY_NEW_FIELD,
    () -> buildMyNewFieldUpdateAction(oldProduct, newProduct))
````

This ensures the field respects the blacklist/whitelist configuration that users set via `ProductSyncOptionsBuilder.syncFilter()`.

## Quick Reference: File Locations

For each resource type, the files you need to modify follow this pattern:

### All fields (simple and reference)

| Resource | UpdateActionUtils | SyncUtils | Test File |
|----------|-------------------|-----------|-----------|
| Products | [`ProductUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/products/utils/ProductUpdateActionUtils.java) | [`ProductSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/products/utils/ProductSyncUtils.java) | [`ProductUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/products/utils/ProductUpdateActionUtilsTest.java) |
| Categories | [`CategoryUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/categories/utils/CategoryUpdateActionUtils.java) | [`CategorySyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/categories/utils/CategorySyncUtils.java) | [`CategoryUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/categories/utils/CategoryUpdateActionUtilsTest.java) |
| ProductTypes | [`ProductTypeUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/producttypes/utils/ProductTypeUpdateActionUtils.java) | [`ProductTypeSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/producttypes/utils/ProductTypeSyncUtils.java) | [`ProductTypeUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/producttypes/utils/ProductTypeUpdateActionUtilsTest.java) |
| Types | [`TypeUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/types/utils/TypeUpdateActionUtils.java) | [`TypeSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/types/utils/TypeSyncUtils.java) | [`TypeUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/types/utils/TypeUpdateActionUtilsTest.java) |
| InventoryEntries | [`InventoryUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/utils/InventoryUpdateActionUtils.java) | [`InventorySyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/utils/InventorySyncUtils.java) | [`InventoryUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/inventories/utils/InventoryUpdateActionUtilsTest.java) |
| CartDiscounts | [`CartDiscountUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtils.java) | [`CartDiscountSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountSyncUtils.java) | [`CartDiscountUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountUpdateActionUtilsTest.java) |
| States | [`StateUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/states/utils/StateUpdateActionUtils.java) | [`StateSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/states/utils/StateSyncUtils.java) | [`StateUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/states/utils/StateUpdateActionUtilsTest.java) |
| TaxCategories | [`TaxCategoryUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/taxcategories/utils/TaxCategoryUpdateActionUtils.java) | [`TaxCategorySyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/taxcategories/utils/TaxCategorySyncUtils.java) | [`TaxCategoryUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/taxcategories/utils/TaxCategoryUpdateActionUtilsTest.java) |
| Customers | [`CustomerUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/customers/utils/CustomerUpdateActionUtils.java) | [`CustomerSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/customers/utils/CustomerSyncUtils.java) | [`CustomerUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/customers/utils/CustomerUpdateActionUtilsTest.java) |
| ShoppingLists | [`ShoppingListUpdateActionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/shoppinglists/utils/ShoppingListUpdateActionUtils.java) | [`ShoppingListSyncUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/shoppinglists/utils/ShoppingListSyncUtils.java) | [`ShoppingListUpdateActionUtilsTest.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/test/java/com/commercetools/sync/shoppinglists/utils/ShoppingListUpdateActionUtilsTest.java) |

### Additional files for reference fields only

| Resource | ReferenceResolver | ReferenceResolutionUtils |
|----------|-------------------|--------------------------|
| Products | [`ProductReferenceResolver.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/products/helpers/ProductReferenceResolver.java) | [`ProductReferenceResolutionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/products/utils/ProductReferenceResolutionUtils.java) |
| Categories | [`CategoryReferenceResolver.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/categories/helpers/CategoryReferenceResolver.java) | [`CategoryReferenceResolutionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/categories/utils/CategoryReferenceResolutionUtils.java) |
| InventoryEntries | [`InventoryReferenceResolver.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/helpers/InventoryReferenceResolver.java) | [`InventoryReferenceResolutionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/inventories/utils/InventoryReferenceResolutionUtils.java) |
| CartDiscounts | [`CartDiscountReferenceResolver.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/cartdiscounts/helpers/CartDiscountReferenceResolver.java) | [`CartDiscountReferenceResolutionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/cartdiscounts/utils/CartDiscountReferenceResolutionUtils.java) |
| Customers | [`CustomerReferenceResolver.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/customers/helpers/CustomerReferenceResolver.java) | [`CustomerReferenceResolutionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/customers/utils/CustomerReferenceResolutionUtils.java) |
| ShoppingLists | [`ShoppingListReferenceResolver.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/shoppinglists/helpers/ShoppingListReferenceResolver.java) | [`ShoppingListReferenceResolutionUtils.java`](https://github.com/commercetools/commercetools-sync-java/blob/master/src/main/java/com/commercetools/sync/shoppinglists/utils/ShoppingListReferenceResolutionUtils.java) |

All paths are relative to `src/main/java/com/commercetools/sync/` (source) and `src/test/java/com/commercetools/sync/` (tests).

## Behavioral Extensions via Hooks

The `beforeUpdateCallback` and `beforeCreateCallback` hooks are designed for customizing sync **behavior**, not for adding field support. They are called during the sync flow to allow you to intercept and modify requests before they are sent to the commercetools API.

**Use hooks when you need to:**
- **Filter out update actions** — e.g., prevent variant removals (see [`KeepOtherVariantsSync`](/src/main/java/com/commercetools/sync/products/templates/beforeupdatecallback/KeepOtherVariantsSync.java))
- **Restrict sync to a subset of data** — e.g., only sync a single locale (see [`SyncSingleLocale`](/src/main/java/com/commercetools/sync/products/templates/beforeupdatecallback/SyncSingleLocale.java))
- **Transform or enrich drafts before creation** — e.g., set computed fields or conditionally skip creation by returning `null`

**Do not use hooks to add field support.** If a resource field is not being synced, the correct approach is to add native support in the library code as described in this guide.

See the [Sync Options](SYNC_OPTIONS.md) documentation for callback signatures, configuration details, and additional examples.
