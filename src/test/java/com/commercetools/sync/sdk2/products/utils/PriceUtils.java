package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.utils.CustomFieldsUtils.createCustomFieldsDraft;

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
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import java.util.List;
import java.util.stream.Collectors;

public class PriceUtils {

  public static List<PriceDraft> createPriceDraft(List<Price> prices) {
    return prices.stream()
        .map(
            price ->
                PriceDraftBuilder.of()
                    .channel(
                        ChannelResourceIdentifierBuilder.of()
                            .id(price.getChannel().getId())
                            .build())
                    .country(price.getCountry())
                    .custom(createCustomFieldsDraft(price.getCustom()))
                    .customerGroup(
                        CustomerGroupResourceIdentifierBuilder.of().id(price.getId()).build())
                    .discounted(createDiscountedPriceDraft(price.getDiscounted()))
                    .key(price.getKey())
                    .validFrom(price.getValidFrom())
                    .validUntil(price.getValidUntil())
                    .tiers(createPriceTierDraft(price.getTiers()))
                    .value(price.getValue())
                    .build())
        .collect(Collectors.toList());
  }

  private static DiscountedPriceDraft createDiscountedPriceDraft(DiscountedPrice discountedPrice) {
    return DiscountedPriceDraftBuilder.of()
        .discount(discountedPrice.getDiscount())
        .value(discountedPrice.getValue())
        .build();
  }

  private static List<PriceTierDraft> createPriceTierDraft(List<PriceTier> priceTiers) {
    return priceTiers.stream()
        .map(
            priceTier ->
                PriceTierDraftBuilder.of()
                    .minimumQuantity(priceTier.getMinimumQuantity())
                    .value(priceTier.getValue())
                    .build())
        .collect(Collectors.toList());
  }
}
