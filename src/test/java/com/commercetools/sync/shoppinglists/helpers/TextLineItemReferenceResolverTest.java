package com.commercetools.sync.shoppinglists.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextLineItemReferenceResolverTest {

  private TypeService typeService;

  private TextLineItemReferenceResolver referenceResolver;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    typeService = MockUtils.getMockTypeService();

    final ShoppingListSyncOptions syncOptions =
        ShoppingListSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    referenceResolver = new TextLineItemReferenceResolver(syncOptions, typeService);
  }

  @Test
  void resolveReferences_WithCustomTypeId_ShouldNotResolveCustomTypeReferenceWithKey() {
    final String customTypeId = "customTypeId";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("dummy-custom-key"))
            .quantity(10L)
            .custom(customFieldsDraft)
            .build();

    final TextLineItemDraft resolvedDraft =
        referenceResolver.resolveReferences(textLineItemDraft).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustom()).isNotNull();
    assertThat(resolvedDraft.getCustom()).isEqualTo(customFieldsDraft);
  }

  @Test
  void resolveReferences_WithNonNullKeyOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(
                typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("customTypeKey"))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("dummy-custom-key"))
            .quantity(10L)
            .custom(customFieldsDraft)
            .build();

    final TextLineItemDraft resolvedDraft =
        referenceResolver.resolveReferences(textLineItemDraft).toCompletableFuture().join();

    assertThat(resolvedDraft.getCustom()).isNotNull();
    assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");
  }

  @Test
  void resolveReferences_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFutureUtils.failed(ExceptionUtils.createBadGatewayException()));

    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(customTypeKey))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("dummy-custom-key"))
            .quantity(10L)
            .custom(customFieldsDraft)
            .build();

    assertThat(referenceResolver.resolveReferences(textLineItemDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class)
        .withMessageContaining("test");
  }

  @Test
  void resolveReferences_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(customTypeKey))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("dummy-custom-key"))
            .quantity(10L)
            .custom(customFieldsDraft)
            .build();

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final String expectedExceptionMessage =
        String.format(
            TextLineItemReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE,
            textLineItemDraft.getName());

    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage,
            String.format(CustomReferenceResolver.TYPE_DOES_NOT_EXIST, customTypeKey));

    assertThat(referenceResolver.resolveReferences(textLineItemDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void resolveReferences_WithEmptyKeyOnCustomTypeResId_ShouldCompleteExceptionally() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(""))
            .fields(fieldContainerBuilder -> fieldContainerBuilder.values(new HashMap<>()))
            .build();

    final TextLineItemDraft textLineItemDraft =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("dummy-custom-key"))
            .quantity(10L)
            .custom(customFieldsDraft)
            .build();

    assertThat(referenceResolver.resolveReferences(textLineItemDraft))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on TextLineItemDraft"
                    + " with name: '%s'. Reason: %s",
                ofEnglish("dummy-custom-key"),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }
}
