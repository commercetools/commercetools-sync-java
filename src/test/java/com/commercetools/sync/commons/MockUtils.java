package com.commercetools.sync.commons;

import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    /**
     * Creates a mock {@link Type} with the supplied {@code id} and {@code key}.
     * @param id the id of the type mock.
     * @param key the key of the type mock.
     * @return a mock product variant with the supplied prices and assets.
     */
    @Nonnull
    public static Type getTypeMock(@Nonnull final String id, @Nonnull final String key) {
        final Type mockCustomType = mock(Type.class);
        when(mockCustomType.getId()).thenReturn(id);
        when(mockCustomType.getKey()).thenReturn(key);
        return mockCustomType;
    }

    /**
     * Creates a mock {@link Asset} with the supplied {@link Type} reference for it's custom field.
     * @param typeReference the type reference to attach to the custom field of the created asset.
     * @return a mock asset with the supplied type reference on it's custom field.
     */
    @Nonnull
    public static Asset getAssetMockWithCustomFields(@Nullable final Reference<Type> typeReference) {
        // Mock Custom with expanded type reference
        final CustomFields mockCustomFields = mock(CustomFields.class);
        when(mockCustomFields.getType()).thenReturn(typeReference);

        // Mock asset with custom fields
        final Asset asset = mock(Asset.class);
        when(asset.getCustom()).thenReturn(mockCustomFields);
        return asset;
    }
}
