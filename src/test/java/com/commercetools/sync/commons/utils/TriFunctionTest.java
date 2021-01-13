package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.SetSku;
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
    final TriFunction<
            List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
        updateActionFilter = TriFunctionTest::filterEnglishNameChangesOnly;

    final Product oldProduct = readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);
    final LocalizedString oldName = oldProduct.getMasterData().getStaged().getName();

    final ProductDraft newDraftWithNoEnglishNameChange = mock(ProductDraft.class);
    final LocalizedString newNameWithSameEnglishLocale = oldName.plus(Locale.GERMAN, "bar");
    when(newDraftWithNoEnglishNameChange.getName()).thenReturn(newNameWithSameEnglishLocale);

    List<UpdateAction<Product>> updateActions =
        Arrays.asList(ChangeName.of(newNameWithSameEnglishLocale), SetSku.of(1, "sku"));

    assertThat(updateActionFilter.apply(updateActions, newDraftWithNoEnglishNameChange, oldProduct))
        .isEmpty();

    final ProductDraft newDraftWithEnglishNameChange = mock(ProductDraft.class);
    final LocalizedString newNameWithDiffEnglishLocale = LocalizedString.ofEnglish("foo");
    when(newDraftWithEnglishNameChange.getName()).thenReturn(newNameWithDiffEnglishLocale);

    updateActions = Arrays.asList(ChangeName.of(newNameWithDiffEnglishLocale), SetSku.of(1, "sku"));

    final List<UpdateAction<Product>> filteredActions =
        updateActionFilter.apply(updateActions, newDraftWithEnglishNameChange, oldProduct);
    assertThat(filteredActions).isNotEmpty();
    assertThat(filteredActions).isEqualTo(updateActions);
  }

  private static List<UpdateAction<Product>> filterEnglishNameChangesOnly(
      @Nonnull final List<UpdateAction<Product>> updateActions,
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
