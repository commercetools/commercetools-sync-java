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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.AssetReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetReferenceResolverTest {
    private TypeService typeService;
    private ProductSyncOptions syncOptions;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @BeforeEach
    void setup() {
        typeService = getMockTypeService();
        syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());
        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        // Test and assertion
        final String expectedExceptionMessage =
            format(FAILED_TO_RESOLVE_CUSTOM_TYPE, assetDraftBuilder.getKey());
        final String expectedMessageWithCause =
            format("%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));
        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(expectedMessageWithCause);
    }

    @Test
    void resolveCustomTypeReference_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> typeReference = ResourceIdentifier.ofId(null);
        when(customFieldsDraft.getType()).thenReturn(typeReference);

        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on AssetDraft with key:'%s'. Reason: %s",
                assetDraftBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {
        // Preparation
        final String customTypeId = UUID.randomUUID().toString();
        final AssetDraftBuilder assetDraftBuilder =
            AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                             .key("assetKey")
                             .custom(CustomFieldsDraft.ofTypeIdAndJson(customTypeId, emptyMap()));

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        // Test
        final AssetDraftBuilder resolvedDraftBuilder =
            assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder)
                                  .toCompletableFuture().join();

        // Assertion
        assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
        assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo(customTypeId);
    }

    @Test
    void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeReference_ShouldCompleteExceptionally() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeKeyAndJson("", new HashMap<>());
        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on AssetDraft with key:'%s'. Reason: %s",
                assetDraftBuilder.getKey(), BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());
        final AssetDraftBuilder assetDraftBuilder = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                                     .key("assetKey")
                                                                     .custom(customFieldsDraft);

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingSphereException);

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    void resolveReferences_WithNoCustomTypeReference_ShouldNotResolveReferences() {
        final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                       .key("assetKey")
                                                       .build();

        final AssetReferenceResolver assetReferenceResolver = new AssetReferenceResolver(syncOptions, typeService);

        final AssetDraft referencesResolvedDraft = assetReferenceResolver.resolveReferences(assetDraft)
                                                                         .toCompletableFuture().join();

        assertThat(referencesResolvedDraft.getCustom()).isNull();
    }
}
