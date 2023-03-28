package com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils;

import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildMoveImageToPositionUpdateActions;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantImagesUpdateActions;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.Image;
import com.commercetools.api.models.common.ImageBuilder;
import com.commercetools.api.models.common.ImageDimensionsBuilder;
import com.commercetools.api.models.product.ProductAddExternalImageAction;
import com.commercetools.api.models.product.ProductAddExternalImageActionImpl;
import com.commercetools.api.models.product.ProductMoveImageToPositionAction;
import com.commercetools.api.models.product.ProductMoveImageToPositionActionBuilder;
import com.commercetools.api.models.product.ProductMoveImageToPositionActionImpl;
import com.commercetools.api.models.product.ProductRemoveImageAction;
import com.commercetools.api.models.product.ProductRemoveImageActionImpl;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildProductVariantImagesUpdateActionsTest {
  @Test
  void buildProductVariantImagesUpdateActions_WithNullImageLists_ShouldNotBuildUpdateActions() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).hasSize(0);
  }

  @Test
  void
      buildProductVariantImagesUpdateActions_WithNullNewImageList_ShouldBuildRemoveImageUpdateActions() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build());
    oldImages.add(
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build());
    when(oldVariant.getImages()).thenReturn(oldImages);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).hasSize(2);
    updateActions.forEach(
        productUpdateAction -> {
          assertThat(productUpdateAction.getAction()).isEqualTo("removeImage");
          assertThat(productUpdateAction).isExactlyInstanceOf(ProductRemoveImageActionImpl.class);
        });
  }

  @Test
  void
      buildProductVariantImagesUpdateActions_WithNullOldImageList_ShouldBuildAddExternalImageActions() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build());
    newImages.add(
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build());
    when(newVariantDraft.getImages()).thenReturn(newImages);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).hasSize(2);
    updateActions.forEach(
        productUpdateAction -> {
          assertThat(productUpdateAction.getAction()).isEqualTo("addExternalImage");
          assertThat(productUpdateAction)
              .isExactlyInstanceOf(ProductAddExternalImageActionImpl.class);
        });
  }

  @Test
  void
      buildProductVariantImagesUpdateActions_WithIdenticalImageLists_ShouldNotBuildUpdateActions() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build());
    oldImages.add(
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build());
    when(oldVariant.getImages()).thenReturn(oldImages);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build());
    newImages.add(
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build());
    when(newVariantDraft.getImages()).thenReturn(newImages);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildProductVariantImagesUpdateActions_WithDifferentImageLists_ShouldBuildAddExternalImageAction() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final Image image =
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build();
    final Image image2 =
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build();

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(image);
    when(oldVariant.getImages()).thenReturn(oldImages);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(image);
    newImages.add(image2);
    when(newVariantDraft.getImages()).thenReturn(newImages);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isExactlyInstanceOf(ProductAddExternalImageActionImpl.class);
    final ProductAddExternalImageAction updateAction =
        (ProductAddExternalImageAction) updateActions.get(0);
    assertThat(updateAction.getImage()).isEqualTo(image2);
  }

  @Test
  void
      buildProductVariantImagesUpdateActions_WithDifferentImageLists_ShouldBuildRemoveImageAction() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final Image image =
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build();
    final Image image2 =
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build();

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(image);
    oldImages.add(image2);
    when(oldVariant.getImages()).thenReturn(oldImages);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(image);
    when(newVariantDraft.getImages()).thenReturn(newImages);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).hasSize(1);
    assertThat(updateActions.get(0)).isExactlyInstanceOf(ProductRemoveImageActionImpl.class);
    final ProductRemoveImageAction updateAction = (ProductRemoveImageAction) updateActions.get(0);
    assertThat(updateAction.getImageUrl()).isEqualTo(image2.getUrl());
  }

  @Test
  void
      buildProductVariantImagesUpdateActions_WithDifferentOrderImageLists_ShouldBuildMovePositionActions() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final Image image =
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build();
    final Image image2 =
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build();

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(image2);
    oldImages.add(image);
    when(oldVariant.getImages()).thenReturn(oldImages);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(image);
    newImages.add(image2);
    when(newVariantDraft.getImages()).thenReturn(newImages);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).hasSize(2);
    updateActions.forEach(
        productUpdateAction -> {
          assertThat(productUpdateAction.getAction()).isEqualTo("moveImageToPosition");
          assertThat(productUpdateAction)
              .isExactlyInstanceOf(ProductMoveImageToPositionActionImpl.class);
        });
    final ProductMoveImageToPositionAction moveImageToPosition =
        (ProductMoveImageToPositionAction) updateActions.get(0);
    final ProductMoveImageToPositionAction moveImage2ToPosition =
        (ProductMoveImageToPositionAction) updateActions.get(1);

    assertThat(moveImageToPosition.getImageUrl()).isEqualTo(image2.getUrl());
    assertThat(moveImageToPosition.getPosition()).isEqualTo(newImages.indexOf(image2));
    assertThat(moveImage2ToPosition.getImageUrl()).isEqualTo(image.getUrl());
    assertThat(moveImage2ToPosition.getPosition()).isEqualTo(newImages.indexOf(image));
  }

  @Test
  void buildProductVariantImagesUpdateActions_WithDifferentOrderAndImages_ShouldBuildActions() {
    final ProductVariant oldVariant = mock(ProductVariant.class);
    final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

    final Image image =
        ImageBuilder.of()
            .url("https://image.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("imageLabel")
            .build();
    final Image image2 =
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build();
    final Image image3 =
        ImageBuilder.of()
            .url("https://image3.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image3Label")
            .build();
    final Image image4 =
        ImageBuilder.of()
            .url("https://image4.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image4Label")
            .build();
    final Image image5 =
        ImageBuilder.of()
            .url("https://image5.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image5Label")
            .build();

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(image5);
    oldImages.add(image2);
    oldImages.add(image);
    oldImages.add(image3);
    when(oldVariant.getImages()).thenReturn(oldImages);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(image4);
    newImages.add(image5);
    newImages.add(image2);
    newImages.add(image3);
    when(newVariantDraft.getImages()).thenReturn(newImages);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
    assertThat(updateActions).hasSize(6);
    final ProductRemoveImageAction removeImage = (ProductRemoveImageAction) updateActions.get(0);
    final ProductAddExternalImageAction addImage =
        (ProductAddExternalImageAction) updateActions.get(1);
    final ProductMoveImageToPositionAction moveImage5ToPosition =
        (ProductMoveImageToPositionAction) updateActions.get(2);
    final ProductMoveImageToPositionAction moveImage2ToPosition =
        (ProductMoveImageToPositionAction) updateActions.get(3);
    final ProductMoveImageToPositionAction moveImage3ToPosition =
        (ProductMoveImageToPositionAction) updateActions.get(4);
    final ProductMoveImageToPositionAction moveImage4ToPosition =
        (ProductMoveImageToPositionAction) updateActions.get(5);

    assertThat(removeImage).isExactlyInstanceOf(ProductRemoveImageActionImpl.class);
    assertThat(removeImage.getImageUrl()).isEqualTo(image.getUrl());

    assertThat(addImage).isExactlyInstanceOf(ProductAddExternalImageActionImpl.class);
    assertThat(addImage.getImage()).isEqualTo(image4);

    assertThat(moveImage5ToPosition)
        .isExactlyInstanceOf(ProductMoveImageToPositionActionImpl.class);
    assertThat(moveImage5ToPosition.getImageUrl()).isEqualTo(image5.getUrl());
    assertThat(moveImage5ToPosition.getPosition()).isEqualTo(newImages.indexOf(image5));

    assertThat(moveImage2ToPosition)
        .isExactlyInstanceOf(ProductMoveImageToPositionActionImpl.class);
    assertThat(moveImage2ToPosition.getImageUrl()).isEqualTo(image2.getUrl());
    assertThat(moveImage2ToPosition.getPosition()).isEqualTo(newImages.indexOf(image2));

    assertThat(moveImage3ToPosition)
        .isExactlyInstanceOf(ProductMoveImageToPositionActionImpl.class);
    assertThat(moveImage3ToPosition.getImageUrl()).isEqualTo(image3.getUrl());
    assertThat(moveImage3ToPosition.getPosition()).isEqualTo(newImages.indexOf(image3));

    assertThat(moveImage4ToPosition)
        .isExactlyInstanceOf(ProductMoveImageToPositionActionImpl.class);
    assertThat(moveImage4ToPosition.getImageUrl()).isEqualTo(image4.getUrl());
    assertThat(moveImage4ToPosition.getPosition()).isEqualTo(newImages.indexOf(image4));
  }

  @Test
  void buildMoveImageToPositionUpdateActions_WithDifferentOrder_ShouldBuildActions() {
    final Image image2 =
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build();
    final Image image3 =
        ImageBuilder.of()
            .url("https://image3.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image3Label")
            .build();
    final Image image4 =
        ImageBuilder.of()
            .url("https://image4.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image4Label")
            .build();
    final Image image5 =
        ImageBuilder.of()
            .url("https://image5.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image5Label")
            .build();

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(image5);
    oldImages.add(image2);
    oldImages.add(image3);
    oldImages.add(image4);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(image4);
    newImages.add(image5);
    newImages.add(image2);
    newImages.add(image3);

    final List<ProductMoveImageToPositionAction> updateActions =
        buildMoveImageToPositionUpdateActions(1, oldImages, newImages);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductMoveImageToPositionActionBuilder.of()
                .imageUrl(image4.getUrl())
                .variantId(1L)
                .position(0L)
                .staged(true)
                .build(),
            ProductMoveImageToPositionActionBuilder.of()
                .imageUrl(image5.getUrl())
                .variantId(1L)
                .position(1L)
                .staged(true)
                .build(),
            ProductMoveImageToPositionActionBuilder.of()
                .imageUrl(image2.getUrl())
                .variantId(1L)
                .position(2L)
                .staged(true)
                .build(),
            ProductMoveImageToPositionActionBuilder.of()
                .imageUrl(image3.getUrl())
                .variantId(1L)
                .position(3L)
                .staged(true)
                .build());
  }

  @Test
  void buildMoveImageToPositionUpdateActions_WithSameOrder_ShouldNotBuildActions() {
    final Image image2 =
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build();
    final Image image3 =
        ImageBuilder.of()
            .url("https://image3.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image3Label")
            .build();
    final Image image4 =
        ImageBuilder.of()
            .url("https://image4.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image4Label")
            .build();
    final Image image5 =
        ImageBuilder.of()
            .url("https://image5.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image5Label")
            .build();

    final List<Image> oldImages = new ArrayList<>();
    oldImages.add(image5);
    oldImages.add(image2);
    oldImages.add(image3);
    oldImages.add(image4);

    final List<Image> newImages = new ArrayList<>();
    newImages.add(image5);
    newImages.add(image2);
    newImages.add(image3);
    newImages.add(image4);

    final List<ProductMoveImageToPositionAction> updateActions =
        buildMoveImageToPositionUpdateActions(1, oldImages, newImages);
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildMoveImageToPositionUpdateActions_WitEmptyArrays_ShouldNotBuildActions() {
    final List<Image> oldImages = new ArrayList<>();
    final List<Image> newImages = new ArrayList<>();

    final List<ProductMoveImageToPositionAction> updateActions =
        buildMoveImageToPositionUpdateActions(1, oldImages, newImages);
    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildMoveImageToPositionUpdateActions_WithDifferentImages_ShouldThrowException() {
    final Image image2 =
        ImageBuilder.of()
            .url("https://image2.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image2Label")
            .build();
    final Image image3 =
        ImageBuilder.of()
            .url("https://image3.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image3Label")
            .build();
    final Image image4 =
        ImageBuilder.of()
            .url("https://image4.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image4Label")
            .build();
    final Image image5 =
        ImageBuilder.of()
            .url("https://image5.url.com")
            .dimensions(ImageDimensionsBuilder.of().h(2).w(2).build())
            .label("image5Label")
            .build();

    final List<Image> oldImages = asList(image5, image2, image3);
    final List<Image> newImagesWithout5 = asList(image4, image2, image3);

    assertThatThrownBy(() -> buildMoveImageToPositionUpdateActions(1, oldImages, newImagesWithout5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(format("[%s] not found", image5));

    final List<Image> newImagesWith4 = new ArrayList<>(newImagesWithout5);
    newImagesWith4.add(image5);

    assertThatThrownBy(() -> buildMoveImageToPositionUpdateActions(1, oldImages, newImagesWith4))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("size")
        .hasMessageContaining(String.valueOf(oldImages.size()))
        .hasMessageContaining(String.valueOf(newImagesWith4.size()));
  }
}
