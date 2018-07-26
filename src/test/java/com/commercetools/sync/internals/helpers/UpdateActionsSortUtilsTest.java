package com.commercetools.sync.internals.helpers;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.RemovePrice;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

import static com.commercetools.sync.internals.utils.UpdateActionsSortUtils.sortPriceActions;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_05_07;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateActionsSortUtilsTest {
    @ParameterizedTest(name = "[#sortPriceActions]: {0}")
    @MethodSource("sortPriceActionsTestCases")
    void sortPriceActionsTest(@Nonnull final String testCaseName,
                              @Nonnull final List<UpdateAction<Product>> updateActions,
                              @Nonnull final List<UpdateAction<Product>> expectedResult) {

        final List<UpdateAction<Product>> result = sortPriceActions(updateActions);

        assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> sortPriceActionsTestCases() {
        final String case1 = "empty list";
        final String case2 = "one element";

        final String case3 = "removePrice, changePrice";
        final String case4 = "changePrice, removePrice";

        final String case5 = "removePrice, addPrice";
        final String case6 = "addPrice, removePrice";

        final String case7 = "changePrice, addPrice";
        final String case8 = "addPrice, changePrice";

        final String case9 = "RemovePrice, changePrice, addPrice";
        final String case10 = "RemovePrice, addPrice, changePrice";

        final String case11 = "addPrice, changePrice, RemovePrice";
        final String case12 = "addPrice, RemovePrice, changePrice";

        final String case13 = "changePrice, RemovePrice, addPrice";
        final String case14 = "changePrice, addPrice, RemovePrice";


        final UpdateAction<Product> changePrice = ChangePrice.of("1", null, true);
        final UpdateAction<Product> addPrice = AddPrice.of(1, DRAFT_NE_777_EUR_05_07);
        final UpdateAction<Product> removePrice = RemovePrice.of("1");

        return Stream.of(
            Arguments.of(case1, emptyList(), emptyList()),
            Arguments.of(case2, singletonList(changePrice), singletonList(changePrice)),

            Arguments.of(case3, asList(removePrice, changePrice), asList(removePrice, changePrice)),
            Arguments.of(case4, asList(changePrice, removePrice), asList(removePrice, changePrice)),

            Arguments.of(case5, asList(removePrice, addPrice), asList(removePrice, addPrice)),
            Arguments.of(case6, asList(addPrice, removePrice), asList(removePrice, addPrice)),

            Arguments.of(case7, asList(changePrice, addPrice), asList(changePrice, addPrice)),
            Arguments.of(case8, asList(addPrice, changePrice), asList(changePrice, addPrice)),

            Arguments.of(case9, asList(removePrice, changePrice, addPrice), asList(removePrice, changePrice, addPrice)),
            Arguments
                .of(case10, asList(removePrice, addPrice, changePrice), asList(removePrice, changePrice, addPrice)),

            Arguments
                .of(case11, asList(addPrice, changePrice, removePrice), asList(removePrice, changePrice, addPrice)),
            Arguments
                .of(case12, asList(addPrice, removePrice, changePrice), asList(removePrice, changePrice, addPrice)),

            Arguments
                .of(case13, asList(changePrice, removePrice, addPrice), asList(removePrice, changePrice, addPrice)),
            Arguments.of(case14, asList(changePrice, addPrice, removePrice), asList(removePrice, changePrice, addPrice))
        );
    }
}
