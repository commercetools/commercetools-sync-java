# 3. Syncing attribute type changes

Date: 2021-09-28

## Status

[Approved](https://github.com/commercetools/commercetools-sync-java/pull/787).

## Context

When changing an attribute definition type, we were using the approach by removing and re-adding the attribute with a new type but this approach is not working anymore. See: [#762](https://github.com/commercetools/commercetools-sync-java/issues/762)

So when removing and adding an attribute from a productType in a single request the API returns:

```
"code" : "AttributeDefinitionAlreadyExists",
"message" : "An attribute definition with name 'attr_name_1' already exists on product type 'newName'.",
```

We've discussed 3 different approaches to resolve the issue:

1. Change the logic to apply remove action only and after that throw and just document as limitation that such errors can happen but if they run same import again later it should at some point work.
2. Change the logic to apply remove action only and after that try apply re-add: handle specific error with a retry (exponential + backoff) with up to x retries.
3. Check if attribute with the same name id deleted and added in the same request and skip the update with triggering error callback with a message to ask the user to handle it manually.

## Decision

The third approach is favoured due to the unpredictability of the other approaches since [removeAttributeDefinition](https://docs.commercetools.com/api/projects/productTypes#remove-attributedefinition) action is [eventually consistent](https://docs.commercetools.com/api/general-concepts#eventual-consistency) now and only takes place after the corresponding attribute has been removed from all the products asynchronously by the platform.

### Best practice to change type

Changes to attributes should be planned carefully and implemented with an understanding of the impact on the product data and performance for each change.

1. Ensure product attributes of the same name are defined consistently across all product types.
2. Allow time between attribute removal and the addition of attributes with the same name. Removal and addition actions of the attribute with the same name should be applied in separate requests.

## Consequences

Support of changing the attribute type within a single API request is not supported anymore.
