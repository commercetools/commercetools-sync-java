package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.shoppinglists.helpers.TextLineItemReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TextLineItemReferenceResolverTest {

    private TypeService typeService;

    private TextLineItemReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @BeforeEach
    void setup() {
        typeService = getMockTypeService();

        final ShoppingListSyncOptions syncOptions = ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new TextLineItemReferenceResolver(syncOptions, typeService);
    }

    @Test
    void resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {

        // preparation
        final String customTypeId = "customTypeId";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson(customTypeId, new HashMap<>());

        final TextLineItemDraftBuilder draftBuilder =
            TextLineItemDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, "dummy-custom-key"), 10L)
                                    .custom(customFieldsDraft);

        // test
        final TextLineItemDraftBuilder resolvedDraft = referenceResolver
            .resolveCustomTypeReference(draftBuilder)
            .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCustom()).isNotNull();
        assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);

    }

    @Test
    void resolveCustomTypeReference_WithNonNullKeyOnCustomTypeResId_ShouldResolveCustomTypeReference() {

        // preparation
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeKeyAndJson("customTypeKey", new HashMap<>());

        final TextLineItemDraftBuilder draftBuilder =
            TextLineItemDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, "dummy-custom-key"), 10L)
                                    .custom(customFieldsDraft);

        // test
        final TextLineItemDraftBuilder resolvedDraft = referenceResolver
            .resolveCustomTypeReference(draftBuilder)
            .toCompletableFuture().join();

        // assertion
        assertThat(resolvedDraft.getCustom()).isNotNull();
        assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");

    }

    @Test
    void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        // preparation
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFutureUtils.failed(new SphereException("CTP error on fetch")));

        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft =
            CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());
        final TextLineItemDraftBuilder draftBuilder =
            TextLineItemDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, "dummy-custom-key"), 10L)
                                    .custom(customFieldsDraft);

        // test
        final CompletionStage<TextLineItemDraftBuilder> resolvedDraftCompletionStage = referenceResolver
            .resolveCustomTypeReference(draftBuilder);

        // assertion
        assertThat(resolvedDraftCompletionStage)
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft =
            CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());

        final TextLineItemDraftBuilder draftBuilder =
            TextLineItemDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, "dummy-custom-key"), 10L)
                                    .custom(customFieldsDraft);

        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // test
        final CompletionStage<TextLineItemDraftBuilder> resolvedDraftCompletionStage = referenceResolver
            .resolveCustomTypeReference(draftBuilder);

        // assertion
        final String expectedExceptionMessage = format(FAILED_TO_RESOLVE_CUSTOM_TYPE,
            draftBuilder.getName());

        final String expectedMessageWithCause =
            format("%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));
        ;
        assertThat(resolvedDraftCompletionStage)
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(expectedMessageWithCause);
    }

    @Test
    void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeKeyAndJson("", new HashMap<>());

        final TextLineItemDraftBuilder draftBuilder =
            TextLineItemDraftBuilder.of(LocalizedString.of(Locale.ENGLISH, "dummy-custom-key"), 10L)
                                    .custom(customFieldsDraft);

        // assertion
        assertThat(referenceResolver.resolveCustomTypeReference(draftBuilder))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on TextLineItemDraft"
                    + " with name: '%s'. Reason: %s", LocalizedString.of(Locale.ENGLISH, "dummy-custom-key"),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));

    }
}
