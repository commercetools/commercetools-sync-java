package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class TriFunctionTest {

  @Test
  void apply_WithUpdateActionLocaleFilter_ShouldReturnCorrectResult() {
    final TriFunction<List<ProductUpdateAction>, ProductDraft, Product, List<ProductUpdateAction>>
        updateActionFilter = TriFunctionTest::filterEnglishNameChangesOnly;

    final Product oldProduct = createObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);
    final LocalizedString oldName = oldProduct.getMasterData().getStaged().getName();

    final ProductDraft newDraftWithNoEnglishNameChange = mock(ProductDraft.class);
    final LocalizedString newNameWithSameEnglishLocale = oldName.plus(Locale.GERMAN, "bar");
    when(newDraftWithNoEnglishNameChange.getName()).thenReturn(newNameWithSameEnglishLocale);

    final ProductChangeNameAction changeNameAction =
        ProductChangeNameActionBuilder.of().name(newNameWithSameEnglishLocale).build();
    final ProductSetSkuAction setSkuAction =
        ProductSetSkuActionBuilder.of().variantId(1L).sku("sku").build();

    List<ProductUpdateAction> updateActions = Arrays.asList(changeNameAction, setSkuAction);

    assertThat(updateActionFilter.apply(updateActions, newDraftWithNoEnglishNameChange, oldProduct))
        .isEmpty();

    final ProductDraft newDraftWithEnglishNameChange = mock(ProductDraft.class);
    final LocalizedString newNameWithDiffEnglishLocale = LocalizedString.ofEnglish("foo");
    when(newDraftWithEnglishNameChange.getName()).thenReturn(newNameWithDiffEnglishLocale);

    final ProductChangeNameAction changeNameDiffAction =
        ProductChangeNameActionBuilder.of().name(newNameWithSameEnglishLocale).build();

    updateActions = Arrays.asList(changeNameDiffAction, setSkuAction);

    final List<ProductUpdateAction> filteredActions =
        updateActionFilter.apply(updateActions, newDraftWithEnglishNameChange, oldProduct);
    assertThat(filteredActions).isNotEmpty();
    assertThat(filteredActions).isEqualTo(updateActions);
  }

  private static List<ProductUpdateAction> filterEnglishNameChangesOnly(
      @Nonnull final List<ProductUpdateAction> updateActions,
      @Nonnull final ProductDraft newDraft,
      @Nonnull final Product oldProduct) {
    return updateActions.stream()
        .filter(
            action -> {
              final String englishOldName =
                  oldProduct.getMasterData().getStaged().getName().get(Locale.ENGLISH);
              final String englishNewName = newDraft.getName().get(Locale.ENGLISH);
              return !Objects.equals(englishOldName, englishNewName);
            })
        .collect(Collectors.toList());
  }

  @Test
  void apply_WithIntegerParams_ShouldReturnCorrectResult() {
    final TriFunction<Integer, Integer, Integer, String> sumToString =
        (num1, num2, num3) -> {
          num1 = num1 != null ? num1 : 0;
          num2 = num2 != null ? num2 : 0;
          num3 = num3 != null ? num3 : 0;
          return String.format("The sum is: %d", num1 + num2 + num3);
        };

    final String threeOnes = sumToString.apply(1, 1, 1);
    final String threeNulls = sumToString.apply(null, null, null);

    assertThat(threeOnes).isEqualTo(String.format("The sum is: %d", 3));
    assertThat(threeNulls).isEqualTo(String.format("The sum is: %d", 0));
  }
}
