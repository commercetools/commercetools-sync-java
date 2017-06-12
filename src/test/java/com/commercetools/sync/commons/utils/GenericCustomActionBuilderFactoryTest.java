package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.channels.helpers.ChannelCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilderFactory;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenericCustomActionBuilderFactoryTest {

    @Test
    public void of_WithImplementedResource_ShouldCreateConcreteBuilder() throws BuildUpdateActionException,
        IllegalAccessException, InstantiationException {
        final GenericCustomActionBuilder categoryCustomActionBuilder = GenericCustomActionBuilderFactory
            .of(mock(Category.class));
        assertThat(categoryCustomActionBuilder).isInstanceOf(CategoryCustomActionBuilder.class);

        final GenericCustomActionBuilder channelCustomActionBuilder = GenericCustomActionBuilderFactory
            .of(mock(Channel.class));
        assertThat(channelCustomActionBuilder).isInstanceOf(ChannelCustomActionBuilder.class);
    }

    @Test
    public void of_WithNonImplementedResource_ShouldThrowBuildUpdateActionException() throws BuildUpdateActionException,
        IllegalAccessException, InstantiationException {
        assertThatThrownBy(() -> {
            final Cart cart = mock(Cart.class);
            when(cart.toReference()).thenReturn(Cart.referenceOfId("cartId"));
            GenericCustomActionBuilderFactory.of(cart);
        }).isInstanceOf(BuildUpdateActionException.class);
    }

}
