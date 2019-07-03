package com.commercetools.sync.integration.externalsource.cartdiscounts;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountUpdateCommand;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeCartPredicate;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeTarget;
import io.sphere.sdk.cartdiscounts.commands.updateactions.ChangeValue;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomField;
import io.sphere.sdk.cartdiscounts.commands.updateactions.SetCustomType;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_CART_PREDICATE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DESC_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DRAFT_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_DRAFT_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_KEY_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_NAME_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_NAME_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_TARGET_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.CART_DISCOUNT_VALUE_2;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_FROM;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.JANUARY_UNTIL;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.OLD_CART_DISCOUNT_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.SORT_ORDER_1;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.createCartDiscountCustomType;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.deleteCartDiscounts;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.getCartDiscountByKey;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.getSortOrders;
import static com.commercetools.sync.integration.commons.utils.CartDiscountITUtils.populateTargetProject;
import static com.commercetools.sync.integration.commons.utils.ITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CartDiscountSyncIT {

    /**
     * Deletes cart discounts from the target CTP projects.
     * Populates the target CTP project with test data.
     */
    @BeforeEach
    void setup() {
        deleteCartDiscounts(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        populateTargetProject();
    }

    @AfterAll
    static void tearDown() {
        deleteCartDiscounts(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewCartPredicate_ShouldUpdateCartDiscountWithNewCartPredicate() {
        // preparation
        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
                                    .cartPredicate(CART_DISCOUNT_CART_PREDICATE_2)
                                    .build();

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
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionsList).containsExactly(ChangeCartPredicate.of(CART_DISCOUNT_CART_PREDICATE_2));
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewValue_ShouldUpdateCartDiscountWithNewValue() {
        // preparation
        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
                                    .value(CART_DISCOUNT_VALUE_2)
                                    .build();

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
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionsList).containsExactly(ChangeValue.of(CART_DISCOUNT_VALUE_2));
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewTarget_ShouldUpdateCartDiscountWithNewTarget() {
        // preparation
        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
                                    .target(CART_DISCOUNT_TARGET_2)
                                    .build();

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
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionsList).containsExactly(ChangeTarget.of(CART_DISCOUNT_TARGET_2));
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewCustomType_ShouldUpdateCartDiscountWithNewCustomType() {
        // preparation
        final Type newCustomType =
            createCartDiscountCustomType("new-type", Locale.ENGLISH, "new-type", CTP_TARGET_CLIENT);

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
                                    .custom(CustomFieldsDraft.ofTypeIdAndJson(newCustomType.getKey(), emptyMap()))
                                    .build();

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
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionsList)
            .containsExactly(SetCustomType.ofTypeIdAndJson(newCustomType.getId(), emptyMap()));
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewCustomTypeWithWrongResIdentifier_ShouldFailToResolveReference() {
        // preparation
        final Type newCustomType =
            createCartDiscountCustomType("new-type", Locale.ENGLISH, "new-type", CTP_TARGET_CLIENT);

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
                                    .custom(CustomFieldsDraft.ofTypeKeyAndJson(newCustomType.getKey(), emptyMap()))
                                    .build();

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
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(errorMessages).containsExactly(
            "Failed to resolve references on CartDiscountDraft with key:'key_1'. Reason: "
                + "Failed to resolve custom type reference on CartDiscountDraft with key:'key_1'. Reason: "
                + "The value of the 'id' field of the Resource Identifier/Reference is blank (null/empty).");
        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(exception -> {
                assertThat(exception).isInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getMessage())
                    .contains("Failed to resolve custom type reference on CartDiscountDraft with key:'key_1'");
                assertThat(exception.getCause()).isInstanceOf(ReferenceResolutionException.class);
                assertThat(exception.getCause().getMessage()).isEqualTo(
                    "The value of the 'id' field of the Resource Identifier/Reference is blank (null/empty).");
            });
        assertThat(updateActionsList).isEmpty();
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 cart discounts were processed in total"
                + " (0 created, 0 updated and 1 failed to sync).");
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithUpdatedCartDiscount_WithNewCustomField_ShouldUpdateCartDiscountWithNewCustomField() {
        // preparation
        final Map<String, JsonNode> customFieldsJsons = createCustomFieldsJsonMap();
        customFieldsJsons.put(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(true));

        final CartDiscountDraft newCartDiscountDraftWithExistingKey =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_1)
                                    .custom(CustomFieldsDraft
                                        .ofTypeIdAndJson(OLD_CART_DISCOUNT_TYPE_KEY, customFieldsJsons))
                                    .build();

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
            .sync(singletonList(newCartDiscountDraftWithExistingKey))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(updateActionsList).containsExactly(SetCustomField
                .ofJson(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(true)));
        assertThat(cartDiscountSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 cart discounts were processed in total"
                + " (0 created, 1 updated and 0 failed to sync).");
        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Test
    void sync_WithNewCartDiscount_ShouldCreateNewDiscount() {
        //preparation
        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);
        // test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(CART_DISCOUNT_DRAFT_2))
            .toCompletableFuture()
            .join();

        //assertions
        assertThat(cartDiscountSyncStatistics).hasValues(1, 1, 0, 0);

        final Optional<CartDiscount> cartDiscountAfterCreation =
            getCartDiscountByKey(CTP_TARGET_CLIENT, CART_DISCOUNT_KEY_2);

        assertThat(cartDiscountAfterCreation).hasValueSatisfying(cartDiscount -> {
            assertThat(cartDiscount.getName()).isEqualTo(CART_DISCOUNT_DRAFT_2.getName());
            assertThat(cartDiscount.getDescription()).isEqualTo(CART_DISCOUNT_DRAFT_2.getDescription());
            assertThat(cartDiscount.getCartPredicate()).isEqualTo(CART_DISCOUNT_DRAFT_2.getCartPredicate());
            assertThat(cartDiscount.getValue()).isEqualTo(CART_DISCOUNT_DRAFT_2.getValue());
        });
    }

    @Test
    void sync_WithoutCartPredicate_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //prepare
        // Draft without "cartPredicate" throws a commercetools exception because "cartPredicate" is a required value
        final CartDiscountDraft newCartDiscountDraftWithoutName =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                (String) null,
                CART_DISCOUNT_VALUE_1,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .key(CART_DISCOUNT_KEY_1)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithoutName))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update cart discount with key: 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("cartPredicate: Missing required value");
            });

        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithoutValue_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        //prepare
        // Draft without "value" throws a commercetools exception because "value" is a required value
        final CartDiscountDraft newCartDiscountDraftWithoutValue =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_1,
                CART_DISCOUNT_CART_PREDICATE_1,
                null,
                CART_DISCOUNT_TARGET_1,
                SORT_ORDER_1,
                false)
                                    .key(CART_DISCOUNT_KEY_1)
                                    .active(false)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .validFrom(JANUARY_FROM)
                                    .validUntil(JANUARY_UNTIL)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(singletonList(newCartDiscountDraftWithoutValue))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to update cart discount with key: 'key_1'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(ErrorResponseException.class);
                assertThat(throwable).hasMessageContaining("value: Missing required value");
            });

        assertThat(cartDiscountSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
        // preparation
        final List<String> sortOrders = getSortOrders(100);
        // Default batch size is 50 (check CartDiscountSyncOptionsBuilder) so we have 2 batches of 50
        final List<CartDiscountDraft> cartDiscountDrafts = IntStream
            .range(0, 100)
            .mapToObj(i ->
                CartDiscountDraftBuilder.of(CART_DISCOUNT_NAME_2,
                    CART_DISCOUNT_CART_PREDICATE_2,
                    CART_DISCOUNT_VALUE_2,
                    CART_DISCOUNT_TARGET_2,
                    sortOrders.get(i),
                    false)
                                        .key(format("key__%s", Integer.toString(i)))
                                        .active(false)
                                        .build())
            .collect(Collectors.toList());

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics cartDiscountSyncStatistics = cartDiscountSync
            .sync(cartDiscountDrafts)
            .toCompletableFuture()
            .join();


        //assertion
        assertThat(cartDiscountSyncStatistics).hasValues(100, 100, 0, 0);
    }

    @Test
    void sync_WithConcurrentModificationException_ShouldRetryToUpdateNewCartDiscountWithSuccess() {
        // Preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();


        final CartDiscountDraft draft2 = CartDiscountDraftBuilder
            .of(CART_DISCOUNT_DRAFT_2)
            .custom(CustomFieldsDraft
                .ofTypeKeyAndJson(OLD_CART_DISCOUNT_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();
        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(draft2))
                         .toCompletableFuture()
                         .join();

        final CartDiscountDraft updatedDraft =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .build();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(spyClient)
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics statistics = cartDiscountSync.sync(singletonList(updatedDraft))
                                                                      .toCompletableFuture()
                                                                      .join();

        // assertion
        assertThat(statistics).hasValues(1, 0, 1, 0);

        // Assert CTP state.
        final PagedQueryResult<CartDiscount> queryResult =
            CTP_TARGET_CLIENT.execute(CartDiscountQuery.of().plusPredicates(queryModel ->
                queryModel.key().is(CART_DISCOUNT_KEY_1)))
                             .toCompletableFuture()
                             .join();

        assertThat(queryResult.head()).hasValueSatisfying(cartDiscount ->
            assertThat(cartDiscount.getKey()).isEqualTo(CART_DISCOUNT_KEY_1));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdate() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final CartDiscountUpdateCommand anyCartDiscountUpdate = any(CartDiscountUpdateCommand.class);

        when(spyClient.execute(anyCartDiscountUpdate))
                .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        //preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

        final CartDiscountDraft draft2 = CartDiscountDraftBuilder
            .of(CART_DISCOUNT_DRAFT_2)
            .custom(CustomFieldsDraft
                .ofTypeKeyAndJson(OLD_CART_DISCOUNT_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();
        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(draft2))
                         .toCompletableFuture()
                         .join();

        final CartDiscountDraft updatedDraft =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics statistics = cartDiscountSync.sync(singletonList(updatedDraft))
                                                                      .toCompletableFuture()
                                                                      .join();

        //assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);

        assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0)).contains(
            format("Failed to update cart discount with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                + "after concurrency modification.", CART_DISCOUNT_KEY_2));

    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CartDiscountQuery.class)))
                .thenCallRealMethod() // Call real fetch on fetching matching cart discounts
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));

        final CartDiscountUpdateCommand anyCartDiscountUpdate = any(CartDiscountUpdateCommand.class);

        when(spyClient.execute(anyCartDiscountUpdate))
                .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        //preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

        final CartDiscountDraft draft2 = CartDiscountDraftBuilder
            .of(CART_DISCOUNT_DRAFT_2)
            .custom(CustomFieldsDraft
                .ofTypeKeyAndJson(OLD_CART_DISCOUNT_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();
        CTP_TARGET_CLIENT.execute(CartDiscountCreateCommand.of(draft2))
                         .toCompletableFuture()
                         .join();

        final CartDiscountDraft updatedDraft =
            CartDiscountDraftBuilder.of(CART_DISCOUNT_DRAFT_2)
                                    .description(CART_DISCOUNT_DESC_1)
                                    .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CartDiscountSyncOptions cartDiscountSyncOptions = CartDiscountSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final CartDiscountSync cartDiscountSync = new CartDiscountSync(cartDiscountSyncOptions);

        //test
        final CartDiscountSyncStatistics statistics = cartDiscountSync.sync(singletonList(updatedDraft))
                                                                      .toCompletableFuture()
                                                                      .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages.get(0)).contains(
            format("Failed to update cart discount with key: '%s'. Reason: Not found when attempting to fetch while "
                + "retrying after concurrency modification.", CART_DISCOUNT_KEY_2));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        final CartDiscountQuery anyCartDiscountQuery = any(CartDiscountQuery.class);

        when(spyClient.execute(anyCartDiscountQuery))
                .thenCallRealMethod() // Call real fetch on fetching matching cart discounts
                .thenReturn(completedFuture(PagedQueryResult.empty()));

        final CartDiscountUpdateCommand anyCartDiscountUpdate = any(CartDiscountUpdateCommand.class);

        when(spyClient.execute(anyCartDiscountUpdate))
                .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()))
                .thenCallRealMethod();

        return spyClient;
    }

}
