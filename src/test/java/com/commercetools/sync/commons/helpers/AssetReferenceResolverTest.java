package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssetReferenceResolverTest {
    private TypeService typeService;
    private ProductSyncOptions syncOptions;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveCustomTypeReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReferences() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson(UUID.randomUUID().toString(), new HashMap<>());

        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .custom(customFieldsDraft);

        final AssetReferenceResolver assetReferenceResolver =
            new AssetReferenceResolver(productSyncOptions, typeService);


        final AssetDraftBuilder resolvedDraftBuilder =
            assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder)
                                  .toCompletableFuture().join();

        assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
        assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveCustomTypeReference_WithKeysAndNotAllowedUuids_ShouldResolveReferences() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson("typeKey", new HashMap<>());

        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .custom(customFieldsDraft);

        final AssetReferenceResolver assetReferenceResolver =
            new AssetReferenceResolver(productSyncOptions, typeService);


        final AssetDraftBuilder resolvedDraftBuilder =
            assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder)
                                  .toCompletableFuture().join();

        assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
        assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo("typeId");
    }


    @Test
    public void resolveCustomTypeReference_WithKeyAsUuidSetAndNotAllowed_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson(UUID.randomUUID().toString(), new HashMap<>());

        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on AssetDraft"
                + " with key:'assetKey'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "allowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveCustomTypeReference_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson(customTypeKey, new HashMap<>());
        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getCustom())
                    && Objects.nonNull(resolvedDraft.getCustom().getType())
                    && Objects.equals(resolvedDraft.getCustom().getType().getId(), customTypeKey));
    }

    @Test
    public void resolveCustomTypeReference_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> typeReference = ResourceIdentifier.ofId(null);
        when(customFieldsDraft.getType()).thenReturn(typeReference);

        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on AssetDraft"
                + " with key:'assetKey'. Reason: The value of 'id' field of the Resource Identifier is blank "
                + "(null/empty).");
    }

    @Test
    public void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson("", new HashMap<>());
        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on AssetDraft"
                + " with key:'assetKey'. Reason: The value of 'id' field of the Resource Identifier is blank"
                + " (null/empty).");
    }

    @Test
    public void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson(customTypeKey, new HashMap<>());
        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingSphereException);

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    public void resolveReferences_WithNoCustomTypeReference_ShouldNotResolveReferences() {
        final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                       .key("assetKey")
                                                       .build();

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        final AssetDraft referencesResolvedDraft = assetReferenceResolver.resolveReferences(assetDraft)
                                                                         .toCompletableFuture().join();

        assertThat(referencesResolvedDraft.getCustom()).isNull();
    }
}
