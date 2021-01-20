package com.commercetools.sync.cartdiscounts.helpers;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class CartDiscountBatchValidator
    extends BaseBatchValidator<
        CartDiscountDraft, CartDiscountSyncOptions, CartDiscountSyncStatistics> {

  static final String CART_DISCOUNT_DRAFT_KEY_NOT_SET =
      "CartDiscountDraft with name: %s doesn't have a key. "
          + "Please make sure all cart discount drafts have keys.";
  static final String CART_DISCOUNT_DRAFT_IS_NULL = "CartDiscountDraft is null.";

  public CartDiscountBatchValidator(
      @Nonnull final CartDiscountSyncOptions syncOptions,
      @Nonnull final CartDiscountSyncStatistics syncStatistics) {

    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link List}&lt;{@link CartDiscountDraft}&gt; of drafts this method attempts to
   * validate drafts and collect referenced type keys from the draft and return an {@link
   * ImmutablePair}&lt;{@link Set}&lt;{@link CartDiscountDraft}&gt; ,{@link Set}&lt;{@link
   * String}&gt;&gt; which contains the {@link Set} of valid drafts and referenced type keys.
   *
   * <p>A valid cart discount draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   *   <li>It has a key which is not blank (null/empty)
   * </ol>
   *
   * @param cartDiscountDrafts the cart discount drafts to validate and collect referenced type
   *     keys.
   * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link ProductTypeDraft}&gt;, {@link
   *     Set}&lt;{@link String}&gt;&gt; which contains the {@link Set} of valid drafts and
   *     referenced type keys.
   */
  @Override
  public ImmutablePair<Set<CartDiscountDraft>, Set<String>> validateAndCollectReferencedKeys(
      @Nonnull final List<CartDiscountDraft> cartDiscountDrafts) {
    final Set<String> typeKeys = new HashSet<>();
    final Set<CartDiscountDraft> validDrafts =
        cartDiscountDrafts.stream()
            .filter(this::isValidCartDiscountDraft)
            .peek(
                cartDiscountDraft ->
                    collectReferencedKeyFromCustomFieldsDraft(
                        cartDiscountDraft.getCustom(), typeKeys::add))
            .collect(Collectors.toSet());

    return ImmutablePair.of(validDrafts, typeKeys);
  }

  private boolean isValidCartDiscountDraft(@Nullable final CartDiscountDraft cartDiscountDraft) {
    if (cartDiscountDraft == null) {
      handleError(CART_DISCOUNT_DRAFT_IS_NULL);
    } else if (isBlank(cartDiscountDraft.getKey())) {
      handleError(format(CART_DISCOUNT_DRAFT_KEY_NOT_SET, cartDiscountDraft.getName()));
    } else {
      return true;
    }

    return false;
  }
}
