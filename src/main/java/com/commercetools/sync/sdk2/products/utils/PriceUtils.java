package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static com.commercetools.sync.sdk2.products.utils.CustomFieldsUtils.createCustomFieldsDraft;

import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.DiscountedPrice;
import com.commercetools.api.models.common.DiscountedPriceDraft;
import com.commercetools.api.models.common.DiscountedPriceDraftBuilder;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.common.PriceTier;
import com.commercetools.api.models.common.PriceTierDraft;
import com.commercetools.api.models.common.PriceTierDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class PriceUtils {

  public static List<PriceDraft> createPriceDraft(final List<Price> prices) {
    return createPriceDraft(prices, null);
  }

  public static List<PriceDraft> createPriceDraft(
      final List<Price> prices, @Nullable final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return prices.stream()
        .map(
            price ->
                PriceDraftBuilder.of()
                    .channel(
                        createChannelResourceIdentifier(
                            price.getChannel(), Optional.ofNullable(referenceIdToKeyCache)))
                    .country(price.getCountry())
                    .custom(createCustomFieldsDraft(price.getCustom(), referenceIdToKeyCache))
                    .customerGroup(
                        createCustomerGroupResourceIdentifier(
                            price.getCustomerGroup(), Optional.ofNullable(referenceIdToKeyCache)))
                    .discounted(createDiscountedPriceDraft(price.getDiscounted()))
                    .key(price.getKey())
                    .validFrom(price.getValidFrom())
                    .validUntil(price.getValidUntil())
                    .tiers(createPriceTierDraft(price.getTiers()))
                    .value(price.getValue())
                    .build())
        .collect(Collectors.toList());
  }

  private static ChannelResourceIdentifier createChannelResourceIdentifier(
      @Nullable ChannelReference channelReference,
      Optional<ReferenceIdToKeyCache> referenceIdToKeyCacheOptional) {
    return (ChannelResourceIdentifier)
        referenceIdToKeyCacheOptional
            .map(cache -> getResourceIdentifierWithKey(channelReference, cache))
            .orElse(
                channelReference != null
                    ? ChannelResourceIdentifierBuilder.of().id(channelReference.getId()).build()
                    : null);
  }

  private static CustomerGroupResourceIdentifier createCustomerGroupResourceIdentifier(
      @Nullable CustomerGroupReference customerGroupReference,
      Optional<ReferenceIdToKeyCache> referenceIdToKeyCacheOptional) {
    return (CustomerGroupResourceIdentifier)
        referenceIdToKeyCacheOptional
            .map(cache -> getResourceIdentifierWithKey(customerGroupReference, cache))
            .orElse(
                customerGroupReference != null
                    ? CustomerGroupResourceIdentifierBuilder.of()
                        .id(customerGroupReference.getId())
                        .build()
                    : null);
  }

  private static DiscountedPriceDraft createDiscountedPriceDraft(
      @Nullable DiscountedPrice discountedPrice) {
    return Optional.ofNullable(discountedPrice)
        .map(
            price ->
                DiscountedPriceDraftBuilder.of()
                    .discount(price.getDiscount())
                    .value(price.getValue())
                    .build())
        .orElse(null);
  }

  @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
  public static List<PriceTierDraft> createPriceTierDraft(@Nullable List<PriceTier> priceTiers) {
    if (priceTiers == null) {
      return null;
    }
    return priceTiers.stream()
        .map(
            priceTier ->
                Optional.ofNullable(priceTier)
                    .map(
                        tier ->
                            PriceTierDraftBuilder.of()
                                .minimumQuantity(tier.getMinimumQuantity())
                                .value(tier.getValue())
                                .build())
                    .orElse(null))
        .collect(Collectors.toList());
  }
}
