package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.channels.helpers.ChannelCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.inventories.helpers.InventoryCustomActionBuilder;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.products.helpers.PriceCustomActionBuilder;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class GenericCustomActionBuilderFactoryTest {

    @Test
    public void createBuilder_WithImplementedResourceAndNullContainer_ShouldCreateConcreteBuilder()
        throws BuildUpdateActionException, IllegalAccessException, InstantiationException {
        final GenericCustomActionBuilder categoryCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(Category.class), null);
        assertThat(categoryCustomActionBuilder).isInstanceOf(CategoryCustomActionBuilder.class);

        final GenericCustomActionBuilder channelCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(Channel.class), null);
        assertThat(channelCustomActionBuilder).isInstanceOf(ChannelCustomActionBuilder.class);

        final GenericCustomActionBuilder inventoryEntryCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(InventoryEntry.class), null);
        assertThat(inventoryEntryCustomActionBuilder).isInstanceOf(InventoryCustomActionBuilder.class);
    }

    @Test
    public void createBuilder_WithImplementedResourceAndAllowedContainer_ShouldCreateConcreteBuilder()
        throws BuildUpdateActionException, IllegalAccessException, InstantiationException {
        final GenericCustomActionBuilder categoryCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(Category.class), Category.class);
        assertThat(categoryCustomActionBuilder).isInstanceOf(CategoryCustomActionBuilder.class);

        final GenericCustomActionBuilder channelCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(Channel.class), Channel.class);
        assertThat(channelCustomActionBuilder).isInstanceOf(ChannelCustomActionBuilder.class);

        final GenericCustomActionBuilder inventoryEntryCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(InventoryEntry.class), InventoryEntry.class);
        assertThat(inventoryEntryCustomActionBuilder).isInstanceOf(InventoryCustomActionBuilder.class);
    }

    @Test
    public void createBuilder_WithImplementedResourceAndNonImplementedContainer_ThrowsBuildUpdateActionException() {
        assertThatThrownBy(() ->
            GenericCustomActionBuilderFactory
                .createBuilder(mock(Category.class), Customer.class)).isInstanceOf(BuildUpdateActionException.class)
                                                    .hasMessageMatching("Update actions for resource: 'Category.*' and "
                                                        + "container resource: 'Customer' is not implemented.");
    }

    @Test
    public void createBuilder_WithImplementedResourceAndImplementedContainer_ShouldCreateConcreteBuilder()
        throws BuildUpdateActionException, IllegalAccessException, InstantiationException {
        final GenericCustomActionBuilder productPriceCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(Price.class), Product.class);
        assertThat(productPriceCustomActionBuilder).isInstanceOf(PriceCustomActionBuilder.class);

        final GenericCustomActionBuilder productAssetCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(Asset.class), Product.class);
        assertThat(productAssetCustomActionBuilder).isInstanceOf(AssetCustomActionBuilder.class);

        final GenericCustomActionBuilder categoryAssetCustomActionBuilder = GenericCustomActionBuilderFactory
            .createBuilder(mock(Asset.class), Category.class);
        assertThat(categoryAssetCustomActionBuilder).isInstanceOf(
            com.commercetools.sync.categories.helpers.AssetCustomActionBuilder.class);
    }

    @Test
    public void createBuilder_WithNonImplementedResourceAndNullContainer_ShouldThrowBuildUpdateActionException() {
        assertThatThrownBy(() ->
            GenericCustomActionBuilderFactory.createBuilder(mock(Cart.class), null))
            .isInstanceOf(BuildUpdateActionException.class)
            .hasMessageMatching("Update actions for resource: 'Cart.*' and container"
                + " resource: 'null' is not implemented.");
    }

    @Test
    public void createBuilder_WithNonImplementedResourceAndImplementedContainer_ThrowsBuildUpdateActionException() {
        assertThatThrownBy(() ->
            GenericCustomActionBuilderFactory.createBuilder(mock(Cart.class), Product.class))
            .isInstanceOf(BuildUpdateActionException.class)
            .hasMessageMatching("Update actions for resource: 'Cart.*' and container"
                + " resource: 'Product' is not implemented.");
    }


}
