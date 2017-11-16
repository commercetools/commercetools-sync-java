# Release Notes

<!-- RELEASE NOTE FORMAT

1. Please use the following format for the release note subtitle
### {version} - {date}

2. link to commits of release.
3. link to Javadoc of release.
4. link to Jar of release.

5. Depending on the contents of the release use the subtitles below to 
  document the new changes in the release accordingly. Please always include
  a link to the releated issue number. 
   **New Features** (n)
   **Beta Features** (n)
   **Major Enhancements** (n)
   **Breaking Changes** (n)
   **Enhancements** (n)
   **Doc Fixes** (n)
   **Critical Bug Fixes** (n)
   **Bug Fixes** (n)
   **Hotfix** (n)
   - **Category Sync** - Sync now supports product variant images syncing. [#114](https://github.com/commercetools/commercetools-sync-java/issues/114)
   - **Build Tools** - Convinient handelling of env vars for integration tests.

6. Add Migration guide section which specifies explicitly if there are breaking changes and how to tackle them.

-->

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [v1.0.0-M4 -  Nov 7, 2017](#v100-m4----nov-7-2017)
- [v1.0.0-M3 -  Nov 3, 2017](#v100-m3----nov-3-2017)
- [v1.0.0-M2 -  Oct 12, 2017](#v100-m2----oct-12-2017)
- [v1.0.0-M2-beta -  Sep 28, 2017](#v100-m2-beta----sep-28-2017)
- [v1.0.0-M1 -  Sep 06, 2017](#v100-m1----sep-06-2017)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->
<!--
### v1.0.0-M5 -  Nov 7, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M4...v1.0.0-M5) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M5/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M5)

**New Features** (1)
- **Inventory Sync** - Introduces `beforeUpdateCallback` which is applied after generation of update actions and before 
actual InventoryEntry update. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)

**Enhancements** (1)
- **Commons** - `setUpdateActionsCallback` is now `beforeUpdateCallback` and takes a TriFunction instead of Function, 
which adds more information about the generated list of update actions, namley, the old resource being updated and the new
resource draft. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)

**Build Tools** (1)
- **Commons** - Appends library name and version to User-Agent headers of JVM SDK clients using the library. [#142](https://github.com/commercetools/commercetools-sync-java/issues/142)

**Doc Fixes** (1)
- **Build Tools** - Adds Snyk vulnerabilities badge to repo README. [#188](https://github.com/commercetools/commercetools-sync-java/pull/188)

**Migration guide** (8)
- **Commons** - Change name of `setUpdateActionsCallback` to `beforeUpdateCallback`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
- **Commons** - Change name of `setAllowUuid` to `allowUuid`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
- **Commons** - Change name of `setWarningCallBack` to `warningCallback`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
- **Commons** - Change name of `setErrorCallBack` to `errorCallback`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
- **Commons** - Change name of `setBatchSize` to `batchSize`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
- **Commons** - Remove `setRemoveOtherLocales` option. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
- **Commons** - Change name of `setRemoveOtherSetEntries`, `setRemoveOtherCollectionEntries` and `setRemoveOtherProperties` 
to `removeOtherSetEntries`, `removeOtherCollectionEntries` and `removeOtherProperties`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
- **Product Sync** - Change name of `setSyncFilter` to `syncFilter`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
-->
### v1.0.0-M4 -  Nov 7, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M3...v1.0.0-M4) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M4/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M4)

**Hotfix** (1)
- **Product Sync** - Fixes an issue with `replaceAttributesReferencesIdsWithKeys` which nullifies localized text attributes due 
to JSON parsing not throwing exception on parsing it to reference set. [#179](https://github.com/commercetools/commercetools-sync-java/issues/179)


### v1.0.0-M3 -  Nov 3, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M2...v1.0.0-M3) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M3/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M3)

**New Features** (7)
- **ProductSync** - Support Product TaxCategory reference resolution and syncing. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
- **ProductSync** - Support Product State reference resolution and syncing. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
- **ProductSync** - Expose `ProductReferenceReplacementUtils#buildProductQuery` util to create a product query with all needed reference expansions to fetch products from a source CTP project for the sync. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
- **ProductSync** - Expose `VariantReferenceReplacementUtils#replaceVariantsReferenceIdsWithKeys` which provides utils to replace reference ids with keys on variants (price and attriute references) coming from a source CTP project to make it ready for reference resolution. [#160](https://github.com/commercetools/commercetools-sync-java/issues/160).
- **ProductSync** - Expose `VariantReferenceResolver` which is a helper that resolves the price and attriute references on a ProductVariantDraft. (Note: This is used now by the already existing ProductReferenceResolver) [#160](https://github.com/commercetools/commercetools-sync-java/issues/160).
- **CategorySync** - Expose `CategoryReferenceReplacementUtils#buildCategoryQuery` util to create a category query with all needed reference expansions to fetch categories from a source CTP project for the sync. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
- **Commons** - Expose `replaceCustomTypeIdWithKeys` and `replaceReferenceIdWithKey`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).

**Bug Fixes** (1)
- **Category Sync** - Fixes an issue where retrying on concurrent modification exception wasn't re-fetching the latest 
Category and rebuilding build update actions. [#94](https://github.com/commercetools/commercetools-sync-java/issues/94)

**Doc Fixes** (6)
- **Product Sync** - Document the reason behind having the latest batch processing time. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
- **Category Sync** - Document the reason behind having the latest batch processing time. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
- **Category Sync** - Fix the statistics summary string used in the documentation. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
- **Inventory Sync** - Document the reason behind having the latest batch processing time. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
- **Product Sync** - Typo fixes. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)
- **Commons** - Provide inline example of how to use logging in callbacks. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)

**Migration guide** (9)
- **Product Sync** - Move `replaceProductsReferenceIdsWithKeys` from `SyncUtils` to `ProductReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
- **Product Sync** - Remove `replaceProductDraftsCategoryReferenceIdsWithKeys` which is not needed anymore. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
- **Product Sync** - Remove `replaceProductDraftCategoryReferenceIdsWithKeys` which is not needed anymore. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
- **Product Sync** - Remove `replaceCategoryOrderHintCategoryIdsWithKeys` which is not needed anymore. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
- **Product Sync** - Move `getDraftBuilderFromStagedProduct` from `SyncUtils` to `ProductReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
- **Category Sync** - Move `replaceCategoriesReferenceIdsWithKeys` from `SyncUtils` to `CategoryReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
- **Inventory Sync** - Move `replaceInventoriesReferenceIdsWithKeys` from `SyncUtils` to `InventoryReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
- **Commons** - Remove slf4j-simple dependency. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)
- **Commons** - Use implementation instead of compile configuration for dependencies. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)

### v1.0.0-M2 -  Oct 12, 2017 

[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M2-beta...v1.0.0-M2) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M2/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M2)

**New Features** (3)
- **Product Sync** - Support syncing entire product variant images, putting order into consideration. [#114](https://github.com/commercetools/commercetools-sync-java/issues/114)
- **Product Sync** - Expose `ProductVariantUpdateActionUtils#buildProductVariantImagesUpdateActions` and `ProductVariantUpdateActionUtils#buildMoveImageToPositionUpdateActions` action build util. [#114](https://github.com/commercetools/commercetools-sync-java/issues/114)
- **Product Sync** - Support Blacklisting/Whitelisting update action groups on sync. [#122](https://github.com/commercetools/commercetools-sync-java/issues/122)

**Bug Fixes** (4)
- **Build Tools** - Fixes issue were JavaDoc jar was not built. [#117](https://github.com/commercetools/commercetools-sync-java/issues/117)
- **Build Tools** - Fixes issue were JavaDoc was not published on github. [#118](https://github.com/commercetools/commercetools-sync-java/issues/118)
- **Product Sync** - Fixes a potential bug where an exisitng master variant key could be blank.[#122](https://github.com/commercetools/commercetools-sync-java/issues/122)
- **Product Sync** - Fixes a potential bug where a product draft could be provided with no master variant set. [#122](https://github.com/commercetools/commercetools-sync-java/issues/122)

**Enhancements** (2)
- **Build Tools** - Integration tests project credentials can now be set on a properties file not only as environment variables and give error messages if not set. [#105](https://github.com/commercetools/commercetools-sync-java/issues/105)
- **Product Sync** - Validates the SKU before making a `ChangeMasterVariant` request by SKU. [#122](https://github.com/commercetools/commercetools-sync-java/issues/122)

 **Doc Fixes** (5)
 - **Build Tools** - Adds bintray badge to repo. [#126](https://github.com/commercetools/commercetools-sync-java/issues/126)
 - **Product Sync** - Adds usage documentation. [#121](https://github.com/commercetools/commercetools-sync-java/issues/121)
 - **Commons** - Seperate contributing README into own README not in the main one. [#121](https://github.com/commercetools/commercetools-sync-java/issues/121)
 - **Commons** - Adds release notes doc. [#125](https://github.com/commercetools/commercetools-sync-java/issues/125)
 - **Build Tools** - Adds JavaDoc badge to repo. [#145](https://github.com/commercetools/commercetools-sync-java/issues/145)

**Migration guide**
- No breaking changes introduced.


### v1.0.0-M2-beta -  Sep 28, 2017 
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M1...v1.0.0-M2-beta) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M2-beta)

**Beta Features** (11)
- **Product Sync** - Support syncing products name, categories, categoryOrderHints, description, slug,  metaTitle, 
metaDescription, metaKeywords, masterVariant and searchKeywords. [#57](https://github.com/commercetools/commercetools-sync-java/issues/57)
- **Product Sync** -  Expose update action build utils for products name, categories, categoryOrderHints, description, slug,  metaTitle, 
metaDescription, metaKeywords, masterVariant and searchKeywords. [#57](https://github.com/commercetools/commercetools-sync-java/issues/57)
- **Product Sync** -  Reference resolution support for product categories, productType and prices. [#95](https://github.com/commercetools/commercetools-sync-java/issues/95)
[#96](https://github.com/commercetools/commercetools-sync-java/issues/96)
- **Product Sync** -  Support syncing products publish state. [#97](https://github.com/commercetools/commercetools-sync-java/issues/97)
- **Product Sync** -  Expose update action build utils for products publish state. [#97](https://github.com/commercetools/commercetools-sync-java/issues/97)
- **Product Sync** -  Support syncing products variant attributes. [#98](https://github.com/commercetools/commercetools-sync-java/issues/98)
- **Product Sync** -  Expose update action build utils for products variant attributes. [#98](https://github.com/commercetools/commercetools-sync-java/issues/98)
- **Product Sync** -  Support syncing products variant prices without update action calculation. [#99](https://github.com/commercetools/commercetools-sync-java/issues/99)
- **Product Sync** -  Support syncing products variant images. [#100](https://github.com/commercetools/commercetools-sync-java/issues/100)
- **Product Sync** -  Expose update action build utils for products variant images. [#100](https://github.com/commercetools/commercetools-sync-java/issues/100)
- **Product Sync** -  Support syncing products against staged projection. [#93](https://github.com/commercetools/commercetools-sync-java/issues/93)

**Migration guide**
- No breaking changes introduced.


### v1.0.0-M1 -  Sep 06, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/commits/v1.0.0-M1) | 
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M1)

**New Features** (16)
- **Category Sync** - Support syncing category name, description, orderHint, metaDescription, metaTitle, 
customFields and parent category. [#2](https://github.com/commercetools/commercetools-sync-java/issues/2)
- **Category Sync** - Expose update action build utils for category name, description, orderHint, metaDescription, metaTitle, 
customFields and parent category. [#2](https://github.com/commercetools/commercetools-sync-java/issues/2)
- **Category Sync** - Sync options builder support. [#5](https://github.com/commercetools/commercetools-sync-java/issues/5)
- **Category Sync** - Support of syncing categories in any order. [#28](https://github.com/commercetools/commercetools-sync-java/issues/28)
- **Category Sync** - Add concurrency modification exception repeater. [#30](https://github.com/commercetools/commercetools-sync-java/issues/30)
- **Category Sync** - Use category keys for matching. [#45](https://github.com/commercetools/commercetools-sync-java/issues/45)
- **Category Sync** - Reference resolution support. [#47](https://github.com/commercetools/commercetools-sync-java/issues/47)
- **Category Sync** - Batch processing support. [#73](https://github.com/commercetools/commercetools-sync-java/issues/73)
- **Category Sync** - Add info about missing parent categories in statistics. [#73](https://github.com/commercetools/commercetools-sync-java/issues/76)
- **Commons** - Sync statistics support. [#6](https://github.com/commercetools/commercetools-sync-java/issues/6)
- **Commons** - Sync ITs should use client that repeats on 5xx errors. [#31](https://github.com/commercetools/commercetools-sync-java/issues/31)
- **Commons** - Sync only accepts drafts. [#46](https://github.com/commercetools/commercetools-sync-java/issues/46)
- **Build Tools** - Travis setup as CI tool. [#1](https://github.com/commercetools/commercetools-sync-java/issues/1)
- **Build Tools** - Setup Bintray release and publising process. [#24](https://github.com/commercetools/commercetools-sync-java/issues/24)
- **Build Tools** - Setup CheckStyle, PMD, FindBugs, Jacoco and CodeCov. [#25](https://github.com/commercetools/commercetools-sync-java/issues/25)
- **Build Tools** - Setup repo PR and issue templates. [#29](https://github.com/commercetools/commercetools-sync-java/issues/29)

**Beta Features** (5)
- **Inventory Sync** - Support syncing inventory supplyChannel, quantityOnStock, restockableInDays, expectedDelivery 
and customFields. [#17](https://github.com/commercetools/commercetools-sync-java/issues/17)
- **Inventory Sync** - Expose update action build utils for inventory supplyChannel, quantityOnStock, restockableInDays, expectedDelivery 
and customFields. [#17](https://github.com/commercetools/commercetools-sync-java/issues/17)
- **Inventory Sync** - Sync options builder support. [#15](https://github.com/commercetools/commercetools-sync-java/issues/15)
- **Inventory Sync** - Reference resolution support. [#47](https://github.com/commercetools/commercetools-sync-java/issues/47)
- **Inventory Sync** - Batch processing support. [#73](https://github.com/commercetools/commercetools-sync-java/issues/73)
