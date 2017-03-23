package com.commercetools.sync.commons;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUtils {
    private static BlockingSphereClient ctpClient;
    private static final long timeout = 30;

    public static CategorySyncOptions getMockCategorySyncOptions() {
        return new CategorySyncOptions("xxxxx",
                "xxxxxxx", "xxxxxxxx");
    }

    public static List<CategoryDraft> getMockCategoryDrafts() {
        List<CategoryDraft> categoryDrafts = new ArrayList<>();
        CategoryDraft categoryDraft1 = getMockCategoryDraft(Locale.GERMAN,"draft1", "slug1", "SH663881");
        CategoryDraft categoryDraft2 = getMockCategoryDraft(Locale.GERMAN,"draft2", "slug2", "SH604972");
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);
        return categoryDrafts;
    }

    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nonnull final String externalId) {
        CategoryDraft mockCategoryDraft = mock(CategoryDraft.class);
        when(mockCategoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(mockCategoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(mockCategoryDraft.getExternalId()).thenReturn(externalId);
        when(mockCategoryDraft.getCustom()).thenReturn(getMockCustomFieldsDraft());
        return mockCategoryDraft;
    }

    public static CustomFieldsDraft getMockCustomFieldsDraft(){
        Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        customFieldsJsons.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de","rot").put("en", "red"));
        return CustomFieldsDraft.ofTypeKeyAndJson("StepCategoryTypeKey", customFieldsJsons);
    }
}
