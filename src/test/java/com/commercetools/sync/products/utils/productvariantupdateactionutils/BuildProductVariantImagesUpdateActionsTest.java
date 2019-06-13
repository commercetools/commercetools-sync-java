package com.commercetools.sync.products.utils.productvariantupdateactionutils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.ImageDimensions;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.MoveImageToPosition;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildMoveImageToPositionUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantImagesUpdateActions;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildProductVariantImagesUpdateActionsTest {
    @Test
    void buildProductVariantImagesUpdateActions_WithNullImageLists_ShouldNotBuildUpdateActions() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).hasSize(0);
    }

    @Test
    void buildProductVariantImagesUpdateActions_WithNullNewImageList_ShouldBuildRemoveImageUpdateActions() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final List<Image> oldImages = new ArrayList<>();
        oldImages.add(Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel"));
        oldImages.add(Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label"));
        when(oldVariant.getImages()).thenReturn(oldImages);

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).hasSize(2);
        updateActions.forEach(
            productUpdateAction -> {
                assertThat(productUpdateAction.getAction()).isEqualTo("removeImage");
                assertThat(productUpdateAction).isExactlyInstanceOf(RemoveImage.class);
            }
        );
    }

    @Test
    void buildProductVariantImagesUpdateActions_WithNullOldImageList_ShouldBuildAddExternalImageActions() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final List<Image> newImages = new ArrayList<>();
        newImages.add(Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel"));
        newImages.add(Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label"));
        when(newVariantDraft.getImages()).thenReturn(newImages);

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).hasSize(2);
        updateActions.forEach(
            productUpdateAction -> {
                assertThat(productUpdateAction.getAction()).isEqualTo("addExternalImage");
                assertThat(productUpdateAction).isExactlyInstanceOf(AddExternalImage.class);
            }
        );
    }

    @Test
    void buildProductVariantImagesUpdateActions_WithIdenticalImageLists_ShouldNotBuildUpdateActions() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final List<Image> oldImages = new ArrayList<>();
        oldImages.add(Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel"));
        oldImages.add(Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label"));
        when(oldVariant.getImages()).thenReturn(oldImages);

        final List<Image> newImages = new ArrayList<>();
        newImages.add(Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel"));
        newImages.add(Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label"));
        when(newVariantDraft.getImages()).thenReturn(newImages);

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildProductVariantImagesUpdateActions_WithDifferentImageLists_ShouldBuildAddExternalImageAction() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final Image image = Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel");
        final Image image2 = Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label");

        final List<Image> oldImages = new ArrayList<>();
        oldImages.add(image);
        when(oldVariant.getImages()).thenReturn(oldImages);

        final List<Image> newImages = new ArrayList<>();
        newImages.add(image);
        newImages.add(image2);
        when(newVariantDraft.getImages()).thenReturn(newImages);

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(AddExternalImage.class);
        final AddExternalImage updateAction = (AddExternalImage) updateActions.get(0);
        assertThat(updateAction.getImage()).isEqualTo(image2);
    }

    @Test
    void buildProductVariantImagesUpdateActions_WithDifferentImageLists_ShouldBuildRemoveImageAction() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final Image image = Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel");
        final Image image2 = Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label");

        final List<Image> oldImages = new ArrayList<>();
        oldImages.add(image);
        oldImages.add(image2);
        when(oldVariant.getImages()).thenReturn(oldImages);

        final List<Image> newImages = new ArrayList<>();
        newImages.add(image);
        when(newVariantDraft.getImages()).thenReturn(newImages);

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(RemoveImage.class);
        final RemoveImage updateAction = (RemoveImage) updateActions.get(0);
        assertThat(updateAction.getImageUrl()).isEqualTo(image2.getUrl());
    }

    @Test
    void buildProductVariantImagesUpdateActions_WithDifferentOrderImageLists_ShouldBuildMovePositionActions() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final Image image = Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel");
        final Image image2 = Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label");

        final List<Image> oldImages = new ArrayList<>();
        oldImages.add(image2);
        oldImages.add(image);
        when(oldVariant.getImages()).thenReturn(oldImages);

        final List<Image> newImages = new ArrayList<>();
        newImages.add(image);
        newImages.add(image2);
        when(newVariantDraft.getImages()).thenReturn(newImages);

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).hasSize(2);
        updateActions.forEach(
            productUpdateAction -> {
                assertThat(productUpdateAction.getAction()).isEqualTo("moveImageToPosition");
                assertThat(productUpdateAction).isExactlyInstanceOf(MoveImageToPosition.class);
            }
        );
        final MoveImageToPosition moveImageToPosition = (MoveImageToPosition) updateActions.get(0);
        final MoveImageToPosition moveImage2ToPosition = (MoveImageToPosition) updateActions.get(1);

        assertThat(moveImageToPosition.getImageUrl()).isEqualTo(image2.getUrl());
        assertThat(moveImageToPosition.getPosition()).isEqualTo(newImages.indexOf(image2));
        assertThat(moveImage2ToPosition.getImageUrl()).isEqualTo(image.getUrl());
        assertThat(moveImage2ToPosition.getPosition()).isEqualTo(newImages.indexOf(image));
    }

    @Test
    void buildProductVariantImagesUpdateActions_WithDifferentOrderAndImages_ShouldBuildActions() {
        final ProductVariant oldVariant = mock(ProductVariant.class);
        final ProductVariantDraft newVariantDraft = mock(ProductVariantDraft.class);

        final Image image = Image.of("https://image.url.com", ImageDimensions.of(2, 2), "imageLabel");
        final Image image2 = Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label");
        final Image image3 = Image.of("https://image3.url.com", ImageDimensions.of(2, 2), "image3Label");
        final Image image4 = Image.of("https://image4.url.com", ImageDimensions.of(2, 2), "image4Label");
        final Image image5 = Image.of("https://image5.url.com", ImageDimensions.of(2, 2), "image5Label");

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

        final List<UpdateAction<Product>> updateActions =
                buildProductVariantImagesUpdateActions(oldVariant, newVariantDraft);
        assertThat(updateActions).hasSize(6);
        final RemoveImage removeImage = (RemoveImage) updateActions.get(0);
        final AddExternalImage addImage = (AddExternalImage) updateActions.get(1);
        final MoveImageToPosition moveImage5ToPosition = (MoveImageToPosition) updateActions.get(2);
        final MoveImageToPosition moveImage2ToPosition = (MoveImageToPosition) updateActions.get(3);
        final MoveImageToPosition moveImage3ToPosition = (MoveImageToPosition) updateActions.get(4);
        final MoveImageToPosition moveImage4ToPosition = (MoveImageToPosition) updateActions.get(5);

        assertThat(removeImage).isExactlyInstanceOf(RemoveImage.class);
        assertThat(removeImage.getImageUrl()).isEqualTo(image.getUrl());

        assertThat(addImage).isExactlyInstanceOf(AddExternalImage.class);
        assertThat(addImage.getImage()).isEqualTo(image4);

        assertThat(moveImage5ToPosition).isExactlyInstanceOf(MoveImageToPosition.class);
        assertThat(moveImage5ToPosition.getImageUrl()).isEqualTo(image5.getUrl());
        assertThat(moveImage5ToPosition.getPosition()).isEqualTo(newImages.indexOf(image5));

        assertThat(moveImage2ToPosition).isExactlyInstanceOf(MoveImageToPosition.class);
        assertThat(moveImage2ToPosition.getImageUrl()).isEqualTo(image2.getUrl());
        assertThat(moveImage2ToPosition.getPosition()).isEqualTo(newImages.indexOf(image2));

        assertThat(moveImage3ToPosition).isExactlyInstanceOf(MoveImageToPosition.class);
        assertThat(moveImage3ToPosition.getImageUrl()).isEqualTo(image3.getUrl());
        assertThat(moveImage3ToPosition.getPosition()).isEqualTo(newImages.indexOf(image3));

        assertThat(moveImage4ToPosition).isExactlyInstanceOf(MoveImageToPosition.class);
        assertThat(moveImage4ToPosition.getImageUrl()).isEqualTo(image4.getUrl());
        assertThat(moveImage4ToPosition.getPosition()).isEqualTo(newImages.indexOf(image4));
    }

    @Test
    void buildMoveImageToPositionUpdateActions_WithDifferentOrder_ShouldBuildActions() {
        final Image image2 = Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label");
        final Image image3 = Image.of("https://image3.url.com", ImageDimensions.of(2, 2), "image3Label");
        final Image image4 = Image.of("https://image4.url.com", ImageDimensions.of(2, 2), "image4Label");
        final Image image5 = Image.of("https://image5.url.com", ImageDimensions.of(2, 2), "image5Label");

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

        final List<MoveImageToPosition> updateActions =
            buildMoveImageToPositionUpdateActions(1, oldImages, newImages);

        assertThat(updateActions).containsExactlyInAnyOrder(
            MoveImageToPosition.ofImageUrlAndVariantId(image4.getUrl(), 1, 0, true),
            MoveImageToPosition.ofImageUrlAndVariantId(image5.getUrl(), 1, 1, true),
            MoveImageToPosition.ofImageUrlAndVariantId(image2.getUrl(), 1, 2, true),
            MoveImageToPosition.ofImageUrlAndVariantId(image3.getUrl(), 1, 3, true));
    }

    @Test
    void buildMoveImageToPositionUpdateActions_WithSameOrder_ShouldNotBuildActions() {
        final Image image2 = Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label");
        final Image image3 = Image.of("https://image3.url.com", ImageDimensions.of(2, 2), "image3Label");
        final Image image4 = Image.of("https://image4.url.com", ImageDimensions.of(2, 2), "image4Label");
        final Image image5 = Image.of("https://image5.url.com", ImageDimensions.of(2, 2), "image5Label");

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

        final List<MoveImageToPosition> updateActions =
                buildMoveImageToPositionUpdateActions(1, oldImages, newImages);
        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildMoveImageToPositionUpdateActions_WitEmptyArrays_ShouldNotBuildActions() {
        final List<Image> oldImages = new ArrayList<>();
        final List<Image> newImages = new ArrayList<>();

        final List<MoveImageToPosition> updateActions =
                buildMoveImageToPositionUpdateActions(1, oldImages, newImages);
        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildMoveImageToPositionUpdateActions_WithDifferentImages_ShouldThrowException() {
        final Image image2 = Image.of("https://image2.url.com", ImageDimensions.of(2, 2), "image2Label");
        final Image image3 = Image.of("https://image3.url.com", ImageDimensions.of(2, 2), "image3Label");
        final Image image4 = Image.of("https://image4.url.com", ImageDimensions.of(2, 2), "image4Label");
        final Image image5 = Image.of("https://image5.url.com", ImageDimensions.of(2, 2), "image5Label");

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
