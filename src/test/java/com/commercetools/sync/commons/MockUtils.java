package com.commercetools.sync.commons;

import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
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

    public static CategoryService mockCategoryService(@Nonnull final Set<Category> existingCategories,
                                                      @Nonnull final Set<Category> createdCategories) {
        final CategoryService mockCategoryService = mock(CategoryService.class);
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
            .thenReturn(CompletableFuture.completedFuture(existingCategories));

        final Map<String, String> keyToIds =
            existingCategories.stream().collect(Collectors.toMap(Category::getKey, Category::getId));
        when(mockCategoryService.cacheKeysToIds()).thenReturn(completedFuture(keyToIds));

        when(mockCategoryService.createCategories(any())).thenReturn(completedFuture(createdCategories));
        return mockCategoryService;
    }

    public static CategoryService mockCategoryService(@Nonnull final Set<Category> existingCategories,
                                                      @Nonnull final Set<Category> createdCategories,
                                                      @Nonnull final Category updatedCategory) {
        final CategoryService mockCategoryService = mockCategoryService(existingCategories, createdCategories);
        when(mockCategoryService.updateCategory(any(), any())).thenReturn(completedFuture(updatedCategory));
        return mockCategoryService;
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
            .thenReturn(completedFuture(Optional.of("typeId")));
        return typeService;
    }
}
