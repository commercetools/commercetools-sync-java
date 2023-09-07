package com.commercetools.sync.commons.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.MockUtils;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.TypeService;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssetReferenceResolverTest {
  private TypeService typeService;
  private ProductSyncOptions syncOptions;

  /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    typeService = MockUtils.getMockTypeService();
    syncOptions = ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldCompleteExceptionally() {
    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of().type(builder -> builder.key(customTypeKey)).build();
    final AssetDraftBuilder assetDraftBuilder =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .custom(customFieldsDraft);

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final AssetReferenceResolver assetReferenceResolver =
        new AssetReferenceResolver(syncOptions, typeService);

    // Test and assertion
    final String expectedExceptionMessage =
        String.format(
            AssetReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE, assetDraftBuilder.getKey());
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage,
            String.format(CustomReferenceResolver.TYPE_DOES_NOT_EXIST, customTypeKey));
    assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(expectedMessageWithCause);
  }

  @Test
  void
      resolveCustomTypeReference_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
    final CustomFieldsDraft customFieldsDraft = mock(CustomFieldsDraft.class);
    final TypeResourceIdentifier typeReference =
        TypeResourceIdentifierBuilder.of().id(null).build();
    when(customFieldsDraft.getType()).thenReturn(typeReference);

    final AssetDraftBuilder assetDraftBuilder =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .custom(customFieldsDraft);

    final AssetReferenceResolver assetReferenceResolver =
        new AssetReferenceResolver(syncOptions, typeService);

    assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on AssetDraft with key:'%s'. Reason: %s",
                assetDraftBuilder.getKey(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  public void
      resolveCustomTypeReference_WithNonNullIdOnCustomTypeResId_ShouldResolveCustomTypeReference() {
    // Preparation
    final String customTypeId = UUID.randomUUID().toString();
    final AssetDraftBuilder assetDraftBuilder =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .custom(
                CustomFieldsDraftBuilder.of().type(builder -> builder.id(customTypeId)).build());

    final AssetReferenceResolver assetReferenceResolver =
        new AssetReferenceResolver(syncOptions, typeService);

    // Test
    final AssetDraftBuilder resolvedDraftBuilder =
        assetReferenceResolver
            .resolveCustomTypeReference(assetDraftBuilder)
            .toCompletableFuture()
            .join();

    // Assertion
    assertThat(resolvedDraftBuilder.getCustom()).isNotNull();
    assertThat(resolvedDraftBuilder.getCustom().getType().getId()).isEqualTo(customTypeId);
  }

  @Test
  void resolveCustomTypeReference_WithEmptyKeyOnCustomTypeReference_ShouldCompleteExceptionally() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(""))
            .build();
    final AssetDraftBuilder assetDraftBuilder =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .custom(customFieldsDraft);

    final AssetReferenceResolver assetReferenceResolver =
        new AssetReferenceResolver(syncOptions, typeService);

    assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ReferenceResolutionException.class)
        .withMessageContaining(
            String.format(
                "Failed to resolve custom type reference on AssetDraft with key:'%s'. Reason: %s",
                assetDraftBuilder.getKey(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
    final String customTypeKey = "customTypeKey";
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(customTypeKey))
            .build();
    final AssetDraftBuilder assetDraftBuilder =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .custom(customFieldsDraft);

    final CompletableFuture<Optional<String>> futureThrowingException = new CompletableFuture<>();
    futureThrowingException.completeExceptionally(new Exception("CTP error on fetch"));
    when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingException);

    final AssetReferenceResolver assetReferenceResolver =
        new AssetReferenceResolver(syncOptions, typeService);

    assertThat(assetReferenceResolver.resolveCustomTypeReference(assetDraftBuilder))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(Exception.class)
        .withMessageContaining("CTP error on fetch");
  }

  @Test
  void resolveReferences_WithNoCustomTypeReference_ShouldNotResolveReferences() {
    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .sources(Collections.emptyList())
            .key("assetKey")
            .build();

    final AssetReferenceResolver assetReferenceResolver =
        new AssetReferenceResolver(syncOptions, typeService);

    final AssetDraft referencesResolvedDraft =
        assetReferenceResolver.resolveReferences(assetDraft).toCompletableFuture().join();

    assertThat(referencesResolvedDraft.getCustom()).isNull();
  }
}
