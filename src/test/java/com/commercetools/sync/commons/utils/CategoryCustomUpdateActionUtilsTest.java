package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CategoryCustomUpdateActionUtilsTest {
    private CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction =
            GenericUpdateActionUtils.<Category, Category>
                buildTypedSetCustomTypeUpdateAction("key", new HashMap<>(), mock(Category.class),
                null, null, Category::getId,
                categoryResource -> categoryResource.toReference().getTypeId(), categoryResource -> null,
                syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction =
            GenericUpdateActionUtils.<Category, Category>buildTypedRemoveCustomTypeUpdateAction(mock(Category.class),
                null, null,
                Category::getId, categoryResource -> categoryResource.toReference().getTypeId(),
                categoryResource -> null, syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction =
            GenericUpdateActionUtils.<Category, Category>buildTypedSetCustomFieldUpdateAction("name",
                mock(JsonNode.class), mock(Category.class), null, null, Category::getId,
                categoryResource -> categoryResource.toReference().getTypeId(),
                categoryResource -> null,
                syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetCustomField.class);
    }
}
