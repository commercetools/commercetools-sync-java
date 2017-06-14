package com.commercetools.sync.commons;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUtils {
    /**
     * Creates a mock instance of {@link CustomFieldsDraft} with the key 'StepCategoryTypeKey' and two custom fields
     * 'invisibleInShop' and'backgroundColor'.
     *
     * <p>The 'invisibleInShop' field is of type {@code boolean} and has value {@code false} and the
     * the 'backgroundColor' field is of type {@code localisedString} and has the values {"de": "rot", "en": "red"}
     *
     * @return a mock instance of {@link CustomFieldsDraft} with some hardcoded custom fields and key.
     */
    public static CustomFieldsDraft getMockCustomFieldsDraft() {
        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        customFieldsJsons
            .put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));
        return CustomFieldsDraft.ofTypeIdAndJson("StepCategoryTypeId", customFieldsJsons);
    }

    /**
     * Creates a mock {@link CategoryService} that returns a mocked {@link Category} instance whenever any of the
     * following methods are called:
     * <ul>
     * <li>{@link CategoryService#fetchCategoryByExternalId(String)}</li>
     * <li>{@link CategoryService#createCategory(CategoryDraft)}</li>
     * <li>{@link CategoryService#updateCategory(Category, List)}</li>
     * </ul>
     * or returns a dummy category id of value "parentId" whenever the following method is called on the service:
     * <ul>
     * <li>{@link CategoryService#fetchCachedCategoryId(String)}</li>
     * </ul>
     *
     * <p>The mocked category returned has the following fields:
     * <ul>
     * <li>name: {"en": "name"}</li>
     * <li>slug: {"en": "slug"}</li>
     * <li>externalId: "externalId"</li>
     * <li>description: {"en": "description"}</li>
     * <li>metaDescription: {"en": "metaDescription"}</li>
     * <li>metaTitle: {"en": "metaTitle"}</li>
     * <li>metaKeywords: {"en": "metaKeywords"}</li>
     * <li>orderHint: "orderHint"</li>
     * <li>parentId: "parentId"</li>
     * </ul>
     *
     * @return the created mock of the {@link CategoryService}.
     */
    public static CategoryService getMockCategoryService() {
        final Category category = getMockCategory(Locale.ENGLISH,
            "name",
            "slug",
            "externalId",
            "description",
            "metaDescription",
            "metaTitle",
            "metaKeywords",
            "orderHint",
            "parentId");


        final CategoryService categoryService = mock(CategoryService.class);
        when(categoryService.fetchCategoryByExternalId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(category)));
        when(categoryService.updateCategory(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(category));
        when(categoryService.createCategory(any()))
            .thenReturn(CompletableFuture.completedFuture(category));
        when(categoryService.fetchCachedCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("parentId")));
        return categoryService;
    }

    /**
     * Creates a mock {@link TypeService} that returns a dummy type id of value "typeId" instance whenever the
     * following method is called on the service:
     * <ul>
     * <li>{@link TypeService#fetchCachedTypeId(String)}</li>
     * </ul>
     *
     * @return the created mock of the {@link TypeService}.
     */
    public static TypeService getMockTypeService() {
        final TypeService typeService = mock(TypeService.class);
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("typeId")));
        return typeService;
    }

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link BaseSyncStatistics}.
     * Note: The order of the fields in the built JSON String depends on the order of the instance variables in this
     * class.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON String.
     * @return a JSON representation of the given {@code statistics} as a String.
     */
    public static String getStatisticsAsJsonString(@Nonnull final BaseSyncStatistics statistics)
        throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(statistics);
    }
}
