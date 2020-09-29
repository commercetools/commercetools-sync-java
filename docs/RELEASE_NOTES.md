# Release Notes

<!-- RELEASE NOTE FORMAT

1. Please use the following format for the release note subtitle
### {version} - {date}

2. link to commits of release.
3. link to Javadoc of release.
4. link to Jar of release.

5. Add a summary of the release that is not too detailed or technical.

6. Depending on the contents of the release use the subitems below to 
  document the new changes in the release accordingly. Please always include
  a link to the releated issue number. 
   **New Features** (n) 🎉 
   **Breaking Changes** (n) 🚧 
   **Enhancements** (n) ✨
   **Dependency Updates** (n) 🛠️ 
   **Documentation** (n) 📋
   **Critical Bug Fixes** (n) 🔥 
   **Bug Fixes** (n)🐞
   - **Category Sync** - Sync now supports product variant images syncing. [#114](https://github.com/commercetools/commercetools-sync-java/issues/114)
   - **Build Tools** - Convinient handelling of env vars for integration tests.

7. Add Migration guide section which specifies explicitly if there are breaking changes and how to tackle them.

-->

### 2.2.1 - Sep 29, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/2.2.0...2.2.1) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/2.2.1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/2.2.1)

 🐞 **Bug Fixes** (1)
    - **Product Sync** - Fixed a bug in the `ProductSync` related handling of unresolved product references provided in 
    different batches. [#580](https://github.com/commercetools/commercetools-sync-java/issues/580)


### 2.2.0 - Sep 25, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/2.1.0...2.2.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/2.2.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/2.2.0)

- 🎉 **New Features** (2)
    - **Product Sync** - Added support for resolving `key-value-document` (custom object) references on attributes of type `Reference`, `Set` of `Reference`, `NestedType` or `Set` of `NestedType`. [#564](https://github.com/commercetools/commercetools-sync-java/issues/564)     
    - Introduced new concept for the validation of `the drafts in batches` for each `Sync` instance, exposed with 
    `BaseBatchValidator` implementations (i.e. ProductBatchValidator, CategoryBatchValidator). [#233](https://github.com/commercetools/commercetools-sync-java/issues/233)
    
- ✨ **Enhancements** (2)    
    - **Category Sync** - Passed category keys in batch to `cacheKeysToIds` method of `CategoryService` to avoid fetching all categories for every batch. 
[#235](https://github.com/commercetools/commercetools-sync-java/issues/235)
    - Populated `keyToId` caches in services before reference resolution to improve the performance of the library 
    with collecting referenced keys in batches of drafts. [#235](https://github.com/commercetools/commercetools-sync-java/issues/235)

- 🛠️ **Dependency Updates** (1)
    - `mockito-junit-jupiter` `3.5.11` ->  [`3.5.13`](https://github.com/mockito/mockito/releases/tag/v3.5.13) 


### 2.1.0 - Sep 21, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/2.0.0...2.1.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/2.1.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/2.1.0)
- 🎉 **New Features** (2)
    - **CustomObject Sync** - Added support for syncing custom objects between ctp projects. [#565](https://github.com/commercetools/commercetools-sync-java/issues/565) For more info how to use it please refer to [CustomObject usage doc](/docs/usage/CUSTOM_OBJECT_SYNC.md).
    - **CustomObject Sync** - Exposed `CustomObjectSyncUtils#hasIdenticalValue` which determines whether update process is required after comparing a `CustomObject` and a `CustomObjectDraft`. [#565](https://github.com/commercetools/commercetools-sync-java/issues/565)
    
- 🛠️ **Dependency Updates** (3)
    - `org.ajoberstar.git-publish` `2.1.3` -> [`3.0.0`](https://github.com/ajoberstar/gradle-git-publish/releases/tag/3.0.0) 
    - `org.ajoberstar.grgit` `4.0.2` -> [`4.1.0`](https://github.com/ajoberstar/grgit/releases/tag/4.1.0)
    - `mockito-junit-jupiter` `3.5.10` ->  [`3.5.11`](https://github.com/mockito/mockito/releases/tag/v3.5.11) 

### 2.0.0 - Sept 14, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.9.1...2.0.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/2.0.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/2.0.0)

- 🚧 **Breaking Changes** (2)
    - Sync options:
        - The signatures of the `errorCallback` and `warningCallback` changed and their parameter lists are extended.
            From now on the resource draft of the source project, the resource of the target project and optionally the failed update actions
            passed to the callbacks. Refer [sync options](./usage/SYNC_OPTIONS.md) for more details. [#107](https://github.com/commercetools/commercetools-sync-java/issues/107)
    - Reference resolution utilities:
        - **Commons** - Renamed `replaceCustomTypeIdWithKeys` to `mapToCustomFieldsDraft`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)
        - **Commons** - Renamed `replaceAssetsReferencesIdsWithKeys` to `mapToAssetDrafts`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)
        - **Category Sync** - Renamed `replaceCategoriesReferenceIdsWithKeys` to `mapToCategoryDrafts`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)
        - **CartDiscount Sync** - Renamed `replaceCartDiscountsReferenceIdsWithKeys` to `mapToCartDiscountDrafts`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)
        - **Inventory Sync** - Renamed `replaceInventoriesReferenceIdsWithKeys` to `mapToInventoryEntryDrafts`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)     
        - **Product Sync** - Renamed `replaceProductsReferenceIdsWithKeys` to `mapToProductDrafts`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)
        - **State Sync** - Renamed `replaceStateReferenceIdsWithKeys` to `mapToStateDrafts`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)
        - **ProductType Sync** - Renamed `replaceProductTypesReferenceIdsWithKeys` to `mapToProductTypeDrafts`. [#138](https://github.com/commercetools/commercetools-sync-java/issues/138)

- ✨ **Enhancements** (1)        
    -  The library will fail fast for the non-existing references that found during the reference resolution. [#219](https://github.com/commercetools/commercetools-sync-java/issues/219)

- 🛠️ **Dependency Updates** (4)
    - `commercetools-jvm-sdk` `1.52.0` -> [`1.53.0`](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_53_0)
    - `org.assertj.assertj-core` `3.16.0` ->  [`3.17.2`](https://assertj.github.io/doc/#assertj-core-3-17-2-release-notes)
    - `junit.jupiterApiVersion` `5.6.2` ->  [`5.7.0`](https://github.com/junit-team/junit5/releases/tag/r5.7.0)
    - `mockito-junit-jupiter` `3.4.4` -> [`3.5.10`](https://github.com/mockito/mockito/releases/tag/v3.5.10)
    - `com.github.ben-manes.versions` `0.29.0` -> [`0.33.0`](https://github.com/ben-manes/gradle-versions-plugin/releases/tag/v0.33.0) 
    
### 1.9.1 -  Aug 5, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.9.0...1.9.1) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.9.1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.9.1)

- 🐞 **Bug Fixes** (1)
    - **Product Sync** - Fixed a bug in the `ProductSync` related to publish/unpublish of the product update actions,
    when a new product draft has publish flag set to true and the existing product is published already then no publish action will be created 
    which was not correct [#530](https://github.com/commercetools/commercetools-sync-java/issues/530)

### 1.9.0 -  July 27, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.8.2...1.9.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.9.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.9.0)

- 🎉 **New Features** (6)
    - **TaxCategory Sync** - Added support for syncing tax categories. [#417](https://github.com/commercetools/commercetools-sync-java/issues/417) For more info how to use it please refer to [TaxCategory usage doc](/docs/usage/TAX_CATEGORY_SYNC.md).
    - **TaxCategory Sync** - Exposed `TaxCategorySyncUtils#buildActions` which calculates all needed update actions after comparing a `TaxCategory` and a `TaxCategoryDraft`. [#417](https://github.com/commercetools/commercetools-sync-java/issues/417)
    - **TaxCategory Sync** - Exposed `TaxCategoryUpdateActionUtils` which contains utils for calculating needed update actions after comparing individual fields of a `TaxCategory` and a `TaxCategoryDraft`. [#417](https://github.com/commercetools/commercetools-sync-java/issues/417)
    - **State Sync** - Added support for syncing states. [#409](https://github.com/commercetools/commercetools-sync-java/issues/409) For more info how to use it please refer to [States usage doc](/docs/usage/STATE_SYNC.md).
    - **State Sync** - Exposed `StateSyncUtils#buildActions` which calculates all needed update actions after comparing a `State` and a `StateDraft`. [#409](https://github.com/commercetools/commercetools-sync-java/issues/409)
    - **State Sync** - Exposed `StateUpdateActionUtils` which contains utils for calculating needed update actions after comparing individual fields of a `State` and a `StateDraft`. [#409](https://github.com/commercetools/commercetools-sync-java/issues/409)
   Thanks, @jarzynp for the contributions!
        
- 🛠️ **Dependency Updates** (6)
    - `com.adarshr.test-logger` `2.0.0` -> [`2.1.0`](https://github.com/radarsh/gradle-test-logger-plugin/releases/tag/v2.1.0)
    - `org.assertj.assertj-core` `3.15.0` ->  [`3.16.0`](https://assertj.github.io/doc/#assertj-core-3-16-1-release-notes)
    - `junit.jupiterApiVersion` `5.6.1` ->  [`5.6.2`](https://github.com/junit-team/junit5/releases/tag/r5.6.2)
    - `commercetools-jvm-sdk` `1.51.0` -> [`1.52.0`](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_52_0)
    - `mockito-junit-jupiter` `3.3.3` -> [`3.4.4`](https://github.com/mockito/mockito/releases/tag/v3.4.4)
    - `com.github.ben-manes.versions` `0.28.0` -> [`0.29.0`](https://github.com/ben-manes/gradle-versions-plugin/releases/tag/v0.29.0) 
        
### 1.8.2 -  April 30, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.8.1...1.8.2) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.8.2/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.8.2)

- 🐞 **Bug Fixes** (2)
    - **Commons** - Fixed a bug in the Sync implementations causing the sync fail with throwing `ClassCastException`. [#466](https://github.com/commercetools/commercetools-sync-java/issues/466)
    - **Product Sync** - Fixed a bug in the `ProductSync` related to the ordering of variant update actions, which was not correct when there is `SetAttributeInAllVariants` action in update actions.
    Thanks, @ahmed-ali225, for the contributions! [#513](https://github.com/commercetools/commercetools-sync-java/issues/513)

### 1.8.1 -  April 22, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.8.0...1.8.1) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.8.1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.8.1)

- ✨ **Enhancements** (1)
    - **Commons** - Remove final keyword on interface/abstract method params. [#165](https://github.com/commercetools/commercetools-sync-java/issues/165)
        
- 🐞 **Bug Fixes** (3)
    - **CartDiscount Sync** - Fixed a bug in the `CartDiscountSync` which generates a `changeValue` action when there is no change, causing the sync to fail for that cart discount.
    Thanks, @michaelbannister, for the contributions! [#494](https://github.com/commercetools/commercetools-sync-java/issues/494)
    - **Product Sync** - Fixed a bug in the `ProductSync` when `ensurePriceChannels` is enabled in ProductSyncOptions should not create a missing channel used on product variant price draft.
        [#499](https://github.com/commercetools/commercetools-sync-java/issues/499)
    - **Product/Category Sync** - Validate that asset keys are always defined on the supplied drafts and the existing target resources (products/categories).
        [#366](https://github.com/commercetools/commercetools-sync-java/issues/366)

- 🛠️ **Dependency Updates** (5)
    - `junit.jupiterApiVersion` `5.5.2` ->  [`5.6.1`](https://github.com/junit-team/junit5/releases/tag/r5.6.1)
    - `commercetools-jvm-sdk` `1.48.0` -> [`1.51.0`](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_51_0)
    - `org.assertj.assertj-core` `3.14.0` ->  [`3.15.0`](https://assertj.github.io/doc/#assertj-core-3-15-0-release-notes)
    - `mockito-junit-jupiter` `3.2.4` -> [`3.3.3`](https://github.com/mockito/mockito/releases/tag/v3.3.3)
    - `com.github.ben-manes.versions` `0.27.0` -> [`0.28.0`](https://github.com/ben-manes/gradle-versions-plugin/releases/tag/v0.28.0) 
    - `ru.vyarus.mkdocs` `2.0.0` -> [`2.0.1`](https://github.com/xvik/gradle-mkdocs-plugin/releases/tag/2.0.1) 
    - `org.ajoberstar.grgit` `4.0.1` -> [`4.0.2`](https://github.com/ajoberstar/grgit/releases/tag/4.0.2) 
    
### 1.8.0 -  Jan 17, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.7.0...1.8.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.8.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.8.0)

- ✨ **Enhancements** (1)
    - **Inventory Sync** - Only cache the needed keys of `Channel` references instead of 
    caching all keys of such resources. [#198](https://github.com/commercetools/commercetools-sync-java/issues/198) 
        **Note**: This might have performance implications on the inventory sync, since now every non cached key-id entry will be individually fetched. 
        However, issue [#235](https://github.com/commercetools/commercetools-sync-java/issues/235) should address this.

### 1.7.0 -  Jan 7, 2020
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.6.1...1.7.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.7.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.7.0)


- ✨ **Enhancements** (2)
    - **Product Sync** - Only cache the needed keys of `Category`, `ProductType` and `Type` references instead of 
    caching all keys of such resources. [#418](https://github.com/commercetools/commercetools-sync-java/issues/418) 
        **Note**: This might have performance implications on the product sync, since now every non cached key-id entry will be individually fetched. 
        However, issue [#235](https://github.com/commercetools/commercetools-sync-java/issues/235) should address this.
    
    - **Commons** - Refactor duplicate implementations in concrete services and generalise it in the `BaseService`. 
    Thanks, @jarzynp, for the contributions! [#418](https://github.com/commercetools/commercetools-sync-java/issues/418)
    
- 🐞 **Bug Fixes** (1)
    - **Product Sync** - Fixed a bug in the Product Sync where keys with special characters failed to be saved 
    for `CustomObject`s, as the characters weren't allowed on the commmercetools platform. 
    [#474](https://github.com/commercetools/commercetools-sync-java/issues/474)
    **Note**: 🚧 This is a breaking change. Previously stored custom objects representing product drafts with unresolved references, won't be   
    synced with this version, since the key is now treated differently. Make sure to sync such drafts again with this version of the library.

- 🛠️ **Dependency Updates** (5)
    - `org.ajoberstar.grgit` `3.1.1` ->  [`4.0.1`](https://github.com/ajoberstar/grgit/releases/tag/4.0.1)
    - `org.ajoberstar.git-publish` `2.1.1` ->  [`2.1.3`](https://github.com/ajoberstar/gradle-git-publish/releases/tag/2.1.3)
    - `mockito-junit-jupiter` `3.1.0` -> [`3.2.4`](https://github.com/mockito/mockito/releases/tag/v3.2.4)
    - `commercetools-jvm-sdk` `1.47.0` -> [`1.48.0`](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_48_0)
    - `org.assertj.assertj-core` `3.13.2` ->  [`3.14.0`](https://assertj.github.io/doc/#assertj-core-3-14-0-release-notes)

### 1.6.1 -  Oct 17, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.6.0...1.6.1) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.6.1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.6.1)

- 🐞 **Bug Fixes** (3)
    - **Commons** - Fixed a bug in the `CtpQueryUtils` which was overwriting the query input query for every page after
the first page is fetched, eventually fetching more than needed. [#463](https://github.com/commercetools/commercetools-sync-java/issues/463)
    - **Product Sync** - Fixed a potential bug in reference resolution of attribute references in case a `null` reference
is passed in an attribute draft of type `Set` of `Reference`. [#441](https://github.com/commercetools/commercetools-sync-java/issues/441) 
    - **ProductType Sync** - Fixed a bug in the productType sync where the statistics `failed` counter was being counted on
failed fetches of missing references. [#426](https://github.com/commercetools/commercetools-sync-java/issues/426)

- 🛠️ **Dependency Updates** (4)
    - `mockito-junit-jupiter` `3.0.0` -> [`3.1.0`](https://github.com/mockito/mockito/releases/tag/v3.1.0)
    - `com.adarshr.test-logger` `1.7.1` -> [`2.0.0`](https://github.com/radarsh/gradle-test-logger-plugin/releases/tag/v2.0.0)
    - `com.github.ben-manes.versions` `0.25.0` -> [`0.27.0`](https://github.com/ben-manes/gradle-versions-plugin/releases/tag/v0.27.0) 
    - `commercetools-jvm-sdk` `1.46.0` -> [`1.47.0`](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_47_0)


### 1.6.0 -  Oct 10, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.5.0...1.6.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.6.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.6.0)

- 🎉 **New Features** (1)
    - **Product Sync** - Introduced support for syncing products with other product references as attributes in any order. [#447](https://github.com/commercetools/commercetools-sync-java/issues/447)

- 🛠️ **Dependency Updates** (1)
    - `com.adarshr.test-logger` 1.7.0 -> 1.7.1 [#456](https://github.com/commercetools/commercetools-sync-java/pull/456)

### 1.5.0 -  Sept 13, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.4.1...1.5.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.5.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.5.0)

- 🎉 **New Features** (4)
    - **Product Sync** - Added support for resolving `Product` references on attributes of type `Reference`, `Set` of `Reference`, `NestedType` or `Set` of `NestedType`. [#438](https://github.com/commercetools/commercetools-sync-java/issues/438)
    - **Product Sync** - Added support for resolving `Category` references on attributes of type `Reference`, `Set` of `Reference`, `NestedType` or `Set` of `NestedType`. [#440](https://github.com/commercetools/commercetools-sync-java/issues/440)
    - **Product Sync** - Added support for resolving `ProductType` references on attributes of type `Reference`, `Set` of `Reference`, `NestedType` or `Set` of `NestedType`. [#440](https://github.com/commercetools/commercetools-sync-java/issues/443)
    - **Commons** - Exposed `ResourceIdentifierUtils#isReferenceOfType` utility which checks if a JSON representation of a CTP `Reference` object is of a certain `typeId` or not. [#443](https://github.com/commercetools/commercetools-sync-java/issues/443)
    
- 🚧 **Breaking Changes** (1)
    - **Product Sync** - Unexposed the methods `VariantReferenceResolver#resolveAttributeReferences` and `VariantReferenceResolver#resolveAttributeReference` to be `private` as they are only meant for internal use of the library. [#440](https://github.com/commercetools/commercetools-sync-java/issues/440)
    
- 🛠️ **Dependency Updates** (5)
    - Gradle 5.6.1 -> [5.6.2](https://docs.gradle.org/5.6.2/release-notes.html)
    - `org.junit.jupiter:junit-jupiter-api` 5.5.1 -> [5.5.2](https://junit.org/junit5/docs/snapshot/release-notes/index.html#release-notes-5.5.2)
    - `org.junit.jupiter:junit-jupiter-engine` 5.5.1 -> [5.5.2](https://junit.org/junit5/docs/snapshot/release-notes/index.html#release-notes-5.5.2)
    - `org.junit.jupiter:junit-jupiter-params` 5.5.1 -> [5.5.2](https://junit.org/junit5/docs/snapshot/release-notes/index.html#release-notes-5.5.2)
    - `com.github.ben-manes.versions` 0.22.0 -> [0.25.0](https://github.com/ben-manes/gradle-versions-plugin/releases/tag/v0.25.0)

### 1.4.1 -  Sept 2, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.4.0...1.4.1) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.4.1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.4.1)

- 🐞 **Bug Fixes** (1)
    - **Commons** - Fixed a bug in the custom fields update actions builders which generated duplicated unnecessary update actions for `null` custom field values. This affected any sync module where the resource contained custom fields (i.e. Product Sync, Category Sync, CartDiscount Sync and Inventory Sync). It also affected any update actions building utility in which the resource/sub-resource contained custom fields.  [#428](https://github.com/commercetools/commercetools-sync-java/issues/428)

- 🛠️ **Enhancements** (1)
    - **Commons** - Bumped commercetools-jvm-sdk to version [1.46.0](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_46_0) which includes a fix for a serialization bug in the `SetCustomField` action which was ignoring empty array values. [#430](https://github.com/commercetools/commercetools-sync-java/issues/430)

### 1.4.0 -  Aug 8, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.3.0...1.4.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.4.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.4.0)


- 🎉 **New Features** (5)
    - **ProductType Sync** - Introduced support for syncing product types with NestedType (or set of NestedType) attributes in any order. [#372](https://github.com/commercetools/commercetools-sync-java/issues/372)
    - **ProductType Sync** - Introduced the new `ProductTypeReferenceReplacementUtils#replaceProductTypesReferenceIdsWithKeys` which is a util that replaces the reference ids with keys in a list of productTypes. [#372](https://github.com/commercetools/commercetools-sync-java/issues/372)
    - **ProductType Sync** - Introduced the new `ProductTypeReferenceReplacementUtils#buildProductTypeQuery` utils to create a product type query with all needed reference expansions to fetch productTypes from a source CTP project for the sync. [#372](https://github.com/commercetools/commercetools-sync-java/issues/372)
    - **ProductType Sync** - Introduced the new `ProductTypeReferenceResolver` which resolves productType references on ProductTypeDrafts. [#372](https://github.com/commercetools/commercetools-sync-java/issues/372)
    - **ProductType Sync** - Introduced the new methods `ProductTypeSyncStatistics#getNumberOfProductTypesWithMissingNestedProductTypes` and `ProductTypeSyncStatistics#getProductTypeKeysWithMissingParents` which represents the nested product types which are still not resolved. [#372](https://github.com/commercetools/commercetools-sync-java/issues/372)

- 🛠️ **Enhancements** (8)
    - **Commons** - Bumped commercetools-jvm-sdk to version [1.45.0](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_45_0).
    - **Commons** - Bumped gradle to version [gradle-5.5.1](https://docs.gradle.org/5.5.1/release-notes.html)
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-api` to 5.5.1.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-engine` to 5.5.1.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-params` to 5.5.1.
    - **Commons** - Bumped `mockito-junit-jupiter` dependency to 3.0.0.
    - **Commons** - Bumped `assertj` to 3.13.2.
    - **Commons** - Bumped `com.github.ben-manes.versions` to 0.22.0.

### 1.3.0 -  Jul 3, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.2.0...1.3.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.3.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.3.0)

- 🎉 **New Features** (6)
    - **CartDiscount Sync** - Added support for syncing cart discounts. [#379](https://github.com/commercetools/commercetools-sync-java/issues/379) For more info how to use it please refer to [CartDiscount usage doc](/docs/usage/CART_DISCOUNT_SYNC.md).
    - **CartDiscount Sync** - Introduced the new `CartDiscountSyncUtils#buildActions` which calculates all needed update actions after comparing a `CartDiscount` and a `CartDiscountDraft`. [#379](https://github.com/commercetools/commercetools-sync-java/issues/379)
    - **CartDiscount Sync** - Introduced the new `CartDiscountUpdateActionUtils` which contains utils for calculating needed update actions after comparing individual fields of a `CartDiscount` and a `CartDiscountDraft`. [#379](https://github.com/commercetools/commercetools-sync-java/issues/379)
    - **CartDiscount Sync** - Introduced the new `CartDiscountReferenceResolver` which resolves custom type references on CartDiscountDrafts. [#379](https://github.com/commercetools/commercetools-sync-java/issues/379)
    - **CartDiscount Sync** - Introduced the new `CartDiscountReferenceReplacementUtils#replaceCartDiscountsReferenceIdsWithKeys` which is a util that replaces the custom type ids with keys in a list of cartDiscounts. [#379](https://github.com/commercetools/commercetools-sync-java/issues/379)
    - **CartDiscount Sync** - Introduced the new `CartDiscountReferenceReplacementUtils#buildCartDiscountQuery` util to create a cart discount query with all needed reference expansions to fetch cart discounts from a source CTP project for the sync. [#379](https://github.com/commercetools/commercetools-sync-java/issues/379).
    
- 🐞 **Bug Fixes** (1)
    - **Commons** -  Fixed a bug in the `BaseSyncStatistics` which caused a wrong calculation of the `latestBatchProcessingTimeInMinutes`. [#378](https://github.com/commercetools/commercetools-sync-java/issues/378)

- 🛠️ **Enhancements** (6)
    - **CartDiscount Sync** - Added benchmarks for the `cartDiscount` sync to be able to compare the performance of the sync with the future releases. [#379](https://github.com/commercetools/commercetools-sync-java/issues/379)
    - **Commons** - Bumped commercetools-jvm-sdk to version [1.44.0](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_44_0).
    - **Commons** - Bumped gradle to version [gradle-5.5](https://docs.gradle.org/5.5/release-notes.html)
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-api` to 5.5.0.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-engine` to 5.5.0.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-params` to 5.5.0.

### 1.2.0 -  Jun 14, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.1.1...1.2.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.2.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.2.0)

- 🚧 **Breaking Changes** (2)
    - **ProductType Sync** - Removed the unneeded `AttributeDefinitionCustomBuilder` which was an exposed but internal helper. [#377](https://github.com/commercetools/commercetools-sync-java/issues/377). 
    - **Commons** - `SyncUtils#replaceReferenceIdWithKey` is now renamed to `SyncUtils#getReferenceWithKeyReplaced`. [#394](https://github.com/commercetools/commercetools-sync-java/issues/394)

- 🎉 **New Features** (2)
    - **Commons** - Added the new `CommonTypeUpdateActionUtils#buildUpdateActionForReferences` which is used for comapring references/resourceIdentifiers and buiding an update action if needed. [#394](https://github.com/commercetools/commercetools-sync-java/issues/394)
    - **Commons** - Added the new `SyncUtils#getResourceIdentifierWithKeyReplaced` util. [#394](https://github.com/commercetools/commercetools-sync-java/issues/394)
    
- 🐞 **Bug Fixes** (1)
    - **Commons** - Fixed a bug where references and resource identifiers were not being compared correctly. [#394](https://github.com/commercetools/commercetools-sync-java/issues/394)

- 🛠️ **Enhancements** (13)
    - **Commons** - Bumped commercetools-jvm-sdk to version [1.43.0](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_43_0).
    - **Commons** - Bumped `mockito` to 2.27.0.
    - **Commons** - Bumped `assertj` to 3.12.2.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-api` to 5.4.2.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-engine` to 5.4.2.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-params` to 5.4.2.
    - **Commons** - Bumped `org.ajoberstar.git-publish` to 2.1.1.
    - **Commons** - Bumped `org.ajoberstar.grgit` to 3.1.1.
    - **Commons** - Bumped `com.github.ben-manes.versions` to 0.21.0.
    - **Commons** - Bumped gradle checkstyle plugin to 8.2.
    - **Commons** - Bumped mockito dependency to 2.28.2.
    - **Commons** - Bumped JaCoCo dependency to 0.8.4.
    - **Commons** - Bumped gradle to version [gradle-5.4.1](https://docs.gradle.org/5.4.1/release-notes.html)
    - **Commons** - Bumped `com.adarshr.test-logger` to 1.7.0.


### 1.1.1 -  Jan 16, 2019
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.1.0...1.1.1) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.1.1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.1.1)

- 🐞 **Bug Fixes** (1)
    - **Product Sync** - Fixed a bug in the `product` sync which would fail on syncing attributes of type `Set` that has
    an empty set as a value. 


### 1.1.0 -  Dec 19, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/1.0.0...1.1.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.1.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.1.0)

- 🎉 **New Features** (4)
    - **Product Sync** - Added support for syncing assets of newly added variants. [#357](https://github.com/commercetools/commercetools-sync-java/issues/357).
    - **Product Sync** - `ProductSyncUtils#buildActions` and `ProductUpdateActionUtils#buildVariantsUpdateActions` now build `AddAsset` actions for every new asset on every new variant on the new `ProductDraft`. [#357](https://github.com/commercetools/commercetools-sync-java/issues/357).
    - **ProductType Sync** - Added support for syncing changes to an `AttributeDefinition` with a `SetType` of a subtype `LocalizableEnumType` or `EnumType` [#313](https://github.com/commercetools/commercetools-sync-java/issues/313)
    - **Type Sync** - Added support for syncing changes to a `FieldDefinition` with a `SetType` of a subtype `LocalizableEnumType` or `EnumType` [#313](https://github.com/commercetools/commercetools-sync-java/issues/313)

- 🐞 **Bug Fixes** (3)
    - **ProductType Sync** - Fixed a bug in the `productType` sync which would try to unset `isSearchable`, `inputHint` 
    and `attributeConstraint` values to `null` instead of their default values. [#354](https://github.com/commercetools/commercetools-sync-java/issues/354)
    - **ProductType Sync** - `ProductTypeSyncUtils#buildActions`, `ProductTypeUpdateActionUtils#buildAttributesUpdateActions`  
    now treat the values of the optional fields `isSearchable`, `inputHint` and `attributeConstraint` 
    as (`true`, `SingleLine` and `None` respectivley) if they are `null` or not passed. [#354](https://github.com/commercetools/commercetools-sync-java/issues/354)
    - **Commons** - Fixed a bug in the `beforeUpdateCallback` which caused the callback to be called even on an empty list of update actions. [#359](https://github.com/commercetools/commercetools-sync-java/issues/359)

- 🛠️ **Enhancements** (1)
    - **Commons** - Benchmarks are now run once on every merge to `master` with a lower number of resources for faster benchmarking. [#246](https://github.com/commercetools/commercetools-sync-java/issues/246)

- 📋 **Documentation** (2)
    - **Commons** - Added link to [documentation pages](https://commercetools.github.io/commercetools-sync-java) in README of the github repo.
    - **Commons** - Fixed link of [`beforeUpdateCallback` for keeping other variants](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/products/templates/beforeupdatecallback/KeepOtherVariantsSyncIT.java) example in the [Sync Options](/docs/usage/SYNC_OPTIONS.md) doc page. [#360](https://github.com/commercetools/commercetools-sync-java/issues/360)

### 1.0.0 -  Dec 10, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M14...1.0.0) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/1.0.0/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/1.0.0)

##### The Beta is Over 🎉

We're happy to announce that the commercetools-sync-java is finally out of beta! Big thanks to all the users 
who were using it when it was still in beta. Your feedback was definitely valuable for us to reach the current state of 
the library. `1.0.0` is here for you to use with all new features, enhancements and bug fixes including:

- The library now supports importing/syncing [`types`](https://docs.commercetools.com/http-api-projects-types.html) into a CTP project from an external feed or another CTP project. [Read more](https://commercetools.github.io/commercetools-sync-java/doc/usage/TYPE_SYNC/).
- The library now handles concurrency modification exceptions for the `productType` sync.
- All new documentation pages including a [quick start guide](https://commercetools.github.io/commercetools-sync-java/doc/usage/QUICK_START/).
- Many more improvements and bug fixes. 

##### Full Release Notes

- 🎉 **New Features** (4)
    - **Type Sync** - Added support for syncing types. [#300](https://github.com/commercetools/commercetools-sync-java/issues/300) For more info how to use it please refer to [Type usage doc](/docs/usage/TYPE_SYNC.md).
    - **Type Sync** - Exposed `TypeSyncUtils#buildActions` which calculates all needed update actions after comparing a `Type` and a `TypeDraft`. [#300](https://github.com/commercetools/commercetools-sync-java/issues/300)
    - **Type Sync** - Exposed `TypeUpdateActionUtils` which contains utils for calculating needed update actions after comparing individual fields of a `Type` and a `TypeDraft`. [#300](https://github.com/commercetools/commercetools-sync-java/issues/300)
    - **Commons** - Added `OptionalUtils#filterEmptyOptionals` which are utility methods that filter out the empty optionals in a supplied list (with a varargs variation) returning a list of the contents of the non-empty 
    optionals. [#255](https://github.com/commercetools/commercetools-sync-java/issues/255)

- 🛠️ **Enhancements** (17)
    - **ProductType Sync** - Added concurrency modification exception handling. [#325](https://github.com/commercetools/commercetools-sync-java/issues/325)
    - **Commons** - `ProductSyncUtils#buildActions`, `CategorySyncUtils#buildActions`, `InventorySyncUtils#buildActions` and `ProductTypeSyncUtils#buildActions` now don't apply the `beforeUpdateCallback` implicitly. 
    If you want, you can apply it explicitly on the result of the `..#buildActions` method. [#302](https://github.com/commercetools/commercetools-sync-java/issues/302)
    - **Product Sync** - Reference keys are not validated if they are in UUID format anymore. [#166](https://github.com/commercetools/commercetools-sync-java/issues/166)
    - **Category Sync** - Reference keys are not validated if they are in UUID format anymore. [#166](https://github.com/commercetools/commercetools-sync-java/issues/166)
    - **Inventory Sync** - Reference keys are not validated if they are in UUID format anymore. [#166](https://github.com/commercetools/commercetools-sync-java/issues/166)
    - **ProductType Sync** - Added benchmarks for the `productType` sync to be able to compare the performance of the sync with the future releases. [#301](https://github.com/commercetools/commercetools-sync-java/issues/301)
    - **Commons** - Bumped commercetools-jvm-sdk to version [1.37.0](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_37_0).
    - **Commons** - Bumped `mockito` to 2.23.4.
    - **Commons** - Bumped `com.adarshr.test-logger` to 1.6.0.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-api` to 5.3.2.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-engine` to 5.3.2.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-params` to 5.3.2.
    - **Commons** - Bumped `org.ajoberstar.git-publish` to 2.0.0.
    - **Commons** - Bumped `org.ajoberstar.grgit` to 3.0.0.
    - **Commons** - Bumped gradle to version [gradle-5.0](https://docs.gradle.org/5.0/release-notes.html)
    - **Type Sync** - Added benchmarks for the `type` sync to be able to compare the performance of the sync with the future releases. [#300](https://github.com/commercetools/commercetools-sync-java/issues/300)
    
- 🚧 **Breaking Changes** (9) 
    - **Product Sync** - `allowUuid` option is now removed. [#166](https://github.com/commercetools/commercetools-sync-java/issues/166) 
    - **Category Sync** - `allowUuid` option is now removed. [#166](https://github.com/commercetools/commercetools-sync-java/issues/166) 
    - **Inventory Sync** - `allowUuid` option is now removed. [#166](https://github.com/commercetools/commercetools-sync-java/issues/166) 
    - **ProductType Sync** - `allowUuid` option is now removed. [#166](https://github.com/commercetools/commercetools-sync-java/issues/166) 
    - **ProductType Sync** - Renamed `ProductTypeUpdateAttributeDefinitionActionUtils` to `AttributeDefinitionsUpdateActionUtils`. It is also now meant to be **only used internally** by the library. 
    Its  behaviour is not guaranteed if used externally. [#302](https://github.com/commercetools/commercetools-sync-java/issues/302)
    - **ProductType Sync** - `AttributeDefinitionUpdateActionUtils` is now meant to be **only used internally** by the library. 
    Its  behaviour is not guaranteed if used externally. [#302](https://github.com/commercetools/commercetools-sync-java/issues/302)
    - **ProductType Sync** - `EnumsUpdateActionUtils` is now `EnumValuesUpdateActionUtils` and is meant to be **only used internally** by the library. 
    Its  behaviour is not guaranteed if used externally. [#300](https://github.com/commercetools/commercetools-sync-java/issues/300)
    - **ProductType Sync** - Utils that were in `ProductTypeUpdateLocalizedEnumActionUtils` and `LocalizedEnumsUpdateActionUtils.` are moved to `LocalizedEnumValueUpdateActionUtils`. [#300](https://github.com/commercetools/commercetools-sync-java/issues/300)
    - **ProductType Sync** - Utils that were in `ProductTypeUpdatePlainEnumActionUtils` and `PlainEnumUpdateActionsUtils.` are moved to `PlainEnumValueUpdateActionUtils`. [#300](https://github.com/commercetools/commercetools-sync-java/issues/300)

- 🐞 **Bug Fixes** (3)
    - **Product Sync** - Fixed a bug that caused the statistics not to be updated correctly on fetch failure. [#331](https://github.com/commercetools/commercetools-sync-java/issues/331)
    - **Category Sync** - Fixed a bug that caused the statistics not to be updated correctly on fetch failure. [#331](https://github.com/commercetools/commercetools-sync-java/issues/331)
    - **ProductType Sync** - Fixed a bug that caused the sync process to continue after failed fetch. [#331](https://github.com/commercetools/commercetools-sync-java/issues/331)
    
- 📋 **Documentation** (4)
    - **Commons** - Added the documentation github pages. https://commercetools.github.io/commercetools-sync-java 
    - **Commons** - Added a [Quick Start Guide](/docs/usage/QUICK_START.md) for a convenient entry into the library.
    - **Commons** - Moved documentation of sync options to a separate [doc](/docs/usage/SYNC_OPTIONS.md).
    - **Commons** - Added a the earliest compatible version of the commercetools-jvm-sdk](https://github.com/commercetools/commercetools-jvm-sdk) as a prerequisite for using the library.

### v1.0.0-M14 -  Oct 5, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M13...v1.0.0-M14) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M14/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M14)

- 🐞 **Bug Fixes** (1)
    - **Product Sync** - Fixed a bug where the removed attributes in the source product variant draft were not being removed from the target variant. [#238](https://github.com/commercetools/commercetools-sync-java/issues/308)

- 🛠 **Enhancements** (8)
    - **Product Sync** - Products create and update requests are now issued in parallel. This should lead to a performance improvement. [#238](https://github.com/commercetools/commercetools-sync-java/issues/238)
    - **Commons** - Bumped `com.adarshr.test-logger` to 1.5.0.
    - **Commons** - Bumped `mockito` to 2.22.0.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-api` to 5.3.1.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-engine` to 5.3.1.
    - **Commons** - Bumped `org.junit.jupiter:junit-jupiter-params` to 5.3.1.
    - **Commons** - `UnorderedCollectionSyncUtils#buildRemoveUpdateActions ensures no `null` elements in the resulting list and ignores `null` keys now. [#238](https://github.com/commercetools/commercetools-sync-java/issues/308)
    - **Commons** - Bumped gradle to version [gradle-4.10.2](https://docs.gradle.org/4.10.2/release-notes.html).

- 🚧 **Breaking Changes** (2)
    - **Product Sync** - `AttributeMetaData#isRequired` is now removed. [#308](https://github.com/commercetools/commercetools-sync-java/issues/308)
    - **Product Sync** - `ProductVariantAttributeUpdateActionUtils#buildProductVariantAttributeUpdateAction` now takes a map of all meta data instead of the specific metadata entry. [#308](https://github.com/commercetools/commercetools-sync-java/issues/308)


### v1.0.0-M13 -  Sept 5, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M12...v1.0.0-M13) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M13/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M13)

- 🎉 **New Features** (15)
    - **ProductType Sync** - Support for syncing productTypes. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286) For more info how to use it please refer to [ProductType usage doc](/docs/usage/PRODUCT_TYPE_SYNC.md). 
    - **Product Sync** - Support for syncing product prices. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101)
    - **Product Sync** - `ProductSyncUtils#buildActions` now also calculates variants' all price update actions needed. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101)
    - **Product Sync** - `ProductUpdateActionUtils#buildVariantsUpdateActions` now also calculates variants' all price update actions needed. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101)
    - **Product Sync** - Introduced new update action build utility for building all needed update actions between two variants' prices `ProductVariantUpdateActionUtils#buildProductVariantPricesUpdateActions`. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101)
    - **ProductSync** - `PriceReferenceResolver` now resolves prices' CustomerGroup references on prices. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101)
    - **InventoryEntry Sync** - `InventoryReferenceReplacementUtils#replaceInventoriesReferenceIdsWithKeys` now supports replacing channel reference ids with keys. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101)
    - **ProductType Sync** - Exposed `ProductTypeSyncUtils#buildActions` which calculates all needed update actions after comparing a `ProductType` and a `ProductTypeDraft`. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)
    - **ProductType Sync** - Exposed `ProductTypeUpdateActionUtils` which contains utils for calculating needed update actions after comparing individual fields of a `ProductType` and a `ProductTypeDraft`. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)
    - **ProductType Sync** - Exposed `ProductTypeUpdateAttributeDefinitionActionUtils` which contains utils for calculating needed update actions after comparing a list of `AttributeDefinition`s and a list of `AttributeDefinitionDraft`s. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)
    - **ProductType Sync** - Exposed `ProductTypeUpdateLocalizedEnumActionUtils` which contains utils for calculating needed update actions after comparing two lists of `LocalizedEnumValue`s. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)
    - **ProductType Sync** - Exposed `ProductTypeUpdatePlainEnumActionUtils` which contains utils for calculating needed update actions after comparing two lists of `EnumValue`s. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)
    - **ProductType Sync** - Exposed `AttributeDefinitionUpdateActionUtils` which contains utils for calculating needed update actions after comparing an `AttributeDefinition` and an `AttributeDefinitionDraft`. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)
    - **ProductType Sync** - Exposed `LocalizedEnumUpdateActionsUtils` which contains utils for calculating needed update actions after comparing two `LocalizedEnumValue`s. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)
    - **ProductType Sync** - Exposed `PlainEnumUpdateActionsUtils` which contains utils for calculating needed update actions after comparing two `EnumValue`s. [#286](https://github.com/commercetools/commercetools-sync-java/issues/286)

-  🛠️ **Enhancements** (7)
    - **Commons** - Bumped gradle to version [gradle-4.10](https://docs.gradle.org/4.10/release-notes.html).
    - **Commons** - Bumped `com.jfrog.bintray` to 1.8.4.
    - **Commons** - Bumped `assertj` to 3.11.1.
    - **Commons** - Bumped `mockito` to 2.21.0.
    - **Commons** - Bumped `org.ajoberstar.grgit` to 2.3.0.
    - **Commons** - Bumped `com.adarshr.test-logger` to 1.4.0.
    - **Commons** - Switched to Junit5 using both `junit-jupiter-engine` and `junit-vintage-engine` for backward compatibility.

-  🛠️ **Breaking Changes** (3)
    - **Product Sync** - Removed redundant `ProductUpdateActionUtils#buildRemoveVariantUpdateActions`. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - Moved `SyncUtils#replaceCustomTypeIdWithKeys` to `CustomTypeReferenceReplacementUtils#replaceCustomTypeIdWithKeys`. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101).
    - **Commons** - Moved `SyncUtils#replaceAssetsReferencesIdsWithKeys` to `AssetReferenceReplacementUtils#replaceAssetsReferencesIdsWithKeys`. [#101](https://github.com/commercetools/commercetools-sync-java/issues/101).


### v1.0.0-M12 -  Jun 05, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M11...v1.0.0-M12) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M12/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M12)

- 🛠️ **Enhancements** (13)
    - **Product Sync** - Support for syncing price custom fields. [#277](https://github.com/commercetools/commercetools-sync-java/issues/277)
    - **Product Sync** - `VariantReferenceResolver` now resolves prices' custom type references on all variants. [#277](https://github.com/commercetools/commercetools-sync-java/issues/277)
    - **Product Sync** - `ProductReferenceReplacementUtils#buildProductQuery` now expands custom types on prices. [#277](https://github.com/commercetools/commercetools-sync-java/issues/277)
    - **Product Sync** - `VariantReferenceReplacementUtils#replacePricesReferencesIdsWithKeys` now supports replacing price custom reference ids with keys. [#277](https://github.com/commercetools/commercetools-sync-java/issues/277)
    - **Commons** - Bumped commercetools-jvm-sdk to version [1.32.0](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_32_0).
    - **Commons** - Bumped gradle to version [gradle-4.8](https://docs.gradle.org/4.8/release-notes.html).
    - **Commons** - Bumped `com.jfrog.bintray` to 1.8.0.
    - **Commons** - Bumped `org.ajoberstar.git-publish` to 1.0.0.
    - **Commons** - Bumped `com.adarshr.test-logger` to 1.2.0.
    - **Commons** - Bumped `org.ajoberstar.grgit` to 2.2.1.
    - **Commons** - Bumped gradle checkstyle plugin to 8.10.1.
    - **Commons** - Bumped mockito dependency to 2.18.3.
    - **Commons** - Bumped JaCoCo dependency to 0.8.1.


### v1.0.0-M11 -  Mar 08, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M10...v1.0.0-M11) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M11/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M11)

- 🎉 **New Features** (19)
    - **Category Sync** - Support of categories' asset syncing. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - Support of product variants' asset syncing. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Category Sync** - `CategorySyncUtils#buildActions` now also calculates all asset update actions needed. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - `ProductSyncUtils#buildActions` now also calculates variants' all asset update actions needed. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - `ProductUpdateActionUtils#buildVariantsUpdateActions` now also calculates variants' all asset update actions needed. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - Introduced the new ActionGroup: `ASSETS` which can be used in blacklisting/whitelisting assets syncing during the product sync. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Category Sync** - Introduced new update action build utility for building all needed update actions between two categories' assets `ProductVariantUpdateActionUtils#buildProductVariantAssetsUpdateActions`. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - Introduced new update action build utility for building all needed update actions between two variants' assets `ProductVariantUpdateActionUtils#buildProductVariantAssetsUpdateActions`. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Category Sync** - Introduced new update action granular build utils for category asset fields in `CategoryAssetUpdateActionUtils`. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - Introduced new update action granular build utils for product variant assets fields in `ProductVariantAssetUpdateActionUtils`. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - Introduced `AssetReferenceResolver` which is a helper that can resolve all the references of an AssetDraft. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - `VariantReferenceResolver` and `CategoryReferenceResolver` now also resolve all the containing AssetDrafts references. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - Support for custom update actions calculation for secondary resources (e.g. prices, product assets and category assets). [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - `ProductReferenceReplacementUtils#replaceProductsReferenceIdsWithKeys` and `VariantReferenceReplacementUtils#replaceVariantsReferenceIdsWithKeys` now support replacing asset custom reference ids with keys. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Category Sync** - `CategoryReferenceReplacementUtils#replaceCategoriesReferenceIdsWithKeys` now supports replacing asset custom reference ids with keys. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - Introduced new `SyncUtils#replaceAssetsReferenceIdsWithKeys` which is a util that replaces the custom type ids with keys in a list of assets. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - `ProductReferenceReplacementUtils#buildProductQuery` now expands custom types on assets. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Category Sync** - `CategoryReferenceReplacementUtils#buildCategoryQuery` now expands custom types on assets. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - Introduced new `ResourceIdentifierUtils#toResourceIdentifierIfNotNull`. [#262](https://github.com/commercetools/commercetools-sync-java/issues/262) 

- **Changes** (5)
    - **Commons** - `CustomUpdateActionUtils#buildCustomUpdateActions` is now 
    `CustomUpdateActionUtils#buildPrimaryResourceCustomUpdateActions`. It now takes a new third parameter `customActionBuilder` 
    which represents the concrete builder of custom update actions. For a list of concrete builder options check the 
    implementors of the `GenericCustomActionBuilder` interface. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - `CustomUpdateActionUtils#buildCustomUpdateActions` can now be used to build custom update actions
    for secondary resources (e.g. assets and prices). [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Commons** - New Custom Type Id is now validated against being empty/null. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Product Sync** - `ProductSyncUtils#buildCoreActions` is now removed. `ProductSyncUtils#buildActions` should be used instead. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)
    - **Category Sync** - `CategorySyncUtils#buildCoreActions` is now removed. `CategorySyncUtils#buildActions` should be used instead. [#3](https://github.com/commercetools/commercetools-sync-java/issues/3)

- 🛠️ **Enhancements** (1)
    - **Build Tools** - Bumped commercetools-jvm-sdk to version [1.30.0](http://commercetools.github.io/commercetools-jvm-sdk/apidocs/io/sphere/sdk/meta/ReleaseNotes.html#v1_30_0). [#262](https://github.com/commercetools/commercetools-sync-java/issues/262)

- 🐞 **Bug Fixes** (1)
    - **Build Tools** - Fixed bug where jar and Codecov were triggered on benchmark stages of the build when they should 
only be triggered on the full build. [#249](https://github.com/commercetools/commercetools-sync-java/issues/249)


### v1.0.0-M10 -  Feb 13, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M9...v1.0.0-M10) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M10/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M10)

- 🎉 **New Features** (1)
    - **Commons** - Added [benchmarking setup](/docs/BENCHMARKS.md) for the library on every release. [#155](https://github.com/commercetools/commercetools-sync-java/issues/155)

- **Changes** (3)
    - **Commons** - Statistics counters are now of type `AtomicInteger` instead of int to support concurrency. [#242](https://github.com/commercetools/commercetools-sync-java/issues/242)
    - **Category Sync** - `categoryKeysWithMissingParents` in the `CategorySyncStatistics` is now of type `ConcurrentHashMap<String, Set<String>` instead of `Map<String, List<String>`. [#242](https://github.com/commercetools/commercetools-sync-java/issues/242)
    - **Category Sync** - `CategorySyncStatistics` now exposes the methods `removeChildCategoryKeyFromMissingParentsMap`, `getMissingParentKey` and `putMissingParentCategoryChildKey` to support manipulating `categoryKeysWithMissingParents` map. [#242](https://github.com/commercetools/commercetools-sync-java/issues/242)

### v1.0.0-M9 -  Jan 22, 2018
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M8...v1.0.0-M9) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M9/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M9)

- 🎉 **New Features** (1)
    - **Commons** - Added `getSyncOptions` to the `ProductSync`, `CategorySync` and `InventorySync`. [#230](https://github.com/commercetools/commercetools-sync-java/issues/230)

- **Changes** (1)
    - **Product Sync** - Added validation for product drafts' SKUs as a required field on the input product drafts since SKUs will be used for product matching in the future. [#230](https://github.com/commercetools/commercetools-sync-java/issues/230)

- 🛠️ **Enhancements** (1)
    - **Product Sync** - Changed the product sync to cache product ids per batch as opposed to caching the entire products ids before syncing products. [#230](https://github.com/commercetools/commercetools-sync-java/issues/230) 

- 🐞 **Bug Fixes** (1)
    - **Commons** - Fixed library version in User-Agent headers of JVM SDK clients using the library. Now it is not fetched
     from the JAR manifest but injected by gradle-scripts/set-release-version.gradle. [#227](https://github.com/commercetools/commercetools-sync-java/issues/227)


### v1.0.0-M8 -  Dec 29, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M7...v1.0.0-M8) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M8/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M8)

- 🎉 **New Features** (1)
    - **Category Sync** - Exposed new method `CategorySyncStatistics#getNumberOfCategoriesWithMissingParents` which gets the
    total number of categories with missing parents from the statistics instance. [#186](https://github.com/commercetools/commercetools-sync-java/issues/186)

- **Changes** (2)
    - **Product Sync** - Changed product sync statistics report message wording. [#186](https://github.com/commercetools/commercetools-sync-java/issues/186)
    - **Product Sync** - Exposed new methods `ProductReferenceResolver#resolveStateReference`, `ProductReferenceResolver#resolveTaxCategoryReference`, `ProductReferenceResolver#resolveCategoryReferences` and `ProductReferenceResolver#resolveProductTypeReference`.
    [#218](https://github.com/commercetools/commercetools-sync-java/issues/218)

- 🛠 **Enhancements** (1) 
    - **Build Tools** - Bumped Gradle to version 4.4. [#205](https://github.com/commercetools/commercetools-sync-java/issues/205)


### v1.0.0-M7 -  Dec 15, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M6...v1.0.0-M7) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M7/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M7)

- 🐞 **Bug Fixes** (1)
    - **Commons** - Changed offset-based pagination of querying all elements to a limit-based with sorted ids approach 
    to mitigate problems of previous approach. [#210](https://github.com/commercetools/commercetools-sync-java/issues/210)


### v1.0.0-M6 -  Dec 5, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M5...v1.0.0-M6) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M6/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M6)

-  🎉 **New Features** (3)
    - **Category Sync** - Introduced `beforeCreateCallback` option which is callback applied on a category draft before a request to create it on CTP is issued. [#183](https://github.com/commercetools/commercetools-sync-java/issues/183)
    - **Product Sync** - Introduced `beforeCreateCallback` option which is callback applied on a product draft before a request to create it on CTP is issued. [#183](https://github.com/commercetools/commercetools-sync-java/issues/183)
    - **Inventory Sync** - Introduced `beforeCreateCallback` option which is callback applied on a inventoryEntry draft before a request to create it on CTP is issued. [#183](https://github.com/commercetools/commercetools-sync-java/issues/183)

- ✨ **Major Enhancements** (2)
    - **Category Sync** - Introduced batching on update action requests to allow for requesting updates of more than 500 actions. [#21](https://github.com/commercetools/commercetools-sync-java/issues/21)
    - **Product Sync** - Introduced batching on update action requests to allow for requesting updates of more than 500 actions. [#21](https://github.com/commercetools/commercetools-sync-java/issues/21)

- 🐞 **Bug Fixes** (1)
    - **Commons** - Fixed library version in User-Agent headers of JVM SDK clients using the library. [#191](https://github.com/commercetools/commercetools-sync-java/issues/191)

- 📋 **Documentation** (1)
    - **Commons** - Added [Code of Conduct](/docs/CODE_OF_CONDUCT.md) doc.

- 🚧 **Migration guide** (6)
    - **Product Sync** - Removed `removeOtherVariants` option which is already done by the sync by default. Removal of 
    variants can be prevented through the beforeUpdateCallback. Please check [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/products/templates/beforeupdatecallback/KeepOtherVariantsSyncIT.java)
    an example of how this can be done. [#26](https://github.com/commercetools/commercetools-sync-java/issues/26)
    - **Commons** - Removed `removeOtherSetEntries`, `removeOtherCollectionEntries` and `removeOtherProperties` options 
    which are already done by the sync by default. The aforementioned options (and even more use cases) can now be covered with help of the beforeCreateCallback and beforeUpdateCallback. Please 
    check [here](https://github.com/commercetools/commercetools-sync-java/tree/master/src/integration-test/java/com/commercetools/sync/integration/ctpprojectsource/products/templates/beforeupdatecallback/KeepOtherVariantsSyncIT.java) 
    an example of how removal of variants can be disabled. [#26](https://github.com/commercetools/commercetools-sync-java/issues/26)
    - **Commons** - Removed website and emergency contact e-mail appened in User-Agent headers of JVM SDK clients using the 
    library. [#191](https://github.com/commercetools/commercetools-sync-java/issues/191)
    - **Category Sync** - `beforeUpdateCallback` now treats a null return as an empty list of update actions. [#183](https://github.com/commercetools/commercetools-sync-java/issues/183)
    - **Product Sync** - `beforeUpdateCallback` now treats a null return as an empty list of update actions. [#183](https://github.com/commercetools/commercetools-sync-java/issues/183)
    - **Inventory Sync** - `beforeUpdateCallback` now treats a null return as an empty list of update actions. [#183](https://github.com/commercetools/commercetools-sync-java/issues/183)


### v1.0.0-M5 -  Nov 16, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M4...v1.0.0-M5) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M5/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M5)

- 🎉 **New Features** (3)
    - **Inventory Sync** - Introduced `beforeUpdateCallback` which is applied after generation of update actions and before 
    actual InventoryEntry update. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Build Tools** - Added `Add Release Notes entry` checkbox in PR template on Github repo. [#161](https://github.com/commercetools/commercetools-sync-java/issues/161)
    - **Commons** - Appended library name and version to User-Agent headers of JVM SDK clients using the library. [#142](https://github.com/commercetools/commercetools-sync-java/issues/142)

- 🛠️ **Enhancements** (3)
    - **Commons** - `setUpdateActionsCallback` has been renamed to `beforeUpdateCallback` and now takes a TriFunction instead 
    of Function, which adds more information about the generated list of update actions, namely, the old resource being 
    updated and the new resource draft. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Build Tools** - Explicitly specified gradle tasks execution order in execution-order.gradle. [#161](https://github.com/commercetools/commercetools-sync-java/issues/161)
    - **Build Tools** - Set PMD to run before Integration tests. [#161](https://github.com/commercetools/commercetools-sync-java/issues/161)
    - **Commons** - Appended library name and version to User-Agent headers of JVM SDK clients using the library. [#142](https://github.com/commercetools/commercetools-sync-java/issues/142)

- 📋 **Documentation** (1)
    - **Build Tools** - Added Snyk vulnerabilities badge to repo README. [#188](https://github.com/commercetools/commercetools-sync-java/pull/188)

- 🚧 **Migration guide** (8)
    - **Commons** - Renamed `setUpdateActionsCallback` to `beforeUpdateCallback`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Commons** - Renamed `setAllowUuid` to `allowUuid`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Commons** - Renamed `setWarningCallBack` to `warningCallback`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Commons** - Renamed `setErrorCallBack` to `errorCallback`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Commons** - Renamed `setBatchSize` to `batchSize`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Commons** - Removed `setRemoveOtherLocales` option. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Commons** - Renamed `setRemoveOtherSetEntries`, `setRemoveOtherCollectionEntries` and `setRemoveOtherProperties` 
    to `removeOtherSetEntries`, `removeOtherCollectionEntries` and `removeOtherProperties`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)
    - **Product Sync** - Renamed `setSyncFilter` to `syncFilter`. [#169](https://github.com/commercetools/commercetools-sync-java/issues/169)

### v1.0.0-M4 -  Nov 7, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M3...v1.0.0-M4) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M4/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M4)

- 🔥 **Hotfix** (1)
    - **Product Sync** - Fixed an issue with `replaceAttributesReferencesIdsWithKeys` which nullifies localized text attributes due 
to JSON parsing not throwing exception on parsing it to reference set. [#179](https://github.com/commercetools/commercetools-sync-java/issues/179)


### v1.0.0-M3 -  Nov 3, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M2...v1.0.0-M3) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M3/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M3)

- 🎉 **New Features** (7)
    - **ProductSync** - Introduced Product TaxCategory reference resolution and syncing. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
    - **ProductSync** - Introduced Product State reference resolution and syncing. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
    - **ProductSync** - Exposed `ProductReferenceReplacementUtils#buildProductQuery` util to create a product query with all needed reference expansions to fetch products from a source CTP project for the sync. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
    - **ProductSync** - Exposed `VariantReferenceReplacementUtils#replaceVariantsReferenceIdsWithKeys` which provides utils to replace reference ids with keys on variants (price and attriute references) coming from a source CTP project to make it ready for reference resolution. [#160](https://github.com/commercetools/commercetools-sync-java/issues/160).
    - **ProductSync** - Exposed `VariantReferenceResolver` which is a helper that resolves the price and attribute references on a ProductVariantDraft. (Note: This is used now by the already existing ProductReferenceResolver) [#160](https://github.com/commercetools/commercetools-sync-java/issues/160).
    - **CategorySync** - Exposed `CategoryReferenceReplacementUtils#buildCategoryQuery` util to create a category query with all needed reference expansions to fetch categories from a source CTP project for the sync. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).
    - **Commons** - Exposed `replaceCustomTypeIdWithKeys` and `replaceReferenceIdWithKey`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120).

- 🐞 **Bug Fixes** (1) 
    - **Category Sync** - Fixes an issue where retrying on concurrent modification exception wasn't re-fetching the latest 
    Category and rebuilding build update actions. [#94](https://github.com/commercetools/commercetools-sync-java/issues/94)

 - 📋 **Documentation** (6)
    - **Product Sync** - Documented the reason behind having the latest batch processing time. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
    - **Category Sync** - Documented the reason behind having the latest batch processing time. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
    - **Category Sync** - Fixed the statistics summary string used in the documentation. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
    - **Inventory Sync** - Documented the reason behind having the latest batch processing time. [#119](https://github.com/commercetools/commercetools-sync-java/issues/119)
    - **Product Sync** - Fixed some typos. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)
    - **Commons** - Provided inline example of how to use logging in callbacks. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)

- 🚧 **Migration guide** (9)
    - **Product Sync** - Moved `replaceProductsReferenceIdsWithKeys` from `SyncUtils` to `ProductReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
    - **Product Sync** - Removed `replaceProductDraftsCategoryReferenceIdsWithKeys` which is not needed anymore. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
    - **Product Sync** - Removed `replaceProductDraftCategoryReferenceIdsWithKeys` which is not needed anymore. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
    - **Product Sync** - Removed `replaceCategoryOrderHintCategoryIdsWithKeys` which is not needed anymore. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
    - **Product Sync** - Moved `getDraftBuilderFromStagedProduct` from `SyncUtils` to `ProductReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
    - **Category Sync** - Moved `replaceCategoriesReferenceIdsWithKeys` from `SyncUtils` to `CategoryReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
    - **Inventory Sync** - Moved `replaceInventoriesReferenceIdsWithKeys` from `SyncUtils` to `InventoryReferenceReplacementUtils`. [#120](https://github.com/commercetools/commercetools-sync-java/issues/120)
    - **Commons** - Removed slf4j-simple dependency. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)
    - **Commons** - Used implementation instead of compile configuration for dependencies. [#172](https://github.com/commercetools/commercetools-sync-java/issues/172)


### v1.0.0-M2 -  Oct 12, 2017 

[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M2-beta...v1.0.0-M2) |
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M2/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M2)

- 🎉 **New Features** (3)
    - **Product Sync** - Supported syncing entire product variant images, putting order into consideration. [#114](https://github.com/commercetools/commercetools-sync-java/issues/114)
    - **Product Sync** - Exposed `ProductVariantUpdateActionUtils#buildProductVariantImagesUpdateActions` and `ProductVariantUpdateActionUtils#buildMoveImageToPositionUpdateActions` action build util. [#114](https://github.com/commercetools/commercetools-sync-java/issues/114)
    - **Product Sync** - Supported Blacklisting/Whitelisting update action groups on sync. [#122](https://github.com/commercetools/commercetools-sync-java/issues/122)

- 🐞 **Bug Fixes** (4)
    - **Build Tools** - Fixed issue were JavaDoc jar was not built. [#117](https://github.com/commercetools/commercetools-sync-java/issues/117)
    - **Build Tools** - Fixed issue were JavaDoc was not published on github. [#118](https://github.com/commercetools/commercetools-sync-java/issues/118)
    - **Product Sync** - Fixed a potential bug where an exisitng master variant key could be blank.[#122](https://github.com/commercetools/commercetools-sync-java/issues/122)
    - **Product Sync** - Fixed a potential bug where a product draft could be provided with no master variant set. [#122](https://github.com/commercetools/commercetools-sync-java/issues/122)

- 🛠 **Enhancements** (2)️
    - **Build Tools** - Integration tests project credentials can now be set on a properties file not only as environment variables and give error messages if not set. [#105](https://github.com/commercetools/commercetools-sync-java/issues/105)
    - **Product Sync** - Validated the SKU before making a `ChangeMasterVariant` request by SKU. [#122](https://github.com/commercetools/commercetools-sync-java/issues/122)

 - 📋 **Documentation** (5)
     - **Build Tools** - Added bintray badge to repo. [#126](https://github.com/commercetools/commercetools-sync-java/issues/126)
     - **Product Sync** - Added usage documentation. [#121](https://github.com/commercetools/commercetools-sync-java/issues/121)
     - **Commons** - Separated contributing README into own README not in the main one. [#121](https://github.com/commercetools/commercetools-sync-java/issues/121)
     - **Commons** - Added release notes doc. [#125](https://github.com/commercetools/commercetools-sync-java/issues/125)
     - **Build Tools** - Added JavaDoc badge to repo. [#145](https://github.com/commercetools/commercetools-sync-java/issues/145)

### v1.0.0-M2-beta -  Sep 28, 2017 
[Commits](https://github.com/commercetools/commercetools-sync-java/compare/v1.0.0-M1...v1.0.0-M2-beta) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M2-beta)


- **Beta Features** (11)
    - **Product Sync** - Introduced syncing products name, categories, categoryOrderHints, description, slug,  metaTitle, 
    metaDescription, metaKeywords, masterVariant and searchKeywords. [#57](https://github.com/commercetools/commercetools-sync-java/issues/57)
    - **Product Sync** -  Exposed update action build utils for products name, categories, categoryOrderHints, description, slug,  metaTitle, 
    metaDescription, metaKeywords, masterVariant and searchKeywords. [#57](https://github.com/commercetools/commercetools-sync-java/issues/57)
    - **Product Sync** -  Introduced reference resolution support for product categories, productType and prices. [#95](https://github.com/commercetools/commercetools-sync-java/issues/95)
    [#96](https://github.com/commercetools/commercetools-sync-java/issues/96)
    - **Product Sync** -  Introduced syncing products publish state. [#97](https://github.com/commercetools/commercetools-sync-java/issues/97)
    - **Product Sync** -  Exposed update action build utils for products publish state. [#97](https://github.com/commercetools/commercetools-sync-java/issues/97)
    - **Product Sync** -  Introduced syncing products variant attributes. [#98](https://github.com/commercetools/commercetools-sync-java/issues/98)
    - **Product Sync** -  Exposed update action build utils for products variant attributes. [#98](https://github.com/commercetools/commercetools-sync-java/issues/98)
    - **Product Sync** -  Introduced syncing products variant prices without update action calculation. [#99](https://github.com/commercetools/commercetools-sync-java/issues/99)
    - **Product Sync** -  Introduced syncing products variant images. [#100](https://github.com/commercetools/commercetools-sync-java/issues/100)
    - **Product Sync** -  Exposed update action build utils for products variant images. [#100](https://github.com/commercetools/commercetools-sync-java/issues/100)
    - **Product Sync** -  Introduced syncing products against staged projection. [#93](https://github.com/commercetools/commercetools-sync-java/issues/93)


### v1.0.0-M1 -  Sep 06, 2017
[Commits](https://github.com/commercetools/commercetools-sync-java/commits/v1.0.0-M1) | 
[Javadoc](https://commercetools.github.io/commercetools-sync-java/v/v1.0.0-M1/) | 
[Jar](https://bintray.com/commercetools/maven/commercetools-sync-java/v1.0.0-M1)

- 🎉 **New Features** (16) 
    - **Category Sync** - Introduced syncing category name, description, orderHint, metaDescription, metaTitle, 
    customFields and parent category. [#2](https://github.com/commercetools/commercetools-sync-java/issues/2)
    - **Category Sync** - Exposed update action build utils for category name, description, orderHint, metaDescription, metaTitle, 
    customFields and parent category. [#2](https://github.com/commercetools/commercetools-sync-java/issues/2)
    - **Category Sync** - Introduced sync options builders. [#5](https://github.com/commercetools/commercetools-sync-java/issues/5)
    - **Category Sync** - Introduced support of syncing categories in any order. [#28](https://github.com/commercetools/commercetools-sync-java/issues/28)
    - **Category Sync** - Added concurrency modification exception repeater. [#30](https://github.com/commercetools/commercetools-sync-java/issues/30)
    - **Category Sync** - Used category keys for matching. [#45](https://github.com/commercetools/commercetools-sync-java/issues/45)
    - **Category Sync** - Introduced reference resolution support. [#47](https://github.com/commercetools/commercetools-sync-java/issues/47)
    - **Category Sync** - Introduced Batch processing support. [#73](https://github.com/commercetools/commercetools-sync-java/issues/73)
    - **Category Sync** - Added info about missing parent categories in statistics. [#73](https://github.com/commercetools/commercetools-sync-java/issues/76)
    - **Commons** - Introduced sync statistics support. [#6](https://github.com/commercetools/commercetools-sync-java/issues/6)
    - **Commons** - Sync ITs should now use client that repeats on 5xx errors. [#31](https://github.com/commercetools/commercetools-sync-java/issues/31)
    - **Commons** - Sync only accepts drafts. [#46](https://github.com/commercetools/commercetools-sync-java/issues/46)
    - **Build Tools** - Travis setup as CI tool. [#1](https://github.com/commercetools/commercetools-sync-java/issues/1)
    - **Build Tools** - Setup Bintray release and publising process. [#24](https://github.com/commercetools/commercetools-sync-java/issues/24)
    - **Build Tools** - Setup CheckStyle, PMD, FindBugs, Jacoco and CodeCov. [#25](https://github.com/commercetools/commercetools-sync-java/issues/25)
    - **Build Tools** - Setup repo PR and issue templates. [#29](https://github.com/commercetools/commercetools-sync-java/issues/29)

- **Beta Features** (5)
    - **Inventory Sync** - Introduced syncing inventory supplyChannel, quantityOnStock, restockableInDays, expectedDelivery 
    and customFields. [#17](https://github.com/commercetools/commercetools-sync-java/issues/17)
    - **Inventory Sync** - Exposed update action build utils for inventory supplyChannel, quantityOnStock, restockableInDays, expectedDelivery 
    and customFields. [#17](https://github.com/commercetools/commercetools-sync-java/issues/17)
    - **Inventory Sync** - Introduced sync options builder support. [#15](https://github.com/commercetools/commercetools-sync-java/issues/15)
    - **Inventory Sync** - Introduced reference resolution support. [#47](https://github.com/commercetools/commercetools-sync-java/issues/47)
    - **Inventory Sync** - Introduced batch processing support. [#73](https://github.com/commercetools/commercetools-sync-java/issues/73)
