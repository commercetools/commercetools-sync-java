# 2. LineItem and TextLineItem update actions of the ShoppingLists.

Date: 2020-11-04

## Status

[Approved](https://github.com/commercetools/commercetools-sync-java/pull/614)

## Context

<!-- The issue motivating this decision, and any context that influences or constrains the decision. -->

In a commerce application, a shopping list is a personal wishlist of a customer, (i.e. ingredients for a recipe, birthday wishes). 
Shopping lists hold line items of products in the platform or any other items that can be described as text line items.

We have challenges to build update actions of the `LineItem` and `TextLineItem` because of the nature of the synchronization, 
so in this document, we will describe the reasons and constraints, mostly related to order of the items: 
- LineItem orders might be important, if the customer has a front end that sorts the line items with their order could mean sorting by importance.

## LineItems

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
            <p>Draft has line items with <b>SKU-1</b> and <b>SKU-2</b>. In the target project line item with
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
      "id": "1c38d582-2e65-43f8-85db-4d34e6cff57a",
      "variant": {
        "sku": "SKU-1"
      },
      "quantity": 1,
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
            <p>Draft has line items with <b>SKU-1</b>, <b>SKU-3</b> and <b>SKU-2</b> also in target project line item with
                <b>SKU-1</b> exists in the same order, <b>SKU-3</b> is a new line item, and <b>SKU-2</b> needs to be in last order.</p>
            <p> So we need to create an <a href="https://docs.commercetools.com/api/projects/shoppingLists#add-lineitem">AddLineItem</a> action and
                a <a href="https://docs.commercetools.com/api/projects/shoppingLists#change-lineitems-order">Change LineItems Order</a>
                of the line items <b>SKU-2</b> and <b>SKU-3</b>, because when we add line item with <b>SKU-3</b>
                the order will be <b>SKU-1</b>, <b>SKU-2</b> and <b>SKU-3</b>.</p>
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
                The solution idea about the new line item and changed order is still same like in the case-1. Do we
                need to remove and add line item with <b>SKU-1</b>? No, it is not needed and we could start the removing
                and adding from the first order change.
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
          "textField": "text-3"
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

### Do we need to remove and add all line items when no new line item is added or removed, just order is different ?

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
      "sku": "SKU-2",
      "quantity": 2,
      "addedAt": "2020-11-05T10:00:10.101Z",
      "custom": {
        "type": {
          "key": "custom-type-for-shoppinglists"
        },
        "fields": {
          "textField": "text-2"
        }
      }
    },
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
      "quantity": 1,
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
            <p>The Draft has line items with <b>SKU-2</b> and <b>SKU-1</b> also in target project line item
                with <b>SKU-2</b> and <b>SKU-1</b> exists but in a different order.</p>
            <p> So we need
                a <a href="https://docs.commercetools.com/api/projects/shoppingLists#change-lineitems-order">Change
                    LineItems Order</a>
                of the line items with order <b>SKU-2</b> and <b>SKU-1</b>.</p>
            <p>The <b>challenge</b> here is about the line item order and no new line item is added or removed,
                just order is different, so we need to find a better way to avoid creating unnecessary actions like
                removing and adding back, is this possible ?
            </p>
        </td>
    </tr>
    <tr>
        <th colspan="2">Proposed solution</th>
    </tr>
    <tr>
        <td colspan="2">
            <p>
                The solution idea for the changing order with removing and adding back looks like an overhead. We know 
                all line item ids, so change order action could be created. However, the challenge is
                finding an algorithm to compare and find the line item ids, and then prepare an order.                
            </p>
            <p>
                The example above seems reasonable but how you would sync a case like: 
                <b>[SKU-1, SKU-2, SKU-3]</b> to <b>[SKU-3, SKU-1, SKU-4, SKU-2]</b>, so with a different algorithm it might 
                be done with change order <b>[line-item-id-3, line-item-id-1, line-item-id-2]</b> then <b>removeLineItem 
                SKU-2</b>, add back <b>addLineItem SKU-4</b>, in total 3 actions. Even for this we need to remove and add back.
            </p>
            <p>
                It looks like there are more different cases, when we dig in. <b>That's why we decided to keep the idea 
                of removing and adding back to  not have a more complex algorithm.</b>      
            </p>
        </td>
    </tr>
</table>

## TextLineItems

### How to ensure text line item order?

