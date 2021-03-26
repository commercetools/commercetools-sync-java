package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildPublishOrUnpublishUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildPublishOrUnpublishUpdateActionTest {
  @Test
  void buildPublishOrUnpublishUpdateAction_State_1_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> action =
        getPublishOrUnpublishUpdateAction(false, false, false, false);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_2_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> action =
        getPublishOrUnpublishUpdateAction(false, false, false, true);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_3_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> action =
        getPublishOrUnpublishUpdateAction(false, false, true, false);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_4_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> action =
        getPublishOrUnpublishUpdateAction(false, false, true, true);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_5_ShouldBuildUnpublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(false, true, false, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Unpublish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_6_ShouldBuildUnpublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(false, true, false, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Unpublish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_7_ShouldBuildUnpublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(false, true, true, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Unpublish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_8_ShouldBuildUnpublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(false, true, true, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Unpublish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_9_ShouldBuildPublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(true, false, false, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Publish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_10_ShouldBuildPublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(true, false, false, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Publish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_11_ShouldBuildPublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(true, false, true, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Publish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_12_ShouldBuildPublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(true, false, true, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Publish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_13_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> action =
        getPublishOrUnpublishUpdateAction(true, true, false, false);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_14_ShouldBuildPublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(true, true, false, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Publish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_15_ShouldBuildPublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(true, true, true, false).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Publish.of());
  }

  @Test
  void buildPublishOrUnpublishUpdateAction_State_16_ShouldBuildPublishUpdateAction() {
    final UpdateAction<Product> action =
        getPublishOrUnpublishUpdateAction(true, true, true, true).orElse(null);

    assertThat(action).isNotNull();
    assertThat(action).isEqualTo(Publish.of());
  }

  @Test
  void
      buildPublishOrUnpublishUpdateAction_WithNullIsPublishedValues_ShouldAssumeTheValuesAsFalse() {
    final Optional<UpdateAction<Product>> action =
        getPublishOrUnpublishUpdateAction(null, null, true, true);

    assertThat(action).isNotNull();
    assertThat(action).isNotPresent();
  }

  private Optional<UpdateAction<Product>> getPublishOrUnpublishUpdateAction(
      @Nullable final Boolean isNewProductDraftPublished,
      @Nullable final Boolean isOldProductPublished,
      final boolean hasNewUpdateActions,
      final boolean hasOldProductStagedChanges) {

    final ProductProjection oldProduct = mock(ProductProjection.class);
    when(oldProduct.isPublished()).thenReturn(isOldProductPublished);
    when(oldProduct.hasStagedChanges()).thenReturn(hasOldProductStagedChanges);
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.isPublish()).thenReturn(isNewProductDraftPublished);
    return buildPublishOrUnpublishUpdateAction(oldProduct, newProductDraft, hasNewUpdateActions);
  }
}
