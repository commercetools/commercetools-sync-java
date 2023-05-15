package com.commercetools.sync.sdk2.categories.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.categories.CategorySyncMockUtils.getMockCategoryDraftBuilder;
import static com.commercetools.sync.sdk2.categories.helpers.CategoryReferenceResolver.*;
import static com.commercetools.sync.sdk2.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.sdk2.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.sdk2.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static java.lang.String.format;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.sdk2.services.CategoryService;
import com.commercetools.sync.sdk2.services.TypeService;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryReferenceResolverTest {

  private TypeService typeService;
  private CategoryService categoryService;

  private static final String CACHED_CATEGORY_ID = UUID.randomUUID().toString();
  private static final String CACHED_CATEGORY_KEY = "someKey";

  private CategoryReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    typeService = getMockTypeService();
    categoryService = mock(CategoryService.class);
    when(categoryService.fetchCachedCategoryId(CACHED_CATEGORY_KEY))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(CACHED_CATEGORY_ID)));
    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver = new CategoryReferenceResolver(syncOptions, typeService, categoryService);
  }

  @Test
  void resolveAssetsReferences_WithEmptyAssets_ShouldNotResolveAssets() {
    final CategoryDraftBuilder categoryDraftBuilder =
        getMockCategoryDraftBuilder(
                Locale.ENGLISH,
                "myDraft",
                "key",
                CACHED_CATEGORY_KEY,
                "customTypeKey",
                new HashMap<>())
            .assets(emptyList());

    final CategoryDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(categoryDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).isEmpty();
  }

  @Test
  void resolveAssetsReferences_WithNullAssets_ShouldNotResolveAssets() {
    final CategoryDraftBuilder categoryDraftBuilder =
        getMockCategoryDraftBuilder(
            Locale.ENGLISH,
            "myDraft",
            "key",
            CACHED_CATEGORY_KEY,
            "customTypeKey",
            new HashMap<>());

    final CategoryDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(categoryDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).isNull();
  }

  @Test
  void resolveAssetsReferences_WithANullAsset_ShouldNotResolveAssets() {
    final CategoryDraftBuilder categoryDraftBuilder =
        getMockCategoryDraftBuilder(
                Locale.ENGLISH,
                "myDraft",
                "key",
                CACHED_CATEGORY_KEY,
                "customTypeKey",
                new HashMap<>())
            .assets(singletonList(null));

    final CategoryDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(categoryDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).isEmpty();
  }

  @Test
  void resolveAssetsReferences_WithAssetReferences_ShouldResolveAssets() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().key("customTypeKey").build())
            .fields(FieldContainerBuilder.of().values(new HashMap<>()).build())
            .build();

    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .sources(emptyList())
            .name(ofEnglish("assetName"))
            .custom(customFieldsDraft)
            .build();

    final CategoryDraftBuilder categoryDraftBuilder =
        getMockCategoryDraftBuilder(
                Locale.ENGLISH,
                "myDraft",
                "key",
                CACHED_CATEGORY_KEY,
                "customTypeKey",
                new HashMap<>())
            .assets(singletonList(assetDraft));

    final CategoryDraftBuilder resolvedBuilder =
        referenceResolver
            .resolveAssetsReferences(categoryDraftBuilder)
            .toCompletableFuture()
            .join();

    final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
    assertThat(resolvedBuilderAssets).hasSize(1);
    assertThat(resolvedBuilderAssets)
        .allSatisfy(
            resolvedDraft -> {
              assertThat(resolvedDraft).isNotNull();
              assertThat(resolvedDraft.getCustom()).isNotNull();
              assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");
            });
  }

  @Test
  void resolveParentReference_WithExceptionOnFetch_ShouldNotResolveReferences() {
    // Preparation
    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new RuntimeException("CTP error on fetch"));
    when(categoryService.fetchCachedCategoryId(any())).thenReturn(futureThrowingSphereException);

    final CategoryDraftBuilder categoryDraft =
        getMockCategoryDraftBuilder(
            Locale.ENGLISH,
            "myDraft",
            "key",
            "nonExistingCategoryKey",
            "customTypeKey",
            new HashMap<>());

    // Test and assertion
    assertThat(referenceResolver.resolveParentReference(categoryDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(RuntimeException.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveParentReference_WithNonExistentParentCategory_ShouldNotResolveParentReference() {
    // Preparation
    final CategoryDraftBuilder categoryDraft =
        getMockCategoryDraftBuilder(
            Locale.ENGLISH,
            "myDraft",
            "key",
            CACHED_CATEGORY_KEY,
            "customTypeKey",
            new HashMap<>());
    when(categoryService.fetchCachedCategoryId(CACHED_CATEGORY_KEY))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    // Test and assertion
    final String expectedMessageWithCause =
        format(
            FAILED_TO_RESOLVE_PARENT,
            categoryDraft.getKey(),
            format(PARENT_CATEGORY_DOES_NOT_EXIST, CACHED_CATEGORY_KEY));

    assertThat(referenceResolver.resolveParentReference(categoryDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveParentReference_WithEmptyKeyOnParentResId_ShouldNotResolveParentReference() {
    final CategoryDraftBuilder categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .key("key")
            .parent(CategoryResourceIdentifierBuilder.of().key("").build());

    assertThat(referenceResolver.resolveParentReference(categoryDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve parent reference on CategoryDraft with key:'key'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveParentReference_WithNullKeyOnParentResId_ShouldNotResolveParentReference() {
    // Preparation
    final CategoryDraftBuilder categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .key("key")
            .parent(CategoryResourceIdentifierBuilder.of().build());

    assertThat(referenceResolver.resolveParentReference(categoryDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve parent reference on CategoryDraft with key:'key'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveParentReference_WithNonNullIdOnParentResId_ShouldResolveParentReference() {
    // Preparation
    final String parentId = UUID.randomUUID().toString();
    final CategoryDraftBuilder categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .key("key")
            .parent(CategoryResourceIdentifierBuilder.of().id(parentId).build());

    // Test
    final CategoryDraftBuilder resolvedDraftBuilder =
        referenceResolver.resolveParentReference(categoryDraft).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getParent()).isNotNull();
    assertThat(resolvedDraftBuilder.getParent().getId()).isEqualTo(parentId);
  }

  @Test
  void resolveParentReference_WithNonNullKeyOnParentResId_ShouldResolveParentReference() {
    // Preparation
    final CategoryDraftBuilder categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("foo"))
            .slug(ofEnglish("bar"))
            .key("key")
            .parent(CategoryResourceIdentifierBuilder.of().key(CACHED_CATEGORY_KEY).build());

    // Test
    final CategoryDraftBuilder resolvedDraftBuilder =
        referenceResolver.resolveParentReference(categoryDraft).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getParent()).isNotNull();
    assertThat(resolvedDraftBuilder.getParent().getId()).isEqualTo(CACHED_CATEGORY_ID);
  }

  @Test
  void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
    // Preparation
    final CompletableFuture<Optional<String>> futureThrowingSphereException =
        new CompletableFuture<>();
    futureThrowingSphereException.completeExceptionally(new RuntimeException("CTP error on fetch"));
    when(typeService.fetchCachedTypeId(any())).thenReturn(futureThrowingSphereException);

    final CategoryDraftBuilder categoryDraft =
        getMockCategoryDraftBuilder(
            Locale.ENGLISH, "myDraft", "key", null, "customTypeId", new HashMap<>());

    // Test and assertion
    assertThat(referenceResolver.resolveCustomTypeReference(categoryDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(RuntimeException.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    // Preparation
    final CategoryDraftBuilder categoryDraft =
        getMockCategoryDraftBuilder(
            Locale.ENGLISH, "myDraft", "key", null, "customTypeKey", new HashMap<>());

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    // Test and assertion
    final String expectedExceptionMessage =
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, categoryDraft.getKey());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, "customTypeKey"));

    assertThat(referenceResolver.resolveCustomTypeReference(categoryDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
    final CategoryDraftBuilder categoryDraft =
        getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key", null, "", emptyMap());

    assertThat(referenceResolver.resolveCustomTypeReference(categoryDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            format(
                "Failed to resolve custom type reference on CategoryDraft with key:'key'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void
      resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    // Preparation
    final String customTypeId = UUID.randomUUID().toString();
    final CategoryDraftBuilder categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.ofEnglish("myDraft"))
            .slug(LocalizedString.ofEnglish("testSlug"))
            .key("key")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().id(customTypeId).build())
                    .fields(FieldContainerBuilder.of().values(new HashMap<>()).build())
                    .build());

    // Test
    final CategoryDraftBuilder resolvedDraftBuilder =
        referenceResolver.resolveCustomTypeReference(categoryDraft).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
    assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo(customTypeId);
  }

  @Test
  void
      resolveCustomTypeReference_WithNonNullKeyOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    // Preparation
    final CategoryDraftBuilder categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.ofEnglish("myDraft"))
            .slug(LocalizedString.ofEnglish("testSlug"))
            .key("key")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("myTypeKey").build())
                    .fields(FieldContainerBuilder.of().values(new HashMap<>()).build())
                    .build());

    // Test
    final CategoryDraftBuilder resolvedDraftBuilder =
        referenceResolver.resolveCustomTypeReference(categoryDraft).toCompletableFuture().join();

    // Assertion
    assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
    assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo("typeId");
  }

  @Test
  void
      resolveReferences_WithNoCustomTypeReferenceAndNoParentReference_ShouldNotResolveReferences() {
    final CategoryDraft categoryDraft = mock(CategoryDraft.class);
    when(categoryDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "myDraft"));
    when(categoryDraft.getKey()).thenReturn("key");
    when(categoryDraft.getSlug()).thenReturn(LocalizedString.ofEnglish("slug"));

    final CategoryDraft referencesResolvedDraft =
        referenceResolver.resolveReferences(categoryDraft).toCompletableFuture().join();

    assertThat(referencesResolvedDraft.getCustom()).isNull();
    assertThat(referencesResolvedDraft.getParent()).isNull();
  }
}
