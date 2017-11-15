package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.LocalizedStringEntry;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithKeys;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_FRENCH_LOCALIZATIONS_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncSingleLocalizationIT {
    private static ProductType productType;
    private static List<Reference<Category>> categoryReferencesWithIds;
    private static List<Reference<Category>> categoryReferencesWithKeys;
    private ProductSyncOptionsBuilder syncOptionsBuilder;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private static List<UpdateAction<Product>> updateActionsFromSync;

    /**
     * Delete all product related test data from target project. Then create custom types for the categories and a
     * productType for the products of the target CTP project.
     */
    @BeforeClass
    public static void setupAllTests() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        final List<Category> categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        categoryReferencesWithIds = getReferencesWithIds(categories);
        categoryReferencesWithKeys = getReferencesWithKeys(categories);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * 1. Deletes all products from target CTP project
     * 2. Clears all sync collections used for test assertions.
     * 3. Creates an instance for {@link ProductSyncOptionsBuilder} that will be used in the tests to build
     * {@link ProductSyncOptions} instances.
     * 4. Create a product in the target CTP project.
     */
    @Before
    public void setupPerTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptionsBuilder = getProductSyncOptionsBuilder();
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH, productType.toReference(),
            null, null, categoryReferencesWithIds, createRandomCategoryOrderHints(categoryReferencesWithIds));
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
    }

    private void clearSyncTestCollections() {
        updateActionsFromSync = new ArrayList<>();
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
    }

    private ProductSyncOptionsBuilder getProductSyncOptionsBuilder() {
        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };

        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            actionsCallBack = (updateActions, newDraft, oldProduct) -> {
                updateActionsFromSync.addAll(updateActions);
                return updateActions;
            };

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
                                        .beforeUpdateCallback(actionsCallBack);
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void sync_withFrenchLocalizationsFilter_shouldOnlySyncFrenchData() {
        final ProductDraft productDraft =
            createProductDraft(PRODUCT_KEY_1_FRENCH_LOCALIZATIONS_PATH, referenceOfId(productType.getKey()), null,
                null, categoryReferencesWithKeys, createRandomCategoryOrderHints(categoryReferencesWithKeys));

        final ProductSyncOptions syncOptions = syncOptionsBuilder
            .beforeUpdateCallback(ProductSyncSingleLocalizationIT::syncFrenchDataOnly)
            .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));
        assertThat(updateActionsFromSync.stream()
                                        .filter(updateAction -> updateAction instanceof ChangeName)
                                        .findFirst()
                                        .map(changeNameUpdateAction -> ((ChangeName)changeNameUpdateAction).getName()))
            .contains(LocalizedString.of(Locale.ENGLISH, "english name", Locale.FRENCH, "french name"));

        assertThat(updateActionsFromSync.stream()
                                        .filter(updateAction -> updateAction instanceof SetDescription)
                                        .findFirst()
                                        .map(setDescriptionAction -> ((SetDescription)setDescriptionAction)
                                            .getDescription()))
            .contains(LocalizedString.of(Locale.ENGLISH, "english description.", Locale.FRENCH, "french description."));

        assertThat(updateActionsFromSync.stream()
                                        .filter(updateAction -> updateAction instanceof ChangeSlug)
                                        .findFirst()
                                        .map(changeSlugAction -> ((ChangeSlug)changeSlugAction).getSlug()))
            .contains(LocalizedString.of(Locale.ENGLISH, "english-slug", Locale.FRENCH, "french-slug"));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }


    /**
     * Takes in a {@link List} of product update actions that was built from comparing a {@code newDraft} and an
     * {@code oldProduct} and maps the update actions so that only localizations with value {@link Locale#FRENCH}
     * are synced and all the other locales are left untouched.
     *
     * @param updateActions the update actions built from comparing {@code newDraft} and {@code oldProduct}.
     * @param newDraft      the new {@link ProductDraft} being synced.
     * @param oldProduct    the old existing {@link Product}.
     * @return a new list of update actions that corresponds to changes on French localizations only.
     */
    private static List<UpdateAction<Product>> syncFrenchDataOnly(
        @Nonnull final List<UpdateAction<Product>> updateActions,
        @Nonnull final ProductDraft newDraft,
        @Nonnull final Product oldProduct) {
        final List<UpdateAction<Product>> filteredActions = updateActions.stream()
                                                                         .map(action -> filterSingleLocalization(action,
                                                                             newDraft, oldProduct, Locale.FRENCH))
                                                                         .filter(Optional::isPresent)
                                                                         .map(Optional::get)
                                                                         .collect(toList());
        updateActionsFromSync.addAll(filteredActions);
        return filteredActions;
    }

    /**
     * Takes a product update action, a new {@link ProductDraft}, an old existing {@link Product} and a {@link Locale}.
     * This method checks if the update action is either one of the following update actions:
     * <ul>
     * <li>{@link ChangeName}</li>
     * <li>{@link SetMetaKeywords}</li>
     * <li>{@link SetMetaDescription}</li>
     * <li>{@link SetMetaTitle}</li>
     * <li>{@link ChangeSlug}</li>
     * <li>{@link SetDescription}</li>
     * </ul>
     * If the update action is one of the aforementioned update actions, the method checks if the change in
     * localization generated from the update action corresponds to a change in the supplied {@link Locale} value and
     * maps the update action so that it only corresponds to such change in the localized field. Namely, if the
     * change was only in another locale value other than the supplied one, then no update action is needed. On the
     * other hand, if the change was in the supplied locale value then the update action is modified so that it only
     * corresponds to the change in that locale value.
     *
     * @param updateAction the update action built from comparing {@code newDraft} and {@code oldProduct}.
     * @param newDraft     the new {@link ProductDraft} being synced.
     * @param oldProduct   the old existing {@link Product}.
     * @param locale       the locale value to only compare and map the update action to accordingly.
     * @return an optional containing the mapped update action or empty value if an update action is not needed.
     */
    private static Optional<UpdateAction<Product>> filterSingleLocalization(
        @Nonnull final UpdateAction<Product> updateAction,
        @Nonnull final ProductDraft newDraft,
        @Nonnull final Product oldProduct,
        @Nonnull final Locale locale) {
        if (updateAction instanceof ChangeName) {
            return filterLocalizedField(newDraft, oldProduct, locale, ProductDraft::getName, ProductData::getName,
                ChangeName::of);
        }
        if (updateAction instanceof SetDescription) {
            return filterLocalizedField(newDraft, oldProduct, locale, ProductDraft::getDescription,
                ProductData::getDescription, SetDescription::of);
        }
        if (updateAction instanceof ChangeSlug) {
            return filterLocalizedField(newDraft, oldProduct, locale, ProductDraft::getSlug, ProductData::getSlug,
                ChangeSlug::of);
        }
        if (updateAction instanceof SetMetaTitle) {
            return filterLocalizedField(newDraft, oldProduct, locale, ProductDraft::getMetaTitle,
                ProductData::getMetaTitle, SetMetaTitle::of);
        }
        if (updateAction instanceof SetMetaDescription) {
            return filterLocalizedField(newDraft, oldProduct, locale, ProductDraft::getMetaDescription,
                ProductData::getMetaDescription, SetMetaDescription::of);
        }
        if (updateAction instanceof SetMetaKeywords) {
            return filterLocalizedField(newDraft, oldProduct, locale, ProductDraft::getMetaKeywords,
                ProductData::getMetaKeywords, SetMetaKeywords::of);
        }
        return Optional.of(updateAction);
    }

    /**
     * Checks if the localized field value of the supplied {@link Locale} is different between the old and the new
     * resource, if it is different, then an update action is generated with that change only of this localized field.
     * if the values are not different, then an empty optional is returned.
     *
     * @param newDraft                the new product draft.
     * @param oldProduct              the old existing product.
     * @param locale                  the locale of the localized field to sync.
     * @param newLocalizedFieldMapper mapper function to access the localized field on the new product draft.
     * @param oldLocalizedFieldMapper mapper function to access the localized field on the old existing product.
     * @param updateActionMapper      mapper function to build the update action to sync the localized field of the
     *                                product.
     * @return an optional containing an update action if the localized field with the specific locale has changed or
     *         empty otherwise.
     */
    private static Optional<UpdateAction<Product>> filterLocalizedField(@Nonnull final ProductDraft newDraft,
                                                                        @Nonnull final Product oldProduct,
                                                                        @Nonnull final Locale locale,
                                                                        @Nonnull final Function<ProductDraft,
                                                                            LocalizedString> newLocalizedFieldMapper,
                                                                        @Nonnull final Function<ProductData,
                                                                            LocalizedString> oldLocalizedFieldMapper,
                                                                        @Nonnull final Function<LocalizedString,
                                                                            UpdateAction<Product>> updateActionMapper) {
        final String newLocaleValue = newLocalizedFieldMapper.apply(newDraft).get(locale);
        final LocalizedString oldLocalizedField = oldLocalizedFieldMapper.apply(oldProduct.getMasterData().getStaged());
        final String oldLocaleValue = oldLocalizedField.get(locale);
        if (!Objects.equals(newLocaleValue, oldLocaleValue)) {
            LocalizedString withLocaleChange = oldLocalizedField;
            if (oldLocaleValue != null) {
                withLocaleChange = LocalizedString.of(
                    oldLocalizedField.stream()
                                     .filter(localization -> !localization.getLocale().equals(locale))
                                     .collect(toMap(LocalizedStringEntry::getLocale, LocalizedStringEntry::getValue)));
            }
            withLocaleChange = withLocaleChange.plus(locale, newLocaleValue);
            return Optional.of(updateActionMapper.apply(withLocaleChange));
        }
        return Optional.empty();
    }
}
