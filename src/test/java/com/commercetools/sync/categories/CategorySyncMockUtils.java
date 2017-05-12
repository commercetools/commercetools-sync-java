package com.commercetools.sync.categories;


import com.commercetools.sync.services.CategoryService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.commons.MockUtils.getMockCustomFieldsDraft;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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

    /**
     * Creates a list of 2 {@link CategoryDraft}s; the first category draft has the following fields:
     * <ul>
     * <li>name: {"de": "draft1"}</li>
     * <li>slug: {"de": "slug1"}</li>
     * <li>externalId: "SH663881"</li>
     * </ul>
     *
     * <p>and the other category draft has the following fields:
     * <ul>
     * <li>name: {"de": "draft2"}</li>
     * <li>slug: {"de": "slug2"}</li>
     * <li>externalId: "SH604972"</li>
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
     * Given a {@code locale}, {@code name}, {@code slug}, {@code externalId}, {@code description},
     * {@code metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code orderHint} and
     * {@code parentId}; this method creates a mock of {@link CategoryDraft} with all those supplied fields. All the
     * supplied arguments are given as {@link String} and the method internally converts them to their required types.
     * For example, for all the fields that require a {@link LocalizedString} as a value type; the method creates an
     * instance of a {@link LocalizedString} with the given {@link String} and {@link Locale}.
     *
     * @param locale          the locale to create with all the {@link LocalizedString} instances.
     * @param name            the name of the category.
     * @param slug            the slug of the category.
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

    /**
     * Given a {@code locale}, {@code name}, {@code slug} and {@code externalId}; this method creates a mock of
     * {@link CategoryDraft} with all those supplied fields. All the supplied arguments are given as {@link String} and
     * the method internally converts them to their required types. For example, for all the fields that require a
     * {@link LocalizedString} as a value type; the method creates an instance of a {@link LocalizedString} with
     * the given {@link String} and {@link Locale}.
     *
     * @param locale     the locale to create with all the {@link LocalizedString} instances.
     * @param name       the name of the category.
     * @param slug       the slug of the category.
     * @param externalId the external id of the category.
     * @return an instance {@link CategoryDraft} with all the given fields set in the given {@link Locale}.
     */
    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nullable final String externalId) {
        final CategoryDraft mockCategoryDraft = mock(CategoryDraft.class);
        when(mockCategoryDraft.getName()).thenReturn(LocalizedString.of(locale, name));
        when(mockCategoryDraft.getSlug()).thenReturn(LocalizedString.of(locale, slug));
        when(mockCategoryDraft.getExternalId()).thenReturn(externalId);
        when(mockCategoryDraft.getCustom()).thenReturn(getMockCustomFieldsDraft());
        return mockCategoryDraft;
    }

    /**
     * Creates a mock {@link CategoryService} that returns a mocked {@link Category} instance whenever any of the
     * following methods are called:
     * <ul>
     * <li>{@link CategoryService#fetchCategoryByExternalId(String)}</li>
     * <li>{@link CategoryService#createCategory(CategoryDraft)}</li>
     * <li>{@link CategoryService#updateCategory(Category, List)}</li>
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
        when(categoryService.fetchCategoryByExternalId(anyString())).thenReturn(category);
        when(categoryService.updateCategory(any(), any())).thenReturn(category);
        when(categoryService.createCategory(any())).thenReturn(category);

        return categoryService;
    }
}
