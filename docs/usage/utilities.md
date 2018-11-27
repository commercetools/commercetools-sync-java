# UTILITIES

A utility which provides an API for building CTP product type update actions and product type synchronisation.

## Build all update actions

A utility method provided by the library to compare a ProductType with a new ProductTypeDraft and results in a list of product type update actions.

```java
List<UpdateAction<ProductType>> updateActions = ProductTypeSyncUtils.buildActions(productType, productTypeDraft, productTypeSyncOptions);
```

## Build particular update action\(s\)

Utility methods provided by the library to compare the specific fields of a ProductType and a new ProductTypeDraft, and in turn, build the update action. One example is the `buildChangeNameUpdateAction` which compares names:

```java
Optional<UpdateAction<ProductType>> updateAction = ProductTypeUpdateActionUtils.buildChangeNameAction(oldProductType, productTypeDraft);
```

More examples of those utils for different fields can be found [here](https://github.com/commercetools/commercetools-sync-java/tree/8510fbcb09426c7c47955e2a2cbcde9cafe81a5c/src/test/java/com/commercetools/sync/producttypes/utils/ProductTypeUpdateActionUtilsTest.java).

