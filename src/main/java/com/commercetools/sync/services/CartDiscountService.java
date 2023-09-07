package com.commercetools.sync.services;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CartDiscountService {

  /**
   * Given a {@link java.util.Set} of CartDiscount keys, this method fetches a set of all the
   * CartDiscounts, matching this given set of keys in the CTP project, defined in an injected
   * {@link com.commercetools.api.client.ProjectApiRoot}. A mapping of the key to the id of the
   * fetched CartDiscount is persisted in an in-memory map.
   *
   * @param keys set of CartDiscounts keys to fetch matching CartDiscount by.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Set}&gt; in which the
   *     result of its completion contains a {@link java.util.Set} of all matching CartDiscounts.
   */
  @Nonnull
  CompletionStage<Set<CartDiscount>> fetchMatchingCartDiscountsByKeys(@Nonnull Set<String> keys);

  /**
   * Given a cart discount key, this method fetches a cart discount that matches this given key in
   * the CTP project defined in a potentially injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. If there is no matching cart discount an empty
   * {@link java.util.Optional} will be returned in the returned future. A mapping of the key to the
   * id of the fetched cart discount is persisted in an in-memory map.
   *
   * @param key the key of the product to fetch.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&gt; in which
   *     the result of it's completion contains an {@link java.util.Optional} that contains the
   *     matching {@link CartDiscount} if exists, otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<CartDiscount>> fetchCartDiscount(@Nullable String key);

  /**
   * Given a resource draft of type {@link CartDiscountDraft}, this method attempts to create a
   * resource {@link CartDiscount} based on it in the CTP project defined by the sync options.
   *
   * <p>A completion stage containing an empty {@link java.util.Optional} and the error callback
   * will be triggered in those cases:
   *
   * <ul>
   *   <li>the draft has a blank key
   *   <li>the create request fails on CTP
   * </ul>
   *
   * <p>On the other hand, if the CartDiscount gets created successfully on CTP, then the created
   * CartDiscount's id and key are cached. The method returns a {@link
   * java.util.concurrent.CompletionStage} in which the result of it's completion contains an
   * instance {@link java.util.Optional} of the created `CartDiscount`.
   *
   * @param cartDiscountDraft the resource draft to create a resource based off of.
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the created
   *     {@link CartDiscount} if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<CartDiscount>> createCartDiscount(
      @Nonnull CartDiscountDraft cartDiscountDraft);

  /**
   * Given a {@link CartDiscount} and a {@link java.util.List}&lt;{@link
   * CartDiscountUpdateAction}&gt;, this method issues an update request with these update actions
   * on this {@link CartDiscount} in the CTP project defined in a potentially injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. This method returns {@link
   * java.util.concurrent.CompletionStage}&lt;{@link CartDiscount}&gt; in which the result of its
   * completion contains an instance of the {@link CartDiscount} which was updated in the CTP
   * project.
   *
   * @param cartDiscount the {@link CartDiscount} to update.
   * @param updateActions the update actions to update the {@link CartDiscount} with.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link CartDiscount}&gt; containing as
   *     a result of its completion an instance of the {@link CartDiscount} which was updated in the
   *     CTP project or an exception.
   */
  @Nonnull
  CompletionStage<CartDiscount> updateCartDiscount(
      @Nonnull CartDiscount cartDiscount, @Nonnull List<CartDiscountUpdateAction> updateActions);
}
