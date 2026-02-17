# Supported Resources and Fields

This document lists all resource types supported by **commercetools-sync-java** and the specific fields that can be created and updated for each resource.

> **Note:** This library only **creates** and **updates** resources — it never deletes them.

---

## Overview

| Resource | Supported Fields | Complexity |
|----------|:----------------:|:----------:|
| [Products](#products) | 20+ | Very High |
| [Categories](#categories) | 12 | Medium |
| [ProductTypes](#producttypes) | 12 | Medium |
| [Types](#types) | 8 | Low |
| [InventoryEntries](#inventoryentries) | 6 | Low |
| [CartDiscounts](#cartdiscounts) | 12 | Medium |
| [States](#states) | 6 | Low |
| [TaxCategories](#taxcategories) | 3 | Low |
| [CustomObjects](#customobjects) | 3 | Low |
| [Customers](#customers) | 20+ | Medium |
| [ShoppingLists](#shoppinglists) | 10 | Medium |

---

## Products

Sync class: `ProductSync`

### Product-Level Fields

| Field | Update Action | Notes |
|-------|---------------|-------|
| `name` | `changeName` | LocalizedString |
| `description` | `setDescription` | LocalizedString |
| `slug` | `changeSlug` | LocalizedString |
| `searchKeywords` | `setSearchKeywords` | LocalizedString |
| `metaTitle` | `setMetaTitle` | LocalizedString |
| `metaDescription` | `setMetaDescription` | LocalizedString |
| `metaKeywords` | `setMetaKeywords` | LocalizedString |
| `categories` | `addToCategory` / `removeFromCategory` | Category references |
| `categoryOrderHints` | `setCategoryOrderHint` | Sort order per category |
| `taxCategory` | `setTaxCategory` | TaxCategory reference |
| `state` | `transitionState` | State reference |
| `publish` | `publish` / `unpublish` | Publication state |

### Variant-Level Fields

All variants (master variant and additional variants) support these fields:

| Field | Update Action | Notes |
|-------|---------------|-------|
| `sku` | `setSku` | Stock keeping unit |
| `attributes` | `setAttribute` / `setAttributeInAllVariants` | Custom attributes; `SameForAll` constraint supported |
| `prices` | `addPrice` / `changePrice` / `removePrice` | Includes value, tiers, and custom fields per price |
| `images` | `addExternalImage` / `removeImage` / `moveImageToPosition` | Image URLs and ordering |
| `assets` | `addAsset` / `removeAsset` / `changeAssetName` / `setAssetDescription` / `setAssetSources` / `setAssetTags` | Includes custom fields per asset |

### Variant Management

| Operation | Update Action | Notes |
|-----------|---------------|-------|
| Add variant | `addVariant` | Add new variants to the product |
| Remove variant | `removeVariant` | Remove variants (except master variant) |
| Change master variant | `changeMasterVariant` | Swap which variant is the master |

### Filtering

Product sync supports **sync filtering** via `SyncFilter` and `ActionGroup` to include or exclude specific field groups from the sync process.

---

## Categories

Sync class: `CategorySync`

| Field | Update Action | Notes |
|-------|---------------|-------|
| `name` | `changeName` | LocalizedString |
| `slug` | `changeSlug` | LocalizedString |
| `description` | `setDescription` | LocalizedString |
| `parent` | `changeParent` | Category reference; cannot be unset |
| `orderHint` | `changeOrderHint` | Cannot be unset |
| `externalId` | `setExternalId` | |
| `metaTitle` | `setMetaTitle` | LocalizedString |
| `metaDescription` | `setMetaDescription` | LocalizedString |
| `metaKeywords` | `setMetaKeywords` | LocalizedString |
| `assets` | `addAsset` / `removeAsset` / `changeAssetName` / `setAssetDescription` / `setAssetSources` / `setAssetTags` | Includes custom fields per asset |
| `custom` | `setCustomType` / `setCustomField` | Custom fields |

---

## ProductTypes

Sync class: `ProductTypeSync`

### ProductType-Level Fields

| Field | Update Action | Notes |
|-------|---------------|-------|
| `name` | `changeName` | |
| `description` | `changeDescription` | |

### Attribute Definition Management

| Operation | Update Action | Notes |
|-----------|---------------|-------|
| Add attribute | `addAttributeDefinition` | |
| Remove attribute | `removeAttributeDefinition` | |
| Reorder attributes | `changeAttributeOrder` | |

### Per-Attribute Definition Fields

| Field | Update Action | Notes |
|-------|---------------|-------|
| `label` | `changeLabel` | LocalizedString |
| `inputTip` | `setInputTip` | LocalizedString |
| `isSearchable` | `changeIsSearchable` | Boolean |
| `inputHint` | `changeInputHint` | SingleLine / MultiLine |
| `attributeConstraint` | `changeAttributeConstraint` | Only `SameForAll` → `None` or `Unique` → `None` |
| `enumValues` | `addEnumValue` / `changeEnumValueOrder` / `changeEnumValueLabel` | For `AttributeEnumType` |
| `localizedEnumValues` | `addLocalizedEnumValue` / `changeLocalizedEnumValueOrder` / `changeLocalizedEnumValueLabel` | For `AttributeLocalizedEnumType` |

---

## Types

Sync class: `TypeSync`

### Type-Level Fields

| Field | Update Action | Notes |
|-------|---------------|-------|
| `name` | `changeName` | LocalizedString |
| `description` | `setDescription` | LocalizedString |

### Field Definition Management

| Operation | Update Action | Notes |
|-----------|---------------|-------|
| Add field definition | `addFieldDefinition` | |
| Remove field definition | `removeFieldDefinition` | |
| Reorder field definitions | `changeFieldDefinitionOrder` | |

### Per-Field Definition Fields

| Field | Update Action | Notes |
|-------|---------------|-------|
| `label` | `changeLabel` | LocalizedString |
| `inputHint` | `changeInputHint` | SingleLine / MultiLine |
| `enumValues` | `addEnumValue` / `changeEnumValueOrder` / `changeEnumValueLabel` | For `CustomFieldEnumType` |
| `localizedEnumValues` | `addLocalizedEnumValue` / `changeLocalizedEnumValueOrder` / `changeLocalizedEnumValueLabel` | For `CustomFieldLocalizedEnumType` |

---

## InventoryEntries

Sync class: `InventorySync`

| Field | Update Action | Notes |
|-------|---------------|-------|
| `quantityOnStock` | `changeQuantity` | Defaults to 0 if null |
| `restockableInDays` | `setRestockableInDays` | |
| `expectedDelivery` | `setExpectedDelivery` | ZonedDateTime |
| `supplyChannel` | `setSupplyChannel` | Channel reference |
| `custom` | `setCustomType` / `setCustomField` | Custom fields |

---

## CartDiscounts

Sync class: `CartDiscountSync`

| Field | Update Action | Notes |
|-------|---------------|-------|
| `name` | `changeName` | LocalizedString |
| `description` | `setDescription` | LocalizedString |
| `value` | `changeValue` | Supports Absolute, Relative, Fixed, and GiftLineItem |
| `cartPredicate` | `changeCartPredicate` | |
| `target` | `changeTarget` | |
| `sortOrder` | `changeSortOrder` | |
| `isActive` | `changeIsActive` | Boolean; defaults to `true` |
| `requiresDiscountCode` | `changeRequiresDiscountCode` | Boolean; defaults to `false` |
| `validFrom` | `setValidFrom` | ZonedDateTime; when both `validFrom` and `validUntil` change, `setValidFromAndUntil` is used instead |
| `validUntil` | `setValidUntil` | ZonedDateTime; see `validFrom` note above |
| `stackingMode` | `changeStackingMode` | Defaults to `Stacking` |
| `custom` | `setCustomType` / `setCustomField` | Custom fields |

---

## States

Sync class: `StateSync`

| Field | Update Action | Notes |
|-------|---------------|-------|
| `type` | `changeType` | |
| `name` | `setName` | LocalizedString |
| `description` | `setDescription` | LocalizedString |
| `initial` | `changeInitial` | Boolean |
| `roles` | `addRoles` / `removeRoles` | |
| `transitions` | `setTransitions` | State references |

---

## TaxCategories

Sync class: `TaxCategorySync`

| Field | Update Action | Notes |
|-------|---------------|-------|
| `name` | `changeName` | |
| `description` | `setDescription` | |
| `taxRates` | `addTaxRate` / `removeTaxRate` / `replaceTaxRate` | Matched by key |

---

## CustomObjects

Sync class: `CustomObjectSync`

| Field | Notes |
|-------|-------|
| `container` | Part of the composite identifier |
| `key` | Part of the composite identifier |
| `value` | JSON value; compared for equality; upsert-based (create or replace) |

> CustomObjects use an **upsert** approach — if a custom object with the same `container` and `key` exists, its `value` is replaced. No individual update actions are generated.

---

## Customers

Sync class: `CustomerSync`

### Personal Information

| Field | Update Action | Notes |
|-------|---------------|-------|
| `email` | `changeEmail` | |
| `firstName` | `setFirstName` | |
| `lastName` | `setLastName` | |
| `middleName` | `setMiddleName` | |
| `title` | `setTitle` | |
| `salutation` | `setSalutation` | |
| `dateOfBirth` | `setDateOfBirth` | |
| `companyName` | `setCompanyName` | |
| `vatId` | `setVatId` | |
| `locale` | `setLocale` | |
| `customerNumber` | `setCustomerNumber` | Immutable once set; triggers warning if changed |
| `externalId` | `setExternalId` | |

### References

| Field | Update Action | Notes |
|-------|---------------|-------|
| `customerGroup` | `setCustomerGroup` | CustomerGroup reference |
| `stores` | `addStore` / `removeStore` / `setStores` | Store references |

### Addresses

| Field | Update Action | Notes |
|-------|---------------|-------|
| `addresses` | `addAddress` / `changeAddress` / `removeAddress` | Matched by address key |
| `defaultShippingAddress` | `setDefaultShippingAddress` | |
| `defaultBillingAddress` | `setDefaultBillingAddress` | |
| `shippingAddressIds` | `addShippingAddressId` / `removeShippingAddressId` | |
| `billingAddressIds` | `addBillingAddressId` / `removeBillingAddressId` | |

### Custom Fields

| Field | Update Action | Notes |
|-------|---------------|-------|
| `custom` | `setCustomType` / `setCustomField` | Custom fields |

---

## ShoppingLists

Sync class: `ShoppingListSync`

### ShoppingList-Level Fields

| Field | Update Action | Notes |
|-------|---------------|-------|
| `name` | `changeName` | LocalizedString |
| `slug` | `setSlug` | LocalizedString |
| `description` | `setDescription` | LocalizedString |
| `customer` | `setCustomer` | Customer reference |
| `store` | `setStore` | Store reference |
| `anonymousId` | `setAnonymousId` | |
| `deleteDaysAfterLastModification` | `setDeleteDaysAfterLastModification` | |
| `custom` | `setCustomType` / `setCustomField` | Custom fields |

### Line Items

| Operation | Update Action | Notes |
|-----------|---------------|-------|
| Add line item | `addLineItem` | |
| Remove line item | `removeLineItem` | |
| Update line item quantity | `changeLineItemQuantity` | |
| Update line item custom fields | `setLineItemCustomType` / `setLineItemCustomField` | Custom fields per line item |

### Text Line Items

| Operation | Update Action | Notes |
|-----------|---------------|-------|
| Add text line item | `addTextLineItem` | |
| Remove text line item | `removeTextLineItem` | |
| Update text line item name | `changeTextLineItemName` | |
| Update text line item description | `setTextLineItemDescription` | |
| Update text line item quantity | `changeTextLineItemQuantity` | |
| Update text line item custom fields | `setTextLineItemCustomType` / `setTextLineItemCustomField` | Custom fields per text line item |
