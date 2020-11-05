# 2. LineItem and TextLineItem update actions of the ShoppingLists.

Date: 2020-11-04

## Status

[Proposed](https://github.com/commercetools/commercetools-sync-java/pull/614)

## Context

<!-- The issue motivating this decision, and any context that influences or constrains the decision. -->

In a commerce application, a shopping list is a personal wishlist of a customer, (i.e. Ingredients for a recipe, birthday wishes). 
Shopping lists hold line items of products in the platform or any other items that can be described as text line items.

We have challenges to build update actions of the `LineItem` and `TextLineItem` because of the nature of the synchronization, 
so in this document, we will describe the reasons and constraints. 

> Note: The cases below are the same for the `TextLineItem` so here those are no added as a separate case.

### How to ensure line item order?

<table>
    <tr>
        <th>LineItemDrafts</th>
        <th>LineItems</th>
    </tr>
    <tr>
        <td><pre lang="json">
{
  "lineItems": [
    {
      "sku": "SKU-1",
      "quantity": 1,
      "addedAt": "2020-11-04T09:38:35.571Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-1"
        }
      }
    },
    {
      "sku": "SKU-2",
      "quantity": 2,
      "addedAt": "2020-11-04T09:40:12.341Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-2"
        }
      }
    }
  ]
}
</pre>
        </td>
        <td><pre lang="json">
{
  "lineItems": [
    {
      "id": "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "variant": {
        "sku": "SKU-2"
      },
      "quantity": 2,
      "custom": {
        "type": {
          "id": "4796e155-f5a4-403a-ae1a-04b10c9dfc54",
          "obj": {
            "key": "custom-type-for-shoppinglists"
          }
        },
        "fields": {
          "textField": "text-2"
        }
      },
     "addedAt": "2020-11-04T09:38:35.571Z"
    }
  ]
}
</pre>
        </td>
    </tr>
    <tr>
        <th colspan="2">Analysis</th>
    </tr>
    <tr>
        <td colspan="2">
            <p>Draft has line items with <b>SKU-1</b> and <b>SKU-2</b> also in target project line item with
                <b>SKU-2</b> exists, so <b>SKU-1</b> is a new line item. </p>
            <p> So we need to create an <a href="https://docs.commercetools.com/api/projects/shoppingLists#add-lineitem">AddLineItem</a> action
                and a <a href="https://docs.commercetools.com/api/projects/shoppingLists#change-lineitems-order">Change LineItems Order</a> 
                of the line items <b>SKU-1</b> and <b>SKU-2</b>, because when we add line item with <b>SKU-1</b>
                the order will be <b>SKU-2</b> and <b>SKU-1</b>.</p>
            <p>The <b>challenge</b> in here is, those actions can not be added in one request because we don't know the
                line item id of the new line item
                with <b>SKU-1</b>, so we need to find another way to create a new line item with the right order.</p>
        </td>
    </tr>
    <tr>
        <th colspan="2">Proposed solution</th>
    </tr>
    <tr>
        <td colspan="2">
            <p>
                Normally, for a difference, we might do a set intersection and then calculate action for differences, 
                but that does not make sense because we are not aware of the order from the draft. 
                So in this case the one request could be created but we might need to remove line item with <b>SKU-2</b>
                and line items in the draft with the given order with the line items <b>SKU-1</b> and <b>SKU-2</b>.
            </p>
            <pre lang="json">
{
  "version": 1,
  "actions": [
    {
      "action": "removeLineItem",
      "lineItemId": "24de3821-e27d-4ddb-bd0b-ecc99365285f"
    },
    {
      "action": "addLineItem",
      "sku": "SKU-1",
      "quantity": 1,
      "addedAt": "2020-11-04T09:38:35.571Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-1"
        }
      }
    },
    {
      "action": "addLineItem",
      "sku": "SKU-2",
      "quantity": 2,
      "addedAt": "2020-11-04T09:40:12.341Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-2"
        }
      }
    }
  ]
}
</pre>
        </td>
    </tr>
</table>

### Do we need to remove all line items when the order changes ?

