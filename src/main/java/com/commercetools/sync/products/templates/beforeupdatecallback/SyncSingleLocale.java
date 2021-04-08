package com.commercetools.sync.products.templates.beforeupdatecallback;

import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.LocalizedStringEntry;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SyncSingleLocale {
  /**
   * Takes in a {@link List} of product update actions that was built from comparing a {@code
   * newProductDraft} and an {@code oldProduct} and maps the update actions so that only
   * localizations with value {@link Locale#FRENCH} are synced and all the other locales are left
   * untouched.
   *
   * @param updateActions the update actions built from comparing {@code newProductDraft} and {@code
   *     oldProduct}.
   * @param newProductDraft the new {@link ProductDraft} being synced.
   * @param oldProduct the old existing {@link Product}.
   * @return a new list of update actions that corresponds to changes on French localizations only.
   */
  @Nonnull
  public static List<UpdateAction<Product>> syncFrenchDataOnly(
      @Nonnull final List<UpdateAction<Product>> updateActions,
      @Nonnull final ProductDraft newProductDraft,
      @Nonnull final ProductProjection oldProduct) {

    final List<Optional<UpdateAction<Product>>> optionalActions =
        updateActions.stream()
            .map(
                action ->
                    filterSingleLocalization(action, newProductDraft, oldProduct, Locale.FRENCH))
            .collect(toList());

    return filterEmptyOptionals(optionalActions);
  }

  /**
   * Takes a product update action, a new {@link ProductDraft}, an old existing {@link Product} and
   * a {@link Locale}. This method checks if the update action is either one of the following update
   * actions:
   *
   * <ul>
   *   <li>{@link ChangeName}
   *   <li>{@link SetMetaKeywords}
   *   <li>{@link SetMetaDescription}
   *   <li>{@link SetMetaTitle}
   *   <li>{@link ChangeSlug}
   *   <li>{@link SetDescription}
   * </ul>
   *
   * If the update action is one of the aforementioned update actions, the method checks if the
   * change in localization generated from the update action corresponds to a change in the supplied
   * {@link Locale} value and maps the update action so that it only corresponds to such change in
   * the localized field. Namely, if the change was only in another locale value other than the
   * supplied one, then no update action is needed. On the other hand, if the change was in the
   * supplied locale value then the update action is modified so that it only corresponds to the
   * change in that locale value.
   *
   * @param updateAction the update action built from comparing {@code newProductDraft} and {@code
   *     oldProduct}.
   * @param newProductDraft the new {@link ProductDraft} being synced.
   * @param oldProduct the old existing {@link Product}.
   * @param locale the locale value to only compare and map the update action to accordingly.
   * @return an optional containing the mapped update action or empty value if an update action is
   *     not needed.
   */
  @Nonnull
  private static Optional<UpdateAction<Product>> filterSingleLocalization(
      @Nonnull final UpdateAction<Product> updateAction,
      @Nonnull final ProductDraft newProductDraft,
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final Locale locale) {

    if (updateAction instanceof ChangeName) {
      return filterLocalizedField(
          newProductDraft,
          oldProduct,
          locale,
          ProductDraft::getName,
          (p) -> oldProduct.getName(),
          ChangeName::of);
    }
    if (updateAction instanceof SetDescription) {
      return filterLocalizedField(
          newProductDraft,
          oldProduct,
          locale,
          ProductDraft::getDescription,
          ProductProjection::getDescription,
          SetDescription::of);
    }
    if (updateAction instanceof ChangeSlug) {
      return filterLocalizedField(
          newProductDraft,
          oldProduct,
          locale,
          ProductDraft::getSlug,
          ProductProjection::getSlug,
          ChangeSlug::of);
    }
    if (updateAction instanceof SetMetaTitle) {
      return filterLocalizedField(
          newProductDraft,
          oldProduct,
          locale,
          ProductDraft::getMetaTitle,
          ProductProjection::getMetaTitle,
          SetMetaTitle::of);
    }
    if (updateAction instanceof SetMetaDescription) {
      return filterLocalizedField(
          newProductDraft,
          oldProduct,
          locale,
          ProductDraft::getMetaDescription,
          ProductProjection::getMetaDescription,
          SetMetaDescription::of);
    }
    if (updateAction instanceof SetMetaKeywords) {
      return filterLocalizedField(
          newProductDraft,
          oldProduct,
          locale,
          ProductDraft::getMetaKeywords,
          ProductProjection::getMetaKeywords,
          SetMetaKeywords::of);
    }
    return Optional.of(updateAction);
  }

  /**
   * Checks if the localized field value of the supplied {@link Locale} is different between the old
   * and the new resource, if it is different, then an update action is generated with that change
   * only of this localized field. if the values are not different, then an empty optional is
   * returned.
   *
   * @param newDraft the new product draft.
   * @param oldProduct the old existing product.
   * @param locale the locale of the localized field to sync.
   * @param newLocalizedFieldMapper mapper function to access the localized field on the new product
   *     draft.
   * @param oldLocalizedFieldMapper mapper function to access the localized field on the old
   *     existing product.
   * @param updateActionMapper mapper function to build the update action to sync the localized
   *     field of the product.
   * @return an optional containing an update action if the localized field with the specific locale
   *     has changed or empty otherwise.
   */
  @Nonnull
  private static Optional<UpdateAction<Product>> filterLocalizedField(
      @Nonnull final ProductDraft newDraft,
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final Locale locale,
      @Nonnull final Function<ProductDraft, LocalizedString> newLocalizedFieldMapper,
      @Nonnull final Function<ProductProjection, LocalizedString> oldLocalizedFieldMapper,
      @Nonnull final Function<LocalizedString, UpdateAction<Product>> updateActionMapper) {
    final LocalizedString newLocalizedField = newLocalizedFieldMapper.apply(newDraft);
    final LocalizedString oldLocalizedField = oldLocalizedFieldMapper.apply(oldProduct);
    if (oldLocalizedField != null && newLocalizedField != null) {
      // if both old and new localized fields are set, only update if the locale values are not
      // equal.
      final String newLocaleValue = newLocalizedField.get(locale);
      final String oldLocaleValue = oldLocalizedField.get(locale);

      // We are sure that both old locale and new locale have different values in this method.
      // if old locale value is set, remove it from old localized field
      final LocalizedString withLocaleChange =
          ofNullable(oldLocaleValue)
              .map(
                  value ->
                      LocalizedString.of(
                          oldLocalizedField.stream()
                              .filter(localization -> !localization.getLocale().equals(locale))
                              .collect(
                                  toMap(
                                      LocalizedStringEntry::getLocale,
                                      LocalizedStringEntry::getValue))))
              .orElse(oldLocalizedField);

      // Only if old locale value is not set and the new locale value is set,
      // update the old localized field with the new locale value
      return ofNullable(
          ofNullable(newLocaleValue)
              .map(val -> updateActionMapper.apply(withLocaleChange.plus(locale, val)))
              .orElseGet(() -> updateActionMapper.apply(withLocaleChange)));
    } else {
      if (oldLocalizedField != null) {
        // If old localized field is set but the new one is unset, only update if the locale value
        // is set in
        // the old field.
        return ofNullable(oldLocalizedField.get(locale))
            .map(localValue -> updateActionMapper.apply(LocalizedString.empty()));
      } else {
        // If old localized field is unset but the new one is set, only update if the locale value
        // is set in
        // the new field.
        return ofNullable(newLocalizedField.get(locale))
            .map(
                newLocalValue ->
                    updateActionMapper.apply(LocalizedString.of(locale, newLocalValue)));
      }
    }
  }

  /**
   * Takes a {@link ProductDraft} and filters out any localization other than {@link Locale#FRENCH}
   * from {@link LocalizedString} fields of the supplied {@link ProductDraft}. Note: This method
   * will only filter the {@link LocalizedString} fields on the {@link ProductDraft} level, so it
   * will not filter out {@link LocalizedString} fields on the variant level.
   *
   * @param productDraft the product draft to filter the localizations from.
   * @return a new product draft with {@link LocalizedString} fields that have only entries of the
   *     {@link Locale#FRENCH} locale.
   */
  @Nonnull
  public static ProductDraft filterFrenchLocales(@Nonnull final ProductDraft productDraft) {
    return filterLocalizedStrings(productDraft, Locale.FRENCH);
  }

  /**
   * Takes a {@link ProductDraft} and a locale and filters out any localization other than the
   * specified one in the locale from {@link LocalizedString} fields of the supplied {@link
   * ProductDraft}. Note: This method will only filter the {@link LocalizedString} fields on the
   * {@link ProductDraft} level, so it will not filter out {@link LocalizedString} fields on the
   * variant level.
   *
   * @param productDraft the product draft to filter the localizations from.
   * @param locale the locale to filter in.
   * @return a new product draft with {@link LocalizedString} fields that have only entries of the
   *     supplied locale.
   */
  @Nonnull
  private static ProductDraft filterLocalizedStrings(
      @Nonnull final ProductDraft productDraft, @Nonnull final Locale locale) {

    final LocalizedString name = productDraft.getName();
    final LocalizedString slug = productDraft.getSlug();
    final LocalizedString description = productDraft.getDescription();
    final LocalizedString metaDescription = productDraft.getMetaDescription();
    final LocalizedString metaKeywords = productDraft.getMetaKeywords();
    final LocalizedString metaTitle = productDraft.getMetaTitle();

    return ProductDraftBuilder.of(productDraft)
        .name(filterLocale(name, locale))
        .slug(filterLocale(slug, locale))
        .description(filterLocale(description, locale))
        .metaDescription(filterLocale(metaDescription, locale))
        .metaKeywords(filterLocale(metaKeywords, locale))
        .metaTitle(filterLocale(metaTitle, locale))
        .build();
  }

  /**
   * Takes a {@link LocalizedString} and a locale and filters out any localization other than the
   * specified locale.
   *
   * @param localizedString the localizedString to filter the localizations from.
   * @param locale the locale to filter in.
   * @return a new {@link LocalizedString} with only entries of the supplied locale.
   */
  @Nullable
  private static LocalizedString filterLocale(
      @Nullable final LocalizedString localizedString, @Nonnull final Locale locale) {
    return ofNullable(localizedString)
        .map(
            lText ->
                lText.stream()
                    .filter(
                        localizedStringEntry ->
                            Objects.equals(localizedStringEntry.getLocale(), locale))
                    .collect(
                        toMap(LocalizedStringEntry::getLocale, LocalizedStringEntry::getValue)))
        .map(LocalizedString::of)
        .orElse(null);
  }

  private SyncSingleLocale() {}
}
