package com.commercetools.sync.sdk2.categories;

import static com.commercetools.sync.sdk2.commons.MockUtils.getMockCustomFieldsDraft;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CategorySyncMockUtils {
  /**
   * Given a {@code locale}, {@code name}, {@code slug}, {@code externalId}, {@code description},
   * {@code metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code orderHint} and {@code
   * parentId}; this method creates a mock of {@link io.sphere.sdk.categories.Category} with all
   * those supplied fields. All the supplied arguments are given as {@link String} and the method
   * internally converts them to their required types. For example, for all the fields that require
   * a {@link io.sphere.sdk.models.LocalizedString} as a value type; the method creates an instance
   * of a {@link io.sphere.sdk.models.LocalizedString} with the given {@link String} and {@link
   * java.util.Locale}.
   *
   * @param locale the locale to create with all the {@link io.sphere.sdk.models.LocalizedString}
   *     instances.
   * @param name the name of the category.
   * @param slug the slug of the category.
   * @param key the key of the category.
   * @param externalId the external id of the category.
   * @param description the description of the category.
   * @param metaDescription the metadescription of the category.
   * @param metaTitle the metatitle of the category.
   * @param metaKeywords the metakeywords of the category.
   * @param orderHint the orderhint of the category.
   * @param parentId the parentId of the category.
   * @return an instance {@link io.sphere.sdk.categories.Category} with all the given fields set in
   *     the given {@link java.util.Locale}.
   */
  public static Category getMockCategory(
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final String slug,
      @Nonnull final String key,
      @Nonnull final String externalId,
      @Nonnull final String description,
      @Nonnull final String metaDescription,
      @Nonnull final String metaTitle,
      @Nonnull final String metaKeywords,
      @Nonnull final String orderHint,
      @Nullable final String parentId) {
    final Category category = mock(Category.class);
    when(category.getName())
        .thenReturn(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), name).build());
    when(category.getSlug())
        .thenReturn(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), slug).build());
    when(category.getKey()).thenReturn(key);
    when(category.getId()).thenReturn(externalId);
    when(category.getExternalId()).thenReturn(externalId);
    when(category.getDescription())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), description).build());
    when(category.getMetaDescription())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), metaDescription).build());
    when(category.getMetaTitle())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), metaTitle).build());
    when(category.getMetaKeywords())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), metaKeywords).build());
    when(category.getOrderHint()).thenReturn(orderHint);
    when(category.getParent()).thenReturn(CategoryReferenceBuilder.of().id(parentId).build());
    when(category.toReference())
        .thenReturn(CategoryReferenceBuilder.of().id(UUID.randomUUID().toString()).build());
    return category;
  }

  public static Category getMockCategory(@Nonnull final String id, @Nonnull final String key) {
    final Category category = mock(Category.class);
    when(category.getKey()).thenReturn(key);
    when(category.getId()).thenReturn(id);
    when(category.toReference()).thenReturn(CategoryReferenceBuilder.of().id(id).build());
    return category;
  }

  public static Category mockRoot() {
    return getMockCategory(
        Locale.GERMAN, "root", "root", "root", "root", "root", "root", "root", "root", "100", null);
  }

  /**
   * Creates a list of 2 {@link io.sphere.sdk.categories.CategoryDraft}s; the first category draft
   * has the following fields:
   *
   * <ul>
   *   <li>name: {"de": "draft1"}
   *   <li>slug: {"de": "slug1"}
   *   <li>key: "SH663881"
   * </ul>
   *
   * <p>and the other category draft has the following fields:
   *
   * <ul>
   *   <li>name: {"de": "draft2"}
   *   <li>slug: {"de": "slug2"}
   *   <li>key: "SH604972"
   * </ul>
   *
   * @return a list of the of the 2 mocked category drafts.
   */
  public static List<CategoryDraft> getMockCategoryDrafts() {
    final List<CategoryDraft> categoryDrafts = new ArrayList<>();
    CategoryDraft categoryDraft1 =
        getMockCategoryDraft(Locale.GERMAN, "draft1", "slug1", "SH663881");
    CategoryDraft categoryDraft2 =
        getMockCategoryDraft(Locale.GERMAN, "draft2", "slug2", "SH604972");
    categoryDrafts.add(categoryDraft1);
    categoryDrafts.add(categoryDraft2);
    return categoryDrafts;
  }

  /**
   * Given a {@code locale}, {@code name}, {@code slug}, {@code key}, {@code externalId}, {@code
   * description}, {@code metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code
   * orderHint} and {@code parentId}; this method creates a mock of {@link
   * io.sphere.sdk.categories.CategoryDraft} with all those supplied fields. All the supplied
   * arguments are given as {@link String} and the method internally converts them to their required
   * types. For example, for all the fields that require a {@link
   * io.sphere.sdk.models.LocalizedString} as a value type; the method creates an instance of a
   * {@link io.sphere.sdk.models.LocalizedString} with the given {@link String} and {@link
   * java.util.Locale}.
   *
   * @param locale the locale to create with all the {@link io.sphere.sdk.models.LocalizedString}
   *     instances.
   * @param name the name of the category.
   * @param slug the slug of the category.
   * @param key the key of the category.
   * @param externalId the external id of the category.
   * @param description the description of the category.
   * @param metaDescription the metadescription of the category.
   * @param metaTitle the metatitle of the category.
   * @param metaKeywords the metakeywords of the category.
   * @param orderHint the orderhint of the category.
   * @param parentId the parentId of the category.
   * @return an instance {@link io.sphere.sdk.categories.CategoryDraft} with all the given fields
   *     set in the given {@link java.util.Locale}.
   */
  public static CategoryDraft getMockCategoryDraft(
      @Nonnull final Locale locale,
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
    when(categoryDraft.getName())
        .thenReturn(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), name).build());
    when(categoryDraft.getSlug())
        .thenReturn(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), slug).build());
    when(categoryDraft.getKey()).thenReturn(key);
    when(categoryDraft.getExternalId()).thenReturn(externalId);
    when(categoryDraft.getDescription())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), description).build());
    when(categoryDraft.getMetaDescription())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), metaDescription).build());
    when(categoryDraft.getMetaTitle())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), metaTitle).build());
    when(categoryDraft.getMetaKeywords())
        .thenReturn(
            LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), metaKeywords).build());
    when(categoryDraft.getOrderHint()).thenReturn(orderHint);
    when(categoryDraft.getParent())
        .thenReturn(CategoryResourceIdentifierBuilder.of().id(parentId).build());
    return categoryDraft;
  }

  public static CategoryDraft getMockCategoryDraft(
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final String key,
      @Nullable final String parentKey,
      @Nonnull final String customTypeKey,
      @Nonnull final Map<String, Object> customFields) {
    return getMockCategoryDraftBuilder(locale, name, key, parentKey, customTypeKey, customFields)
        .build();
  }

  /**
   * Given a {@code locale}, {@code name}, {@code slug} and {@code key}; this method creates a mock
   * of {@link io.sphere.sdk.categories.CategoryDraft} with all those supplied fields. All the
   * supplied arguments are given as {@link String} and the method internally converts them to their
   * required types. For example, for all the fields that require a {@link
   * io.sphere.sdk.models.LocalizedString} as a value type; the method creates an instance of a
   * {@link io.sphere.sdk.models.LocalizedString} with the given {@link String} and {@link
   * java.util.Locale}.
   *
   * @param locale the locale to create with all the {@link io.sphere.sdk.models.LocalizedString}
   *     instances.
   * @param name the name of the category.
   * @param slug the slug of the category.
   * @param key the key of the category.
   * @return an instance {@link io.sphere.sdk.categories.CategoryDraft} with all the given fields
   *     set in the given {@link java.util.Locale}.
   */
  public static CategoryDraft getMockCategoryDraft(
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final String slug,
      @Nullable final String key) {
    final CategoryDraft mockCategoryDraft = mock(CategoryDraft.class);
    when(mockCategoryDraft.getName())
        .thenReturn(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), name).build());
    when(mockCategoryDraft.getSlug())
        .thenReturn(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), slug).build());
    when(mockCategoryDraft.getKey()).thenReturn(key);
    when(mockCategoryDraft.getCustom()).thenReturn(getMockCustomFieldsDraft());
    return mockCategoryDraft;
  }

  /**
   * Given a {@code locale}, {@code name}, {@code slug}, {@code key}, {@code description}, {@code
   * metaDescription}, {@code metaTitle}, {@code metaKeywords}, {@code orderHint} and {@code
   * parentId}; this method creates a {@link io.sphere.sdk.categories.CategoryDraftBuilder} with
   * mocked all those supplied fields. All the supplied arguments are given as {@link String} and
   * the method internally converts them to their required types. For example, for all the fields
   * that require a {@link io.sphere.sdk.models.LocalizedString} as a value type; the method creates
   * an instance of a {@link io.sphere.sdk.models.LocalizedString} with the given {@link String} and
   * {@link java.util.Locale}.
   *
   * @param locale the locale to create with all the {@link io.sphere.sdk.models.LocalizedString}
   *     instances.
   * @param name the name of the category.
   * @param key the key id of the category.
   * @param parentKey the key of the parent category.
   * @param customTypeKey the key of the custom type of category.
   * @param customFields the custom fields of the category.
   * @return an instance {@link io.sphere.sdk.categories.CategoryDraftBuilder} with all the given
   *     fields set in the given {@link java.util.Locale}.
   */
  public static CategoryDraftBuilder getMockCategoryDraftBuilder(
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final String key,
      @Nullable final String parentKey,
      @Nonnull final String customTypeKey,
      @Nonnull final Map<String, Object> customFields) {
    return CategoryDraftBuilder.of()
        .name(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), name).build())
        .slug(LocalizedStringBuilder.of().addValue(locale.toLanguageTag(), "testSlug").build())
        .key(key)
        .parent(CategoryResourceIdentifierBuilder.of().key(parentKey).build())
        .custom(
            CustomFieldsDraftBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.key(customTypeKey))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFields))
                .build());
  }
}
