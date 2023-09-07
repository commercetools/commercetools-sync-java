package com.commercetools.sync.commons.utils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategorySetCustomFieldAction;
import com.commercetools.api.models.category.CategorySetCustomTypeAction;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.categories.models.CategoryCustomTypeAdapter;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
    final String newCustomTypeId = UUID.randomUUID().toString();

    final CategoryUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                CategoryCustomTypeAdapter.of(mock(Category.class)),
                new CategoryCustomActionBuilder(),
                null,
                CategoryCustomTypeAdapter::getId,
                categoryResource -> ResourceTypeId.CATEGORY.getJsonName(),
                categoryResource -> null,
                CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(CategorySetCustomTypeAction.class);
    CategorySetCustomTypeAction categorySetCustomTypeAction =
        (CategorySetCustomTypeAction) updateAction;
    assertThat(categorySetCustomTypeAction.getType().getId()).isEqualTo(newCustomTypeId);
    assertThat(categorySetCustomTypeAction.getFields().values()).isEqualTo(emptyMap());
  }

  @Test
  void buildRemoveCustomTypeAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
    final CategoryUpdateAction updateAction =
        new CategoryCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

    assertThat(updateAction).isInstanceOf(CategorySetCustomTypeAction.class);
    CategorySetCustomTypeAction categorySetCustomTypeAction =
        (CategorySetCustomTypeAction) updateAction;
    assertThat(categorySetCustomTypeAction.getFields()).isEqualTo(null);
    assertThat(categorySetCustomTypeAction.getType()).isEqualTo(null);
  }

  @Test
  void buildSetCustomFieldAction_WithCategoryResource_ShouldBuildCategoryUpdateAction() {
    final String customFieldValue = "foo";
    final String customFieldName = "name";

    final CategoryUpdateAction updateAction =
        new CategoryCustomActionBuilder()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(CategorySetCustomFieldAction.class);
    CategorySetCustomFieldAction categorySetCustomFieldAction =
        (CategorySetCustomFieldAction) updateAction;
    assertThat(categorySetCustomFieldAction.getName()).isEqualTo(customFieldName);
    assertThat(categorySetCustomFieldAction.getValue()).isEqualTo(customFieldValue);
  }
}
