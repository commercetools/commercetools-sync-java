package com.commercetools.sync.products.actions;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Test;

import java.util.List;

import static com.commercetools.sync.products.ProductTestUtils.addName;
import static com.commercetools.sync.products.ProductTestUtils.localizedString;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductUpdateActionsBuilderTest {

    @Test
    public void of_expectSingleton() {
        ProductUpdateActionsBuilder productUpdateActionsBuilder = ProductUpdateActionsBuilder.of();

        assertThat(productUpdateActionsBuilder).isNotNull();

        ProductUpdateActionsBuilder productUpdateActionsBuilder1 = ProductUpdateActionsBuilder.of();

        assertThat(productUpdateActionsBuilder1).isSameAs(productUpdateActionsBuilder);
    }

    @Test
    public void buildActions_emptyForIdentical() {
        ProductUpdateActionsBuilder updateActionsBuilder = ProductUpdateActionsBuilder.of();
        Product productMock = mock(Product.class);
        addName(productMock, null);
        ProductDraft productDraftMock = mock(ProductDraft.class);

        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(productMock, productDraftMock);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildActions_nonEmptyForDifferent() {
        ProductUpdateActionsBuilder updateActionsBuilder = ProductUpdateActionsBuilder.of();
        Product productMock = mock(Product.class);
        addName(productMock, "name1");
        ProductDraft draftMock = mock(ProductDraft.class);
        when(draftMock.getName()).thenReturn(localizedString("name2"));

        List<UpdateAction<Product>> updateActions = updateActionsBuilder.buildActions(productMock, draftMock);

        assertThat(updateActions).isEqualTo(singletonList(ChangeName.of(localizedString("name2"))));
    }

}
