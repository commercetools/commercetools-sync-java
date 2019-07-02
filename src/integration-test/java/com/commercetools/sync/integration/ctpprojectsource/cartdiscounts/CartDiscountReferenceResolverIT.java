package com.commercetools.sync.integration.ctpprojectsource.cartdiscounts;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftDsl;
import io.sphere.sdk.cartdiscounts.GiftLineItemCartDiscountValue;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceReplacementUtils.buildCartDiscountQuery;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceReplacementUtils.replaceCartDiscountsReferenceIdsWithKeys;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DESC_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DRAFT_WITH_REFERENCES;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_GIFT_LINEITEM_PRODUCT_KEY;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_NAME_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_FROM;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_UNTIL;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscountsFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.getCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

class CartDiscountReferenceResolverIT {

    @BeforeEach
    void setup() {
        deleteCartDiscountsFromTargetAndSource();
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        populateSourceProject();
        populateTargetProject();
    }

    @AfterAll
    static void tearDown() {
        deleteCartDiscountsFromTargetAndSource();
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithProductReferenceWithIdNotKey_ShouldFailToResolve() {
        // preparation
        final Product sourceProduct = CTP_SOURCE_CLIENT
            .execute(ProductByKeyGet.of(CART_DISCOUNT_GIFT_LINEITEM_PRODUCT_KEY))
            .toCompletableFuture()
            .join();

        final GiftLineItemCartDiscountValue inValidValue = GiftLineItemCartDiscountValue.of(
            ResourceIdentifier.ofId(sourceProduct.getId()), 1, null, null);
        final CartDiscountDraftDsl invalidDraft =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                CART_DISCOUNT_CART_PREDICATE_1,
                inValidValue,
                null,
                "0.2439849",
                false)
                                    .key("invalidCartDiscountKey")
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .custom(getCustomFieldsDraft())
                                    .build();

        CTP_SOURCE_CLIENT.execute(CartDiscountCreateCommand.of(invalidDraft)).toCompletableFuture().join();

        final List<CartDiscount> cartDiscounts = CTP_SOURCE_CLIENT
            .execute(buildCartDiscountQuery())
            .toCompletableFuture()
            .join()
            .getResults();


        final List<CartDiscountDraft> cartDiscountDrafts =
            replaceCartDiscountsReferenceIdsWithKeys(cartDiscounts);

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<UpdateAction<CartDiscount>> updateActionsList = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .beforeUpdateCallback((updateActions, newCartDiscount, oldCartDiscount) -> {
                updateActionsList.addAll(updateActions);
                return updateActions;
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(cartDiscountDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(errorMessage ->
                assertThat(errorMessage).isEqualTo("Failed to resolve references on cartDiscount with key:"
                    + "'invalidCartDiscountKey'. Reason: Failed to resolve a GiftLineItem resourceIdentifier on the "
                    + "CartDiscount with key:'invalidCartDiscountKey'. Reason: The value of the 'key' field of the "
                    + "resourceIdentifier of the 'product' field is blank (null/empty)."));

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(exception -> {
                assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                assertThat(exception).hasMessage("Failed to resolve a GiftLineItem resourceIdentifier on the "
                    + "CartDiscount with key:'invalidCartDiscountKey'. Reason: The value of the 'key' field of the "
                    + "resourceIdentifier of the 'product' field is blank (null/empty).");

                final Throwable cause = exception.getCause();
                assertThat(cause).isInstanceOf(ReferenceResolutionException.class);
                assertThat(cause).hasMessage("The value of the 'key' field of the resourceIdentifier of the "
                    + "'product' field is blank (null/empty).");
            });

        assertThat(updateActionsList).isEmpty();
        AssertionsForStatistics.assertThat(cartDiscountSyncStatistics).hasValues(4, 2, 0, 1);
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 4 cart discounts were processed in total"
                + " (2 created, 0 updated and 1 failed to sync).");
    }

    @Test
    void sync_WithGiftLineItemReferenceChanges_ShouldSyncCorrectly() {
        // preparation

        // Create a cart discount in the target project with different gift line item references than the matching cart
        // discount in the source project.
        final ProductDraft productDraft = ProductDraftBuilder
            .of(ResourceIdentifier.ofKey("syncProductType"), ofEnglish("product"), ofEnglish("new-slug"), emptyList())
            .key("new-prod")
            .build();
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

        final ChannelDraft newProductDistributionChannel = ChannelDraftBuilder
            .of("productDistribution")
            .roles(singleton(ChannelRole.PRODUCT_DISTRIBUTION))
            .key("new-dist")
            .build();

        final ChannelDraft newSupplyChannel = ChannelDraftBuilder
            .of("supply")
            .roles(singleton(ChannelRole.INVENTORY_SUPPLY))
            .key("new-supp")
            .build();

        CompletableFuture.allOf(
            CTP_SOURCE_CLIENT.execute(ChannelCreateCommand.of(newProductDistributionChannel)).toCompletableFuture(),
            CTP_SOURCE_CLIENT.execute(ChannelCreateCommand.of(newSupplyChannel)).toCompletableFuture()).join();

        CompletableFuture.allOf(
            CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(newProductDistributionChannel)).toCompletableFuture(),
            CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(newSupplyChannel)).toCompletableFuture()).join();


        final GiftLineItemCartDiscountValue changedGiftLineItemValue = GiftLineItemCartDiscountValue.of(
            ResourceIdentifier.ofKey(productDraft.getKey()), 1,
            ResourceIdentifier.ofKey(newSupplyChannel.getKey()),
            ResourceIdentifier.ofKey(newProductDistributionChannel.getKey()));


        final CartDiscountDraft draftWithChangedReferences =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_WITH_REFERENCES)
                                    .value(changedGiftLineItemValue)
                                    .build();

        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(draftWithChangedReferences))
                         .toCompletableFuture()
                         .join();

        final List<CartDiscount> cartDiscounts = CTP_SOURCE_CLIENT
            .execute(buildCartDiscountQuery())
            .toCompletableFuture()
            .join()
            .getResults();


        final List<CartDiscountDraft> cartDiscountDrafts =
            replaceCartDiscountsReferenceIdsWithKeys(cartDiscounts);

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<UpdateAction<CartDiscount>> updateActionsList = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .beforeUpdateCallback((updateActions, newCartDiscount, oldCartDiscount) -> {
                updateActionsList.addAll(updateActions);
                return updateActions;
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(cartDiscountDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        assertThat(updateActionsList).isEmpty();
        AssertionsForStatistics.assertThat(cartDiscountSyncStatistics).hasValues(4, 2, 0, 1);
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 4 cart discounts were processed in total"
                + " (2 created, 0 updated and 1 failed to sync).");
    }
}
