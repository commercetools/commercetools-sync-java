package com.commercetools.sync.categories;


import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.commercetools.sync.commons.MockUtils.getMockCustomFieldsDraft;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncMockUtils {
    /**
     * Given a {@code locale}, {@code name}, {@code slug}, {@code externalId}, {@code description},
     * {@code metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code orderHint} and
     * {@code parentId}; this method creates a mock of {@link Category} with all those supplied fields. All the supplied
     * arguments are given as {@link String} and the method internally converts them to their required types.
     * For example, for all the fields that require a {@link LocalizedString} as a value type; the method creates an
     * instance of a {@link LocalizedString} with the given {@link String} and {@link Locale}.
     *
     * @param locale          the locale to create with all the {@link LocalizedString} instances.
     * @param name            the name of the category.
     * @param slug            the slug of the category.
     * @param key             the key of the category.
     * @param externalId      the external id of the category.
     * @param description     the description of the category.
     * @param metaDescription the metadescription of the category.
     * @param metaTitle       the metatitle of the category.
     * @param metaKeywords    the metakeywords of the category.
     * @param orderHint       the orderhint of the category.
     * @param parentId        the parentId of the category.
     * @return an instance {@link Category} with all the given fields set in the given {@link Locale}.
     */
    public static Category getMockCategory(@Nonnull final Locale locale,
                                           @Nonnull final String name,
                                           @Nonnull final String slug,
                                           @Nonnull final String key,
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
        when(oldCategory.getKey()).thenReturn(key);
        when(oldCategory.getExternalId()).thenReturn(externalId);
        when(oldCategory.getDescription()).thenReturn(LocalizedString.of(locale, description));
        when(oldCategory.getMetaDescription()).thenReturn(LocalizedString.of(locale, metaDescription));
        when(oldCategory.getMetaTitle()).thenReturn(LocalizedString.of(locale, metaTitle));
        when(oldCategory.getMetaKeywords()).thenReturn(LocalizedString.of(locale, metaKeywords));
        when(oldCategory.getOrderHint()).thenReturn(orderHint);
        when(oldCategory.getParent()).thenReturn(Category.referenceOfId(parentId));
        return oldCategory;
    }

    /**
     * Creates a list of 2 {@link CategoryDraft}s; the first category draft has the following fields:
     * <ul>
     * <li>name: {"de": "draft1"}</li>
     * <li>slug: {"de": "slug1"}</li>
     * <li>key: "SH663881"</li>
     * </ul>
     *
     * <p>and the other category draft has the following fields:
     * <ul>
     * <li>name: {"de": "draft2"}</li>
     * <li>slug: {"de": "slug2"}</li>
     * <li>key: "SH604972"</li>
     * </ul>
     *
     * @return a list of the of the 2 mocked category drafts.
     */
    public static List<CategoryDraft> getMockCategoryDrafts() {
        final List<CategoryDraft> categoryDrafts = new ArrayList<>();
        CategoryDraft categoryDraft1 = getMockCategoryDraft(Locale.GERMAN, "draft1", "slug1", "SH663881");
        CategoryDraft categoryDraft2 = getMockCategoryDraft(Locale.GERMAN, "draft2", "slug2", "SH604972");
        categoryDrafts.add(categoryDraft1);
        categoryDrafts.add(categoryDraft2);
        return categoryDrafts;
    }

    /**
     * Given a {@code locale}, {@code name}, {@code slug}, {@code key}, {@code externalId}, {@code description},
     * {@code metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code orderHint} and
     * {@code parentId}; this method creates a mock of {@link CategoryDraft} with all those supplied fields. All the
     * supplied arguments are given as {@link String} and the method internally converts them to their required types.
     * For example, for all the fields that require a {@link LocalizedString} as a value type; the method creates an
     * instance of a {@link LocalizedString} with the given {@link String} and {@link Locale}.
     *
     * @param locale          the locale to create with all the {@link LocalizedString} instances.
     * @param name            the name of the category.
     * @param slug            the slug of the category.
     * @param key             the key of the category.
     * @param externalId      the external id of the category.
     * @param description     the description of the category.
     * @param metaDescription the metadescription of the category.
     * @param metaTitle       the metatitle of the category.
     * @param metaKeywords    the metakeywords of the category.
     * @param orderHint       the orderhint of the category.
     * @param parentId        the parentId of the category.
     * @return an instance {@link CategoryDraft} with all the given fields set in the given {@link Locale}.
     */
    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nonnull final String key,
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
        when(categoryDraft.getKey()).thenReturn(key);
        when(categoryDraft.getExternalId()).thenReturn(externalId);
        when(categoryDraft.getDescription()).thenReturn(LocalizedString.of(locale, description));
        when(categoryDraft.getMetaDescription()).thenReturn(LocalizedString.of(locale, metaDescription));
        when(categoryDraft.getMetaTitle()).thenReturn(LocalizedString.of(locale, metaTitle));
        when(categoryDraft.getMetaKeywords()).thenReturn(LocalizedString.of(locale, metaKeywords));
        when(categoryDraft.getOrderHint()).thenReturn(orderHint);
        when(categoryDraft.getParent()).thenReturn(Category.referenceOfId(parentId));
        return categoryDraft;
    }

    /**
     * Given a {@code locale}, {@code name}, {@code slug}, {@code key}, {@code description},
     * {@code metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code orderHint} and
     * {@code parentId}; this method creates a mock of {@link CategoryDraft} with all those supplied fields. All the
     * supplied arguments are given as {@link String} and the method internally converts them to their required types.
     * For example, for all the fields that require a {@link LocalizedString} as a value type; the method creates an
     * instance of a {@link LocalizedString} with the given {@link String} and {@link Locale}.
     *
     * @param locale          the locale to create with all the {@link LocalizedString} instances.
     * @param name            the name of the category.
     * @param key             the key id of the category.
     * @param parentId        the id of the parent category.
     * @param customTypeId    the id of the custom type of category.
     * @param customFields    the custom fields of the category.
     * @return an instance {@link CategoryDraft} with all the given fields set in the given {@link Locale}.
     */
    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String key,
                                                     @Nullable final String parentId,
                                                     @Nonnull final String customTypeId,
                                                     @Nonnull final Map<String, JsonNode> customFields) {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(categoryDraft.getKey()).thenReturn(key);
        when(categoryDraft.getParent()).thenReturn(Category.referenceOfId(parentId));
        final CustomFieldsDraft mockCustomFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson(customTypeId, customFields);
        when(categoryDraft.getCustom()).thenReturn(mockCustomFieldsDraft);
        return categoryDraft;
    }


    /**
     * Given a {@code locale}, {@code name}, {@code slug} and {@code key}; this method creates a mock of
     * {@link CategoryDraft} with all those supplied fields. All the supplied arguments are given as {@link String} and
     * the method internally converts them to their required types. For example, for all the fields that require a
     * {@link LocalizedString} as a value type; the method creates an instance of a {@link LocalizedString} with
     * the given {@link String} and {@link Locale}.
     *
     * @param locale     the locale to create with all the {@link LocalizedString} instances.
     * @param name       the name of the category.
     * @param slug       the slug of the category.
     * @param key        the key of the category.
     * @return an instance {@link CategoryDraft} with all the given fields set in the given {@link Locale}.
     */
    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nullable final String key) {
        final CategoryDraft mockCategoryDraft = mock(CategoryDraft.class);
        when(mockCategoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(mockCategoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(mockCategoryDraft.getKey()).thenReturn(key);
        when(mockCategoryDraft.getCustom()).thenReturn(getMockCustomFieldsDraft());
        return mockCategoryDraft;
    }
}
