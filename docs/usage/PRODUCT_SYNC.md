# commercetools product sync

## Usage

### Correlation of product sync options in the context of publishing and reverting staged changes

Product sync module provides (among others) the following sync options:
* `shouldPublish` - whether to auto-publish product after creation or update
* `shouldRevertStagedChanges` - whether to revert potential staged changes before synchronization

#### Creating new product

In case of creating new product based on provided draft only `shouldPublish` option matters.
It controls whether the created product will be published or left as staged only.

#### Updating existing product

In case of updating existing product both options are considered and certain scenarios might occur.
Those scenarios depend on the state of existing product described by its two parameters:
* product.`hasStagedChanges` - informs whether the existing product already has staged changes before sync happens
* product.`isPublished` - informs whether the existing product is published

Before update sync is executed the value of `shouldRevertStagedChanges` is checked.
If `shouldRevertStagedChanges` == true and product.`hasStagedChanges` == true then `RevertStagedChanges` action
is executed on the existing product. Thus, through `shouldRevertStagedChanges` option library's user might
control if potential external changes on staged projection of product should be reverted on the product
before syncing with draft.

After this step actual update actions are executed.

Last step is publishing product which is executed when `shouldPublish` == true and depends on the following:
* if product.`isPublished` == false then Publish action is executed on product.
* if product.`isPublished` == true it depends on the following:
  * if product.`hasStagedChanges` == false then no action is required because product is already published and
  does not have any staged changes
  * if product.`hasStagedChanges` == true then Publish action is executed on product.