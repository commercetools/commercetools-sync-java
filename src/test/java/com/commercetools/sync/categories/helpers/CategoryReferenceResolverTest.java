package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraftBuilder;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryReferenceResolverTest {

    private TypeService typeService;
    private CategoryService categoryService;

    private static final String CACHED_CATEGORY_ID = UUID.randomUUID().toString();
    private static final String CACHED_CATEGORY_KEY = "someKey";


    private CategoryReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        categoryService = mock(CategoryService.class);
        when(categoryService.fetchCachedCategoryId(CACHED_CATEGORY_KEY))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(CACHED_CATEGORY_ID)));
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new CategoryReferenceResolver(syncOptions, typeService, categoryService);
    }

    @Test
    public void resolveAssetsReferences_WithEmptyAssets_ShouldNotResolveAssets() {
        final CategoryDraftBuilder categoryDraftBuilder =
            getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key", CACHED_CATEGORY_KEY,
                "customTypeId", new HashMap<>())
                .assets(emptyList());

        final CategoryDraftBuilder resolvedBuilder = referenceResolver.resolveAssetsReferences(categoryDraftBuilder)
                                                                      .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isEmpty();
    }

    @Test
    public void resolveAssetsReferences_WithNullAssets_ShouldNotResolveAssets() {
        final CategoryDraftBuilder categoryDraftBuilder =
            getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key", CACHED_CATEGORY_KEY,
                "customTypeId", new HashMap<>())
                .assets(null);

        final CategoryDraftBuilder resolvedBuilder = referenceResolver.resolveAssetsReferences(categoryDraftBuilder)
                                                                      .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isNull();
    }

    @Test
    public void resolveAssetsReferences_WithANullAsset_ShouldNotResolveAssets() {
        final CategoryDraftBuilder categoryDraftBuilder =
            getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key", CACHED_CATEGORY_KEY,
                "customTypeId", new HashMap<>()).assets(singletonList(null));

        final CategoryDraftBuilder resolvedBuilder = referenceResolver.resolveAssetsReferences(categoryDraftBuilder)
                                                                      .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isEmpty();
    }

    @Test
    public void resolveAssetsReferences_WithAssetReferences_ShouldResolveAssets() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson("customTypeId", new HashMap<>());

        final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                       .custom(customFieldsDraft)
                                                       .build();


        final CategoryDraftBuilder categoryDraftBuilder =
            getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key", CACHED_CATEGORY_KEY,
                "customTypeId", new HashMap<>()).assets(singletonList(assetDraft));

        final CategoryDraftBuilder resolvedBuilder = referenceResolver.resolveAssetsReferences(categoryDraftBuilder)
                                                                      .toCompletableFuture().join();

        final List<AssetDraft> resolvedBuilderAssets = resolvedBuilder.getAssets();
        assertThat(resolvedBuilderAssets).isNotEmpty();
        final AssetDraft resolvedAssetDraft = resolvedBuilderAssets.get(0);
        assertThat(resolvedAssetDraft).isNotNull();
        assertThat(resolvedAssetDraft.getCustom()).isNotNull();
        assertThat(resolvedAssetDraft.getCustom().getType().getId()).isEqualTo("typeId");
    }


    @Ignore("TODO: SHOULD COMPLETE EXCEPTIONALLY. GITHUB ISSUE#219")
    @Test
    public void resolveParentReference_WithNonExistentParentCategory_ShouldNotResolveParentReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, "customTypeId", new HashMap<>());
        when(categoryService.fetchCachedCategoryId(CACHED_CATEGORY_KEY))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        referenceResolver.resolveParentReference(categoryDraft)
                                 .thenApply(CategoryDraftBuilder::build)
                                 .thenAccept(resolvedDraft -> {
                                     assertThat(resolvedDraft.getParent()).isNotNull();
                                     assertThat(resolvedDraft.getParent().getId()).isEqualTo(CACHED_CATEGORY_ID);
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, "customTypeId", new HashMap<>());

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingSphereException);

        assertThat(referenceResolver.resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    public void resolveCustomTypeReference_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, "customTypeId", new HashMap<>());
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        referenceResolver.resolveCustomTypeReference(categoryDraft)
                                 .thenApply(CategoryDraftBuilder::build)
                                 .thenAccept(resolvedDraft -> {
                                     assertThat(resolvedDraft.getCustom()).isNotNull();
                                     assertThat(resolvedDraft.getCustom().getType()).isNotNull();
                                     assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("customTypeId");
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveParentReference_WithEmptyIdOnParentReference_ShouldNotResolveParentReference() {
        final CategoryDraftBuilder categoryDraft = CategoryDraftBuilder.of(ofEnglish("foo"), ofEnglish("bar"));
        categoryDraft.key("key");
        categoryDraft.parent(Category.referenceOfId("").toResourceIdentifier());

        assertThat(referenceResolver.resolveParentReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve parent reference on CategoryDraft with key:'key'. Reason: %s",
                BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveParentReference_WithNullIdOnParentReference_ShouldNotResolveParentReference() {
        final CategoryDraftBuilder categoryDraft = CategoryDraftBuilder.of(ofEnglish("foo"), ofEnglish("bar"));
        categoryDraft.key("key");
        categoryDraft.parent(Category.referenceOfId(null).toResourceIdentifier());

        assertThat(referenceResolver.resolveParentReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve parent reference on CategoryDraft with key:'key'. Reason: %s",
                BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCustomTypeReference_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());


        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference =
            ResourceIdentifier.ofId(null);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        categoryDraft.custom(newCategoryCustomFieldsDraft);

        assertThat(referenceResolver.resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on CategoryDraft with key:'key'. Reason: %s",
                BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());

        categoryDraft.custom(CustomFieldsDraft.ofTypeIdAndObjects("", emptyMap()));

        assertThat(referenceResolver.resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on CategoryDraft with key:'key'. Reason: %s",
                BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveReferences_WithNoCustomTypeReferenceAndNoParentReference_ShouldNotResolveReferences() {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "myDraft"));
        when(categoryDraft.getKey()).thenReturn("key");

        final CategoryDraft referencesResolvedDraft = referenceResolver.resolveReferences(categoryDraft)
                                                                       .toCompletableFuture().join();

        assertThat(referencesResolvedDraft.getCustom()).isNull();
        assertThat(referencesResolvedDraft.getParent()).isNull();
    }
}
