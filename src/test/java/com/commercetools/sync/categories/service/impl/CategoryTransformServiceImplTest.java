package com.commercetools.sync.categories.service.impl;

import static com.commercetools.sync.categories.utils.CategoryReferenceResolutionUtils.mapToCategoryDrafts;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.categories.service.CategoryTransformService;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class CategoryTransformServiceImplTest {

  @Test
  void transform_ShouldReplaceCategoryReferenceIdsWithKeys() {
    // preparation
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    final SphereClient sourceClient = mock(SphereClient.class);
    final CategoryTransformService categoryTransformService =
        new CategoryTransformServiceImpl(sourceClient, referenceIdToKeyCache);
    final List<Category> categoryPage =
        asList(
            readObjectFromResource("category-key-1.json", Category.class),
            readObjectFromResource("category-key-2.json", Category.class));
    final List<String> referenceIds =
        categoryPage.stream()
            .filter(category -> category.getCustom() != null)
            .map(category -> category.getCustom().getType().getId())
            .collect(Collectors.toList());

    String jsonStringCategories =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3\",\"key\":\"cat1\"}]}";
    final ResourceKeyIdGraphQlResult categoriesResult =
        SphereJsonUtils.readObject(jsonStringCategories, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any()))
        .thenReturn(CompletableFuture.completedFuture(categoriesResult));

    // test
    final CompletionStage<List<CategoryDraft>> draftsFromPageStage =
        categoryTransformService.toCategoryDrafts(categoryPage);

    // assertions
    final List<CategoryDraft> expectedResult =
        mapToCategoryDrafts(categoryPage, referenceIdToKeyCache);
    final List<String> referenceKeys =
        expectedResult.stream()
            .filter(category -> category.getCustom() != null)
            .map(category -> category.getCustom().getType().getId())
            .collect(Collectors.toList());
    assertThat(referenceKeys).doesNotContainSequence(referenceIds);
    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  }
}
