package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategoryReferenceResolver;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
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
import static org.mockito.Matchers.anyString;
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
    public void resolveReferences_WithNoUuidSet_ShouldResolveParentReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);
        final CategoryDraft draftWithResolvedReferences = categoryReferenceResolver
            .resolveReferences(categoryDraft).toCompletableFuture().join();

        assertThat(draftWithResolvedReferences.getParent()).isNotNull();
        assertThat(draftWithResolvedReferences.getParent().getId()).isEqualTo("parentId");
    }

    @Test
    public void resolveReferences_WithUuidSetAndAllowed_ShouldNotResolveReferences() {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                  .setAllowUuid(true)
                                                                                  .build();
        final String parentUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            parentUuid, "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(categorySyncOptions, typeService, categoryService);
        final CategoryDraft draftWithResolvedReferences = categoryReferenceResolver
            .resolveReferences(categoryDraft).toCompletableFuture().join();

        assertThat(draftWithResolvedReferences.getParent()).isNotNull();
        assertThat(draftWithResolvedReferences.getParent().getId()).isEqualTo("parentId");

        assertThat(draftWithResolvedReferences.getCustom()).isNotNull();
        assertThat(draftWithResolvedReferences.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveReferences_WithParentUuidSetAndNotAllowed_ShouldNotResolveParentReference() {
        final String parentUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            parentUuid, "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve parent reference. Reason: Found a UUID in"
                                             + " the id field. Expecting a key without a UUID value. If you want to"
                                             + " allow UUID values for reference keys, please use the"
                                             + " setAllowUuid(true) option in the sync options.");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithNonExistentParentCategory_ShouldNotResolveParentReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());
        when(categoryService.fetchCachedCategoryId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .thenAccept(resolvedDraft -> {
                                     assertThat(resolvedDraft.getParent()).isNotNull();
                                     assertThat(resolvedDraft.getParent().getId()).isEqualTo("parentExternalId");
                                     assertThat(resolvedDraft.getParent().getObj()).isNull();
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithExceptionOnFetch_ShouldNotResolveReferences() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentId", "customTypeId", new HashMap<>());

        CompletableFuture<Optional<String>> futureThrowingSphereException = CompletableFuture.supplyAsync(() -> {
            throw new SphereException("bad request");
        });
        when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingSphereException);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(SphereException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .contains("bad request");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithCustomTypeUuidSetAndNotAllowed_ShouldNotResolveCustomTypeReference() {
        final String customTypeUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", customTypeUuid, new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve custom type reference. Reason: Found a UUID in "
                                             + "the id field. Expecting a key without a UUID value. If you want to "
                                             + "allow UUID values for reference keys, please use the setAllowUuid(true)"
                                             + " option in the sync options.");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithNoUuidSet_ShouldResolveCustomTypeReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        final CategoryDraft draftWithResolvedReferences = categoryReferenceResolver
            .resolveReferences(categoryDraft).toCompletableFuture().join();

        assertThat(draftWithResolvedReferences.getCustom()).isNotNull();
        assertThat(draftWithResolvedReferences.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveReferences_WithNonExistentCustomType_ShouldResolveCustomTypeReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .thenAccept(resolvedDraft -> {
                                     assertThat(resolvedDraft.getCustom()).isNotNull();
                                     assertThat(resolvedDraft.getCustom().getType()).isNotNull();
                                     assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("customTypeId");
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithEmptyIdOnParentReference_ShouldNotResolveParentReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());
        when(categoryDraft.getParent()).thenReturn(Reference.of("type", ""));

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve parent reference. Reason: Key is blank "
                                             + "(null/empty) on both expanded reference object and reference id "
                                             + "field.");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());

        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference =
            ResourceIdentifier.ofId(null);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(categoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve custom type reference. Reason: Reference 'id' "
                                             + "field value is blank (null/empty).");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());

        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference =
            ResourceIdentifier.ofId("");
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(categoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        final CategoryReferenceResolver categoryReferenceResolver =
            new CategoryReferenceResolver(syncOptions, typeService, categoryService);

        categoryReferenceResolver.resolveReferences(categoryDraft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve custom type reference. Reason: Reference 'id' "
                                             + "field value is blank (null/empty).");
                                     return null;
                                 }).toCompletableFuture().join();
    }
}
