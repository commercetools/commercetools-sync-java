package com.commercetools.sync.commons;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUtils {
    private static BlockingSphereClient ctpClient;
    private static final long timeout = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(MockUtils.class);

    public static CategorySyncOptions getMockCategorySyncOptions() {
        return new CategorySyncOptions("xxxxx",
                "xxxxxxx", "xxxxxxxx",
                LOGGER::error,
                LOGGER::warn);
    }

    public static Category getMockCategory(@Nonnull final Locale locale,
                                           @Nonnull final String name,
                                           @Nonnull final String slug,
                                           @Nonnull final String externalId,
                                           @Nonnull final String description,
                                           @Nonnull final String metaDescription,
                                           @Nonnull final String metaTitle,
                                           @Nonnull final String metaKeywords,
                                           @Nonnull final String orderHint,
                                           @Nonnull final String parentId) {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getName()).thenReturn(LocalizedString.of(locale, name));
        when(oldCategory.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(oldCategory.getExternalId()).thenReturn(externalId);
        when(oldCategory.getDescription()).thenReturn(LocalizedString.of(locale, description));
        when(oldCategory.getMetaDescription()).thenReturn(LocalizedString.of(locale, metaDescription));
        when(oldCategory.getMetaTitle()).thenReturn(LocalizedString.of(locale, metaTitle));
        when(oldCategory.getMetaKeywords()).thenReturn(LocalizedString.of(locale, metaKeywords));
        when(oldCategory.getOrderHint()).thenReturn(orderHint);
        when(oldCategory.getParent()).thenReturn(Reference.of("type", parentId));
        return oldCategory;
    }

    public static List<CategoryDraft> getMockCategoryDrafts() {
        final List<CategoryDraft> categoryDrafts = new ArrayList<>();
        CategoryDraft categoryDraft1 = getMockCategoryDraft(Locale.GERMAN, "draft1", "slug1", "SH663881");
        CategoryDraft categoryDraft2 = getMockCategoryDraft(Locale.GERMAN, "draft2", "slug2", "SH604972");
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);
        return categoryDrafts;
    }

    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nonnull final String externalId) {
        final CategoryDraft mockCategoryDraft = mock(CategoryDraft.class);
        when(mockCategoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(mockCategoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(mockCategoryDraft.getExternalId()).thenReturn(externalId);
        when(mockCategoryDraft.getCustom()).thenReturn(getMockCustomFieldsDraft());
        return mockCategoryDraft;
    }

    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nonnull final String externalId,
                                                     @Nonnull final String description,
                                                     @Nonnull final String metaDescription,
                                                     @Nonnull final String metaTitle,
                                                     @Nonnull final String metaKeywords,
                                                     @Nonnull final String orderHint,
                                                     @Nonnull final String parentId) {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(categoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(categoryDraft.getExternalId()).thenReturn(externalId);
        when(categoryDraft.getDescription()).thenReturn(LocalizedString.of(locale, description));
        when(categoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(locale, metaDescription));
        when(categoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(locale, metaTitle));
        when(categoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(locale, metaKeywords));
        when(categoryDraft.getOrderHint()).thenReturn(orderHint);
        when(categoryDraft.getParent()).thenReturn(Reference.of("type", parentId));
        return categoryDraft;
    }

    public static CustomFieldsDraft getMockCustomFieldsDraft() {
        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        customFieldsJsons.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));
        return CustomFieldsDraft.ofTypeKeyAndJson("StepCategoryTypeKey", customFieldsJsons);
    }
}
