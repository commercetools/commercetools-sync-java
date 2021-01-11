package com.commercetools.sync.internals.helpers;

import static java.util.Optional.ofNullable;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 *
 * <p>Since there is no one unique identifier for prices on the commercetools API, we uniquely
 * identify a price as a composite id of the following fields: countryCode, currencyCode, channelId,
 * customerGroupId, validFrom and validUntil. This class models such composite key.
 */
public final class PriceCompositeId {

  private final CountryCode countryCode;
  private final String currencyCode;
  private final String channelId;
  private final String customerGroupId;
  private final ZonedDateTime validFrom;
  private final ZonedDateTime validUntil;

  private PriceCompositeId(
      @Nonnull final String currencyCode,
      @Nullable final CountryCode countryCode,
      @Nullable final String channelId,
      @Nullable final String customerGroupId,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil) {
    this.currencyCode = currencyCode;
    this.countryCode = countryCode;
    this.channelId = channelId;
    this.customerGroupId = customerGroupId;
    this.validFrom = validFrom;
    this.validUntil = validUntil;
  }

  /**
   * Given a {@link PriceDraft}, creates a {@link PriceCompositeId} using the following fields from
   * the supplied {@link PriceDraft}:
   *
   * <ol>
   *   <li>{@link PriceCompositeId#currencyCode}: CurrencyCode of {@link PriceDraft#getValue()}
   *   <li>{@link PriceCompositeId#countryCode}: {@link PriceDraft#getCountry()}
   *   <li>{@link PriceCompositeId#channelId}: id of {@link PriceDraft#getChannel()}
   *   <li>{@link PriceCompositeId#customerGroupId}: id of {@link PriceDraft#getCustomerGroup()}
   *   <li>{@link PriceCompositeId#validFrom}: {@link PriceDraft#getValidFrom()}
   *   <li>{@link PriceCompositeId#validUntil}: {@link PriceDraft#getValidUntil()}
   * </ol>
   *
   * @param priceDraft a composite id is built using its fields.
   * @return a composite id comprised of the fields of the supplied {@link PriceDraft}.
   */
  @Nonnull
  public static PriceCompositeId of(@Nonnull final PriceDraft priceDraft) {
    return new PriceCompositeId(
        priceDraft.getValue().getCurrency().getCurrencyCode(),
        priceDraft.getCountry(),
        ofNullable(priceDraft.getChannel()).map(ResourceIdentifier::getId).orElse(null),
        ofNullable(priceDraft.getCustomerGroup()).map(Reference::getId).orElse(null),
        priceDraft.getValidFrom(),
        priceDraft.getValidUntil());
  }

  /**
   * Given a {@link Price}, creates a {@link PriceCompositeId} using the following fields from the
   * supplied {@link Price}:
   *
   * <ol>
   *   <li>{@link PriceCompositeId#currencyCode}: CurrencyCode of {@link Price#getValue()}
   *   <li>{@link PriceCompositeId#countryCode}: {@link Price#getCountry()}
   *   <li>{@link PriceCompositeId#channelId}: id of {@link Price#getChannel()}
   *   <li>{@link PriceCompositeId#customerGroupId}: id of {@link Price#getCustomerGroup()}
   *   <li>{@link PriceCompositeId#validFrom}: {@link Price#getValidFrom()}
   *   <li>{@link PriceCompositeId#validUntil}: {@link Price#getValidUntil()}
   * </ol>
   *
   * @param price a composite id is built using its fields.
   * @return a composite id comprised of the fields of the supplied {@link Price}.
   */
  @Nonnull
  public static PriceCompositeId of(@Nonnull final Price price) {
    return new PriceCompositeId(
        price.getValue().getCurrency().getCurrencyCode(),
        price.getCountry(),
        ofNullable(price.getChannel()).map(Reference::getId).orElse(null),
        ofNullable(price.getCustomerGroup()).map(Reference::getId).orElse(null),
        price.getValidFrom(),
        price.getValidUntil());
  }

  public CountryCode getCountryCode() {
    return countryCode;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public String getChannelId() {
    return channelId;
  }

  public String getCustomerGroupId() {
    return customerGroupId;
  }

  public ZonedDateTime getValidFrom() {
    return validFrom;
  }

  public ZonedDateTime getValidUntil() {
    return validUntil;
  }

  @Override
  public boolean equals(final Object otherObject) {
    if (this == otherObject) {
      return true;
    }
    if (!(otherObject instanceof PriceCompositeId)) {
      return false;
    }
    final PriceCompositeId that = (PriceCompositeId) otherObject;
    return countryCode == that.countryCode
        && Objects.equals(currencyCode, that.currencyCode)
        && Objects.equals(channelId, that.channelId)
        && Objects.equals(customerGroupId, that.customerGroupId)
        && Objects.equals(validFrom, that.validFrom)
        && Objects.equals(validUntil, that.validUntil);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        countryCode, currencyCode, channelId, customerGroupId, validFrom, validUntil);
  }
}
