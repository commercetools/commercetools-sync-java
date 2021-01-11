package com.commercetools.sync.shoppinglists.helpers;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.shoppinglists.helpers.LineItemReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LineItemReferenceResolverTest {

  private TypeService typeService;

  private LineItemReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    typeService = getMockTypeService();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    referenceResolver = new LineItemReferenceResolver(syncOptions, typeService);
  }

  @Test
  void resolveReferences_WithCustomTypeId_ShouldNotResolveCustomTypeReferenceWithKey() {
    final String customTypeId = "customTypeId";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeIdAndJson(customTypeId, new HashMap<>());

    final LineItemDraft lineItemDraft =
        LineItemDraftBuilder.ofSku("dummy-sku", 10L).custom(customFieldsDraft).build();

    final LineItemDraft resolvedDraft =
        referenceResolver.resolveReferences(lineItemDraft).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustom()).isNotNull();
    assertThat(resolvedDraft.getCustom()).isEqualTo(customFieldsDraft);
  }

  @Test
  void resolveReferences_WithNonNullKeyOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson("customTypeKey", new HashMap<>());

    final LineItemDraft lineItemDraft =
        LineItemDraftBuilder.ofSku("dummy-sku", 10L).custom(customFieldsDraft).build();

    final LineItemDraft resolvedDraft =
        referenceResolver.resolveReferences(lineItemDraft).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustom()).isNotNull();
    assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");
  }

  @Test
  void resolveReferences_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFutureUtils.failed(new SphereException("CTP error on fetch")));

    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());

    final LineItemDraft lineItemDraft =
        LineItemDraftBuilder.ofSku("dummy-sku", 10L).custom(customFieldsDraft).build();

    assertThat(referenceResolver.resolveReferences(lineItemDraft))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(SphereException.class)
        .hasMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveReferences_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, new HashMap<>());

    final LineItemDraft lineItemDraft =
        LineItemDraftBuilder.ofSku("dummy-sku", 10L).custom(customFieldsDraft).build();

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final String expectedExceptionMessage =
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, lineItemDraft.getSku());

    final String expectedMessageWithCause =
        format(
            "%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, customTypeKey));

    assertThat(referenceResolver.resolveReferences(lineItemDraft))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(expectedMessageWithCause);
  }

  @Test
  void resolveReferences_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson("", new HashMap<>());

    final LineItemDraft lineItemDraft =
        LineItemDraftBuilder.ofSku("dummy-sku", 10L).custom(customFieldsDraft).build();

    assertThat(referenceResolver.resolveReferences(lineItemDraft))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(ReferenceResolutionException.class)
        .hasMessage(
            format(
                "Failed to resolve custom type reference on LineItemDraft"
                    + " with SKU: 'dummy-sku'. Reason: %s",
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }
}