<table>
    <tr>
        <th>LineItemDrafts</th>
        <th>LineItems</th>
    </tr>
    <tr>
        <td><pre lang="json">
{
  "lineItems": [
    {
      "sku": "SKU-1",
      "quantity": 1,
      "addedAt": "2020-11-04T09:38:35.571Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-1"
        }
      }
    },
    {
      "sku": "SKU-3",
      "quantity": 3,
      "addedAt": "2020-11-05T10:00:10.101Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-3"
        }
      }
    }
  ]
}
</pre>
        </td>
        <td><pre lang="json">
{
  "lineItems": [
    {
      "id": "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "variant": {
        "sku": "SKU-1"
      },
      "quantity": 2,
      "custom": {
        "type": {
          "id": "4796e155-f5a4-403a-ae1a-04b10c9dfc54",
          "obj": {
            "key": "custom-type-for-shoppinglists"
          }
        },
        "fields": {
          "textField": "text-1"
        }
      },
     "addedAt": "2020-11-04T09:38:35.571Z"
    },
    {
      "id": "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "variant": {
        "sku": "SKU-2"
      },
      "quantity": 2,
      "custom": {
        "type": {
          "id": "4796e155-f5a4-403a-ae1a-04b10c9dfc54",
          "obj": {
            "key": "custom-type-for-shoppinglists"
          }
        },
        "fields": {
          "textField": "text-2"
        }
      },
     "addedAt": "2020-11-04T09:40:12.341Z"
    }
  ]
}
</pre>
        </td>
    </tr>
    <tr>
        <th colspan="2">Analysis</th>
    </tr>
    <tr>
        <td colspan="2">
            <p>Draft has line items with <b>SKU-1</b> and <b>SKU-3</b> also in target project line item with
                <b>SKU-1</b> exists, so <b>SKU-3</b> is a new line item, and <b>SKU-2</b> needs to be removed.</p>
            <p> So we need to create an <a href="https://docs.commercetools.com/api/projects/shoppingLists#add-lineitem">AddLineItem</a> action
                , a <a href="https://docs.commercetools.com/api/projects/shoppingLists#remove-lineitem">RemoveLineItem</a> action and
                a <a href="https://docs.commercetools.com/api/projects/shoppingLists#change-lineitems-order">Change LineItems Order</a> 
                of the line items <b>SKU-1</b> and <b>SKU-3</b>, because when we add line item with <b>SKU-1</b>
                the order will be <b>SKU-1</b> and <b>SKU-3</b>.</p>
            <p>The <b>challenge</b> in here is, those actions can not be added in one request because we don't know the
                line item id of the new line item
                with <b>SKU-3</b>, so we need to find another way to create a new line item with the right order.</p>
            <p>Also another <b>challenge</b> in here is about the line item with <b>SKU-1</b>, as the order and data is
                exactly same, we need to find a better way to avoid creating unnecessary actions.
            </p>
        </td>
    </tr>
    <tr>
        <th colspan="2">Proposed solution</th>
    </tr>
    <tr>
        <td colspan="2">
            <p>
                The solution idea about the new line item and changed order is still same with the case-1, but do we
                need to remove and add line item with <b>SKU-1</b>? Which is not needed and we could start the removing
                and adding from the changed order.
            </p>
            <pre lang="json">
{
  "version": 1,
  "actions": [
    {
      "action": "removeLineItem",
      "lineItemId": "24de3821-e27d-4ddb-bd0b-ecc99365285f"
    },
    {
      "action": "addLineItem",
      "sku": "SKU-3",
      "quantity": 3,
      "addedAt": "2020-11-05T10:00:10.101Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-3
        }
      }
    }
  ]
}
</pre>
        </td>
    </tr>
</table>

## Decision

<!-- The change that we're proposing or have agreed to implement. -->

- Product variant will be matched by its SKU, if the SKU not set for a LineItemDraft, the draft will not be synced and an error callback will be triggered.
> Note: In commercetools API, the product variant to be selected in the LineItemDraft can be specified either by its product ID plus variant ID or by its SKU.

- When a [Change LineItems Order](https://docs.commercetools.com/api/projects/shoppingLists#change-lineitems-order">) action is needed 
the line items will be removed and added back with the order provided in the `ShoppingListDraft`.

## Consequences

<!-- What becomes easier or more difficult to do and any risks introduced by the change that will need to be mitigated. -->

- To ensure the order, we need to remove and add line items, which means a bigger payload, so performance overhead.
- [Add TextLineItem](https://docs.commercetools.com/api/projects/shoppingLists#add-textlineitem) is not checking the data is exists, so a user could add the
exact same data multiple times, so it's hard to know the order by just checking the object, so in this case, if the data is the same, the order will not be changed.