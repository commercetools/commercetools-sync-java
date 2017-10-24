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
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryReferenceResolverTest {

    private TypeService typeService;
    private CategoryService categoryService;
    private CategorySyncOptions syncOptions;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        categoryService = getMockCategoryService();
        syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveParentReference_WithNoKeysAsUuidSet_ShouldResolveParentReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);
        final CategoryDraft draftWithResolvedReferences = categoryReferenceResolver
            .resolveParentReference(CategoryDraftBuilder.of(categoryDraft)).toCompletableFuture().join()
            .build();

        assertThat(draftWithResolvedReferences.getParent()).isNotNull();
        assertThat(draftWithResolvedReferences.getParent().getId()).isEqualTo("parentId");
    }

    @Test
    public void resolveCustomTypeReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReferences() {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                  .setAllowUuidKeys(true)
                                                                                  .build();
        final String uuidKey = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
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
        final String parentUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            parentUuid, "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveParentReference(CategoryDraftBuilder.of(categoryDraft))
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getMessage())
                                         .isEqualTo("Failed to resolve parent reference on CategoryDraft with"
                                                 + " key:'key'. Reason: Found a UUID in the id field. Expecting a"
                                                 + " key without a UUID value. If you want to allow UUID values for"
                                                 + " reference keys, please use the setAllowUuidKeys(true) option in"
                                                 + " the sync options.");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveParentReference_WithNonExistentParentCategory_ShouldNotResolveParentReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());
        when(categoryService.fetchCachedCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveParentReference(CategoryDraftBuilder.of(categoryDraft))
                                 .thenApply(CategoryDraftBuilder::build)
                                 .thenAccept(resolvedDraft -> {
                                     assertThat(resolvedDraft.getParent()).isNotNull();
                                     assertThat(resolvedDraft.getParent().getId()).isEqualTo("parentKey");
                                     assertThat(resolvedDraft.getParent().getObj()).isNull();
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentId", "customTypeId", new HashMap<>());

        CompletableFuture<Optional<String>> futureThrowingSphereException = CompletableFuture.supplyAsync(() -> {
            throw new SphereException("bad request");
        });
        when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingSphereException);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause()).isExactlyInstanceOf(SphereException.class);
                                     assertThat(exception.getCause().getMessage()).contains("bad request");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithKeyAsUuidSetAndNotAllowed_ShouldNotResolveCustomTypeReference() {
        final String customTypeUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentKey", customTypeUuid, new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve custom type reference on CategoryDraft"
                                             + " with key:'key'. Reason: Found a UUID in the id field. Expecting"
                                             + " a key without a UUID value. If you want to allow UUID values for"
                                             + " reference keys, please use the setAllowUuidKeys(true) option in"
                                             + " the sync options.");
                                     return null;
                                 }).toCompletableFuture().join();
    }


    @Test
    public void resolveCustomTypeReference_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

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
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());
        when(categoryDraft.getParent()).thenReturn(Category.referenceOfId(""));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveParentReference(CategoryDraftBuilder.of(categoryDraft))
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getMessage())
                                         .isEqualTo("Failed to resolve parent reference on CategoryDraft"
                                             + " with key:'key'. Reason: Key is blank (null/empty) on both expanded"
                                             + " reference object and reference id field.");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveParentReference_WithNullIdOnParentReference_ShouldNotResolveParentReference() {
        final CategoryDraft categoryDraft = mock(CategoryDraft.class);
        when(categoryDraft.getKey()).thenReturn("key");
        final Reference<Category> parentCategoryReference = Category.referenceOfId(null);
        when(categoryDraft.getParent()).thenReturn(parentCategoryReference);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveParentReference(CategoryDraftBuilder.of(categoryDraft))
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getMessage())
                                         .isEqualTo("Failed to resolve parent reference on CategoryDraft"
                                             + " with key:'key'. Reason: Key is blank (null/empty) on both expanded"
                                             + " reference object and reference id field.");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());

        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference =
            ResourceIdentifier.ofId(null);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(categoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve custom type reference on CategoryDraft"
                                                 + " with key:'key'. Reason: Reference 'id' field value is blank"
                                                 + " (null/empty).");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "key",
            "parentKey", "customTypeId", new HashMap<>());

        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference =
            ResourceIdentifier.ofId("");
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(categoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveCustomTypeReference(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve custom type reference on CategoryDraft"
                                                 + " with key:'key'. Reason: Reference 'id' field value is blank"
                                                 + " (null/empty).");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithNoCustomTypeReferenceAndNoParentReference_ShouldResolveReferences() {
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
