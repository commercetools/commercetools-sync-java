package com.commercetools.sync.products.utils.productupdateactionutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductPublishActionBuilder;
import com.commercetools.api.models.product.ProductUnpublishActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.products.utils.ProductUpdateActionUtils;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildPublishOrUnpublishUpdateActionTest {
  @Test
  void buildPublishOrUnpublishUpdateAction_State_1_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> action =
        getPublishOrUnpublishUpdateAction(false, false, false, false);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_2_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> action =
        getPublishOrUnpublishUpdateAction(false, false, false, true);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_3_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> action =
        getPublishOrUnpublishUpdateAction(false, false, true, false);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_4_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> action =
        getPublishOrUnpublishUpdateAction(false, false, true, true);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_5_ShouldBuildUnpublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(false, true, false, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductUnpublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_6_ShouldBuildUnpublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(false, true, false, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductUnpublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_7_ShouldBuildUnpublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(false, true, true, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductUnpublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_8_ShouldBuildUnpublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(false, true, true, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductUnpublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_9_ShouldBuildPublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(true, false, false, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_10_ShouldBuildPublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(true, false, false, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_11_ShouldBuildPublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(true, false, true, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_12_ShouldBuildPublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(true, false, true, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_13_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> action =
        getPublishOrUnpublishUpdateAction(true, true, false, false);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_14_ShouldBuildPublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(true, true, false, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_15_ShouldBuildPublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(true, true, true, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_16_ShouldBuildPublishUpdateAction() {
    final ProductUpdateAction action =
        getPublishOrUnpublishUpdateAction(true, true, true, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(ProductPublishActionBuilder.of().build());
  }

  @Test
  void
      buildPublishOrUnpublishUpdateAction_WithNullIsPublishedValues_ShouldAssumeTheValuesAsFalse() {
    final Optional<ProductUpdateAction> action =
        getPublishOrUnpublishUpdateAction(null, null, true, true);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  private Optional<ProductUpdateAction> getPublishOrUnpublishUpdateAction(
      @Nullable final Boolean isNewProductDraftPublished,
      @Nullable final Boolean isOldProductPublished,
      final boolean hasNewUpdateActions,
      final boolean hasOldProductStagedChanges) {

    final ProductProjection oldProduct = mock(ProductProjection.class);
    when(oldProduct.getPublished()).thenReturn(isOldProductPublished);
    when(oldProduct.getHasStagedChanges()).thenReturn(hasOldProductStagedChanges);
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getPublish()).thenReturn(isNewProductDraftPublished);
    return ProductUpdateActionUtils.buildPublishOrUnpublishUpdateAction(
        oldProduct, newProductDraft, hasNewUpdateActions);
  }
}