<table>
    <tr>
        <th>TextLineItemDrafts</th>
        <th>TextLineItems</th>
    </tr>
    <tr>
        <td><pre lang="json">
{
  "textLineItems": [
    {
     "name": {
        "de": "name1-DE",
        "en": "name1-EN"
      },
      "description": {
        "de": "desc1-DE",
        "en": "desc1-EN"
      },
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
      "name": {
        "de": "name2-DE",
        "en": "name2-EN"
      },
      "description": {
        "de": "desc2-DE",
        "en": "desc2-EN"
      },
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
  "textLineItems": [
    {
      "id": "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "name": {
        "de": "name2-DE",
        "en": "name2-EN"
      },
      "description": {
        "de": "desc2-DE",
        "en": "desc2-EN"
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
            <p>Draft has text line items with <b>name-1</b> and <b>name-2</b>. In the target project text line item with
                <b>name-2</b> exists, so <b>name-1</b> is a new text line item. </p>
            <p> So we need to create an <a
                    href="https://docs.commercetools.com/api/projects/shoppingLists#add-textlineitem">AddTextLineItem</a>
                action
                and a <a href="https://docs.commercetools.com/api/projects/shoppingLists#change-textlineitems-order">Change
                    TextLineItems Order</a>
                of the text line items <b>name-1</b> and <b>name-2</b>, because when we add text line item with <b>name-1</b>
                the order will be <b>name-2</b> and <b>name-1</b>.</p>
            <p>The <b>challenge</b> in here is, those actions cannot be added in one request because we don't know the
                text line item id of the new text line item
                with <b>name-1</b>. We need to find another way to create a new text line item with the right order.
            </p>
        </td>
    </tr>
    <tr>
        <th colspan="2">Proposed solution</th>
    </tr>
    <tr>
        <td colspan="2">
            <p>
                Normally, for a difference, we do a set intersection and then calculate action for differences,
                but that does not make sense because we are not aware of the order from the draft.
            </p>
            <p>
                Before that, we need to analyse the <a
                    href="https://docs.commercetools.com/api/projects/shoppingLists#add-textlineitem">AddTextLineItem</a>
                action, because the platform is not checking if the data exist. An API user could add the
                exact same data multiple times. So it's impossible to know the order by
                just checking the differences between the resource and draft object. Also, the name of the text line item 
                does not need to be unique as line item does. Each line item is identified by its product variant and 
                custom fields. Luckily the platform supports changing all field (except <b>addedAt</b>) of the text line 
                items, so when an order change is needed we update the
                fields of the text line items. Which will look like:
            </p>
            <pre lang="json">
{
  "version": 1,
  "actions": [
    {
      "action" : "changeTextLineItemName",
      "textLineItemId" : "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "name": {
        "de": "name1-DE",
        "en": "name1-EN"
      }
    },
    {
      "action" : "changeTextLineItemQuantity",
      "textLineItemId" : "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "quantity" : 1
    },
    {
      "action" : "setTextLineItemDescription",
      "textLineItemId" : "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "description": {
        "de": "desc1-DE",
        "en": "desc1-EN"
      }
    },
    {
      "action" : "setTextLineItemCustomField",
      "textLineItemId" : "24de3821-e27d-4ddb-bd0b-ecc99365285f",
      "name" : "textField",
      "value" : "text-1"
    },
    {
      "action" : "addTextLineItem",
      "name": {
        "de": "name2-DE",
        "en": "name2-EN"
      },
      "description": {
        "de": "desc2-DE",
        "en": "desc2-EN"
      },
      "quantity": 2,
      "addedAt": "2020-11-04T09:38:35.571Z",
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

## Common

### How addedAt will be compared?

In commercetools shopping lists API, there is no [update action](https://docs.commercetools.com/api/projects/shoppingLists#update-actions) 
to change the `addedAt` field of the `LineItem` and `TextLineItem`, also in API it has a default value described as `Defaults to the current date and time.`, 
when it's not set in the draft, so how to compare and update this field?

**Proposed solution:**

The `addedAt` field will be synced only if the value provided in the line item draft, otherwise, the `addedAt` value will be omitted. 
To be able to sync it we need to remove and add this line item back with the up-to-date value. 
After some discussions in pull requests, we decided to not change this field.

## Decision

<!-- The change that we're proposing or have agreed to implement. -->

- In commercetools API, the product variant to be selected in the LineItemDraft can be specified either by its product ID plus variant ID or by its SKU. 
For the sync library, product variant will be matched by its SKU, if the SKU not set for a LineItemDraft, the draft will not be synced and an error callback will be triggered.
Check [LineItemDraft Product Variant Selection](https://docs.commercetools.com/api/projects/shoppingLists#lineitemdraft-product-variant-selection) for more details.

- When a [Change LineItems Order](https://docs.commercetools.com/api/projects/shoppingLists#change-lineitems-order) action is needed, 
the line items will be removed and added back with the order provided in the `ShoppingListDraft`.

- When a [Change TextLineItems Order](https://docs.commercetools.com/api/projects/shoppingLists#change-textlineitems-order) action is needed, 
the text line items will be updated with using update actions with the order provided in the `ShoppingListDraft`.

- In commercetools shopping lists API, there is no [update action](https://docs.commercetools.com/api/projects/shoppingLists#update-actions) 
to change the `addedAt` field of the `LineItem` and `TextLineItem`, hereby we will not update the `addedAt` value.

## Consequences

<!-- What becomes easier or more difficult to do and any risks introduced by the change that will need to be mitigated. -->

- To ensure the order of the line items, we need to remove and add line items. That means a bigger payload and a performance overhead. 

- To ensure the order of text line items, we need to calculate and update more than expected. That means a bigger payload and a performance overhead.

- **Caveat**: `addedAt` values not synced.
