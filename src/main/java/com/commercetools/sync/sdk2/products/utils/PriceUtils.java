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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class PriceUtils {

  public static List<PriceDraft> createPriceDraft(@Nullable final List<Price> prices) {
    return createPriceDraft(prices, null);
  }

  public static List<PriceDraft> createPriceDraft(
      @Nullable final List<Price> prices,
      @Nullable final ReferenceIdToKeyCache referenceIdToKeyCache) {
    if (prices == null) {
      return Collections.emptyList();
    }
    return prices.stream()
        .map(
            price ->
                PriceDraftBuilder.of()
                    .channel(
                        createChannelResourceIdentifier(price.getChannel(), referenceIdToKeyCache))
                    .country(price.getCountry())
                    .custom(createCustomFieldsDraft(price.getCustom(), referenceIdToKeyCache))
                    .customerGroup(
                        createCustomerGroupResourceIdentifier(
                            price.getCustomerGroup(), referenceIdToKeyCache))
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
      @Nullable final ChannelReference channelReference,
      @Nullable final ReferenceIdToKeyCache referenceIdToKeyCache) {
    ChannelResourceIdentifier channelResourceIdentifier = null;
    if (referenceIdToKeyCache != null) {
      channelResourceIdentifier =
          (ChannelResourceIdentifier)
              getResourceIdentifierWithKey(
                  channelReference,
                  referenceIdToKeyCache,
                  (id, key) -> ChannelResourceIdentifierBuilder.of().id(id).key(key).build());
    } else if (channelReference != null) {
      channelResourceIdentifier =
          ChannelResourceIdentifierBuilder.of().id(channelReference.getId()).build();
    }
    return channelResourceIdentifier;
  }

  private static CustomerGroupResourceIdentifier createCustomerGroupResourceIdentifier(
      @Nullable final CustomerGroupReference customerGroupReference,
      @Nullable final ReferenceIdToKeyCache referenceIdToKeyCache) {
    CustomerGroupResourceIdentifier customerGroupResourceIdentifier = null;
    if (referenceIdToKeyCache != null) {
      customerGroupResourceIdentifier =
          (CustomerGroupResourceIdentifier)
              getResourceIdentifierWithKey(
                  customerGroupReference,
                  referenceIdToKeyCache,
                  (id, key) -> CustomerGroupResourceIdentifierBuilder.of().id(id).key(key).build());
    } else if (customerGroupReference != null) {
      customerGroupResourceIdentifier =
          CustomerGroupResourceIdentifierBuilder.of().id(customerGroupReference.getId()).build();
    }
    return customerGroupResourceIdentifier;
  }

  private static DiscountedPriceDraft createDiscountedPriceDraft(
      @Nullable final DiscountedPrice discountedPrice) {
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
  public static List<PriceTierDraft> createPriceTierDraft(
      @Nullable final List<PriceTier> priceTiers) {
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
