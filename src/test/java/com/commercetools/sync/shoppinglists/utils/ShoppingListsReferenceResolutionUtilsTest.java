package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.expansion.ExpansionPath;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListsReferenceResolutionUtilsTest {

    @Test
    void buildShoppingListQuery_Always_ShouldReturnQueryWithAllNeededReferencesExpanded() {
        assertThat(buildShoppingListQuery().expansionPaths())
            .containsExactly(
                ExpansionPath.of("customer"),
                ExpansionPath.of("custom.type"),
                ExpansionPath.of("lineItems[*].custom.type"),
                ExpansionPath.of("textLineItems[*].custom.type"));
    }
}
