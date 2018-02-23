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
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraftBuilder;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.utils.CompletableFutureUtils.emptyOptionalCompletedFuture;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryReferenceResolverTest {

    private TypeService typeService;
    private CategoryService categoryService;
    private CategorySyncOptions syncOptions;
    
    private static final String CACHED_CATEGORY_ID = UUID.randomUUID().toString();
    private static final String CACHED_CATEGORY_KEY = "someKey";

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        categoryService = mock(CategoryService.class);
        when(categoryService.fetchCachedCategoryId(CACHED_CATEGORY_KEY))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(CACHED_CATEGORY_ID)));
        syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveParentReference_WithNoKeysAsUuidSet_ShouldResolveParentReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);
        final CategoryDraft draftWithResolvedReferences = categoryReferenceResolver
            .resolveParentReference(categoryDraft).toCompletableFuture().join()
            .build();

        assertThat(draftWithResolvedReferences.getParent()).isNotNull();
        assertThat(draftWithResolvedReferences.getParent().getId()).isEqualTo(CACHED_CATEGORY_ID);
    }

    @Test
    public void resolveCustomTypeReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReferences() {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                  .allowUuidKeys(true)
                                                                                  .build();
        final String uuidKey = String.valueOf(UUID.randomUUID());
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            uuidKey, uuidKey, new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(categorySyncOptions, typeService, categoryService);
        final CategoryDraft draftWithResolvedReferences = categoryReferenceResolver
            .resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture().join()
            .build();

        assertThat(draftWithResolvedReferences.getCustom()).isNotNull();
        assertThat(draftWithResolvedReferences.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveParentReference_WithParentKeyAsUuidSetAndNotAllowed_ShouldNotResolveParentReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            UUID.randomUUID().toString(), "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        assertThat(categoryReferenceResolver.resolveParentReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve parent reference on CategoryDraft with"
                + " key:'key'. Reason: Found a UUID in the id field. Expecting a"
                + " key without a UUID value. If you want to allow UUID values for"
                + " reference keys, please use the allowUuidKeys(true) option in"
                + " the sync options.");
    }

    @Ignore("TODO: SHOULD COMPLETE EXCEPTIONALLY. GITHUB ISSUE#219")
    @Test
    public void resolveParentReference_WithNonExistentParentCategory_ShouldNotResolveParentReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, "customTypeId", new HashMap<>());
        when(categoryService.fetchCachedCategoryId(CACHED_CATEGORY_KEY))
            .thenReturn(emptyOptionalCompletedFuture());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveParentReference(categoryDraft)
                                 .thenApply(CategoryDraftBuilder::build)
                                 .thenAccept(resolvedDraft -> {
                                     assertThat(resolvedDraft.getParent()).isNotNull();
                                     assertThat(resolvedDraft.getParent().getId()).isEqualTo(CACHED_CATEGORY_ID);
                                     assertThat(resolvedDraft.getParent().getObj()).isNull();
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, "customTypeId", new HashMap<>());

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingSphereException);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        assertThat(categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    public void resolveCustomTypeReference_WithKeyAsUuidSetAndNotAllowed_ShouldNotResolveCustomTypeReference() {
        final String customTypeUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, customTypeUuid, new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        assertThat(categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on CategoryDraft"
                + " with key:'key'. Reason: Found a UUID in the id field. Expecting"
                + " a key without a UUID value. If you want to allow UUID values for"
                + " reference keys, please use the allowUuidKeys(true) option in"
                + " the sync options.");
    }

    @Test
    public void resolveCustomTypeReference_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            CACHED_CATEGORY_KEY, "customTypeId", new HashMap<>());
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(emptyOptionalCompletedFuture());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
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
        categoryDraft.parent(Category.referenceOfId(""));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        assertThat(categoryReferenceResolver.resolveParentReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve parent reference on CategoryDraft"
                + " with key:'key'. Reason: Key is blank (null/empty) on both expanded"
                + " reference object and reference id field.");
    }

    @Test
    public void resolveParentReference_WithNullIdOnParentReference_ShouldNotResolveParentReference() {
        final CategoryDraftBuilder categoryDraft = CategoryDraftBuilder.of(ofEnglish("foo"), ofEnglish("bar"));
        categoryDraft.key("key");
        categoryDraft.parent(Category.referenceOfId(null));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        assertThat(categoryReferenceResolver.resolveParentReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve parent reference on CategoryDraft"
                + " with key:'key'. Reason: Key is blank (null/empty) on both expanded"
                + " reference object and reference id field.");
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

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        assertThat(categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on CategoryDraft"
                + " with key:'key'. Reason: Reference 'id' field value is blank (null/empty).");
    }

    @Test
    public void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CategoryDraftBuilder categoryDraft = getMockCategoryDraftBuilder(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());

        categoryDraft.custom(CustomFieldsDraft.ofTypeIdAndObjects("", emptyMap()));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        assertThat(categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
            .toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on CategoryDraft"
                + " with key:'key'. Reason: Reference 'id' field value is blank (null/empty).");
    }

    @Test
    public void resolveReferences_WithNoCustomTypeReferenceAndNoParentReference_ShouldNotResolveReferences() {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getName()).thenReturn(LocalizedString.of(Locale.ENGLISH, "myDraft"));
        when(categoryDraft.getKey()).thenReturn("key");

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        final CategoryDraft referencesResolvedDraft = categoryReferenceResolver
            .resolveReferences(categoryDraft)
            .toCompletableFuture().join();

        assertThat(referencesResolvedDraft.getCustom()).isNull();
        assertThat(referencesResolvedDraft.getParent()).isNull();
    }
}
