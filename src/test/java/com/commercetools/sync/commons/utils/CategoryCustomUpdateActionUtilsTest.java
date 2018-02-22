package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
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

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction("key", new HashMap<>(), mock(Category.class),
                new CategoryCustomActionBuilder(), null, Category::getId,
                categoryResource -> categoryResource.toReference().getTypeId(), categoryResource -> null,
                CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildRemoveCustomTypeAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction =
            new CategoryCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
    }

    @Test
    public void buildSetCustomFieldAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction = new CategoryCustomActionBuilder()
            .buildSetCustomFieldAction(null, null, "name", mock(JsonNode.class));

        assertThat(updateAction).isInstanceOf(SetCustomField.class);
    }
}
