# RELEASE NOTES

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

6. Add Compatibility notes section, which specifies explicitly if there
are breaking changes. If there are, then a migration guide should be provided.

-->

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [v1.0.0-M2-beta -  Sep 28, 2017](#v100-m2-beta----sep-28-2017)
- [v1.0.0-M1 -  Sep 06, 2017](#v100-m1----sep-06-2017)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

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

**Compatibility notes**
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