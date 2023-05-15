package com.commercetools.sync.sdk2.categories.utils;

import static com.commercetools.sync.sdk2.categories.utils.CategoryReferenceResolutionUtils.mapToCategoryDrafts;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.readObjectFromResource;
import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromJsonString;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class CategoryTransformUtilsTest {

  @Test
  void transform_ShouldReplaceCategoryReferenceIdsWithKeys() {
    // preparation
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<Category> categoryPage =
        asList(
            readObjectFromResource("category-key-1.json", Category.class),
            readObjectFromResource("category-key-2.json", Category.class));
    final List<String> referenceIds =
        categoryPage.stream()
            .filter(category -> category.getCustom() != null)
            .map(category -> category.getCustom().getType().getId())
            .collect(Collectors.toList());

    final String jsonStringCategories =
        "{\"data\":{\"categories\":{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3\",\"key\":\"cat1\"}]}}}";
    final GraphQLResponse categoriesResult =
        fromJsonString(jsonStringCategories, GraphQLResponse.class);

    final ApiHttpResponse<GraphQLResponse> response = mock(ApiHttpResponse.class);
    when(response.getBody()).thenReturn(categoriesResult);

    when(sourceClient.graphql()).thenReturn(mock());
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute()).thenReturn(CompletableFuture.completedFuture(response));

    // test
    final CompletionStage<List<CategoryDraft>> draftsFromPageStage =
        CategoryTransformUtils.toCategoryDrafts(sourceClient, referenceIdToKeyCache, categoryPage);

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
