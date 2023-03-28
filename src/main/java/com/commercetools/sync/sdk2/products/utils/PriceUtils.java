package com.commercetools.sync.sdk2.products.utils;

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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class PriceUtils {

  public static List<PriceDraft> createPriceDraft(List<Price> prices) {
    return prices.stream()
        .map(
            price ->
                PriceDraftBuilder.of()
                    .channel(createChannelResourceIdentifier(price.getChannel()))
                    .country(price.getCountry())
                    .custom(CustomFieldsUtils.createCustomFieldsDraft(price.getCustom()))
                    .customerGroup(createCustomerGroupResourceIdentifier(price.getCustomerGroup()))
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
      @Nullable ChannelReference channelReference) {
    return Optional.ofNullable(channelReference)
        .map(reference -> ChannelResourceIdentifierBuilder.of().id(reference.getId()).build())
        .orElse(null);
  }

  private static CustomerGroupResourceIdentifier createCustomerGroupResourceIdentifier(
      @Nullable CustomerGroupReference customerGroupReference) {
    return Optional.ofNullable(customerGroupReference)
        .map(reference -> CustomerGroupResourceIdentifierBuilder.of().id(reference.getId()).build())
        .orElse(null);
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
