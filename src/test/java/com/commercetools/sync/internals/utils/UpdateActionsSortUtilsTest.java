package com.commercetools.sync.internals.utils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.AddAsset;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.categories.commands.updateactions.RemoveAsset;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.categories.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.categories.commands.updateactions.SetAssetSources;
import io.sphere.sdk.categories.commands.updateactions.SetAssetTags;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.ChangePrice;
import io.sphere.sdk.products.commands.updateactions.RemovePrice;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

import static com.commercetools.sync.internals.utils.UpdateActionsSortUtils.sortCategoryAssetActions;
import static com.commercetools.sync.internals.utils.UpdateActionsSortUtils.sortPriceActions;
import static com.commercetools.sync.products.utils.productvariantupdateactionutils.prices.PriceDraftFixtures.DRAFT_NE_777_EUR_05_07;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
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

    /**
     * Testing all different conditions stated in Effective Java for comparator implementation:
     * - anticommutation
     * - exception symmetry
     * - transitivity
     * - consistency with <code>equals</code>.
     *
     * <p>More info here: http://www.javapractices.com/topic/TopicAction.do?Id=10
     */
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

        final String case15 = "changePrice, changePrice2";
        final String case16 = "changePrice2, changePrice";
        final String case17 = "removePrice, removePrice2";
        final String case18 = "removePrice2, removePrice";
        final String case19 = "addPrice, addPrice2";
        final String case20 = "addPrice2, addPrice";

        final String case21 = "removePrice, changePrice2";
        final String case22 = "changePrice2, removePrice";

        final String case23 = "removePrice, addPrice";
        final String case24 = "addPrice, removePrice";

        final String case25 = "changePrice2, addPrice";
        final String case26 = "addPrice, changePrice2";

        final String case27 = "RemovePrice, changePrice2, addPrice";
        final String case28 = "RemovePrice, addPrice, changePrice2";

        final String case29 = "addPrice, changePrice2, RemovePrice";
        final String case30 = "addPrice, RemovePrice, changePrice2";

        final String case31 = "changePrice2, RemovePrice, addPrice";
        final String case32 = "changePrice2, addPrice, RemovePrice";

        final UpdateAction<Product> changePrice = ChangePrice.of("1", null, true);
        final UpdateAction<Product> changePrice2 = ChangePrice.of("2", null, true);
        final UpdateAction<Product> addPrice = AddPrice.of(1, DRAFT_NE_777_EUR_05_07);
        final UpdateAction<Product> addPrice2 = AddPrice.of(2, DRAFT_NE_777_EUR_05_07);
        final UpdateAction<Product> removePrice = RemovePrice.of("1");
        final UpdateAction<Product> removePrice2 = RemovePrice.of("2");

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
            Arguments
                .of(case14, asList(changePrice, addPrice, removePrice), asList(removePrice, changePrice, addPrice)),

            Arguments.of(case15, asList(changePrice, changePrice2), asList(changePrice, changePrice2)),
            Arguments.of(case16, asList(changePrice2, changePrice), asList(changePrice2, changePrice)),
            Arguments.of(case17, asList(removePrice, removePrice2), asList(removePrice, removePrice2)),
            Arguments.of(case18, asList(removePrice2, removePrice), asList(removePrice2, removePrice)),
            Arguments.of(case19, asList(addPrice, addPrice2), asList(addPrice, addPrice2)),
            Arguments.of(case20, asList(addPrice2, addPrice), asList(addPrice2, addPrice)),

            Arguments.of(case21, asList(removePrice, changePrice2), asList(removePrice, changePrice2)),
            Arguments.of(case22, asList(changePrice2, removePrice), asList(removePrice, changePrice2)),

            Arguments.of(case23, asList(removePrice, addPrice), asList(removePrice, addPrice)),
            Arguments.of(case24, asList(addPrice, removePrice), asList(removePrice, addPrice)),

            Arguments.of(case25, asList(changePrice2, addPrice), asList(changePrice2, addPrice)),
            Arguments.of(case26, asList(addPrice, changePrice2), asList(changePrice2, addPrice)),

            Arguments
                .of(case27, asList(removePrice, changePrice2, addPrice), asList(removePrice, changePrice2, addPrice)),
            Arguments
                .of(case28, asList(removePrice, addPrice, changePrice2), asList(removePrice, changePrice2, addPrice)),

            Arguments
                .of(case29, asList(addPrice, changePrice2, removePrice), asList(removePrice, changePrice2, addPrice)),
            Arguments
                .of(case30, asList(addPrice, removePrice, changePrice2), asList(removePrice, changePrice2, addPrice)),

            Arguments
                .of(case31, asList(changePrice2, removePrice, addPrice), asList(removePrice, changePrice2, addPrice)),
            Arguments
                .of(case32, asList(changePrice2, addPrice, removePrice), asList(removePrice, changePrice2, addPrice))
        );
    }

    @ParameterizedTest(name = "[#sortCategoryAssetActions]: {0}")
    @MethodSource("sortCategoryAssetActionsTestCases")
    void sortCategoryAssetActionsTest(@Nonnull final String testCaseName,
                                      @Nonnull final List<UpdateAction<Category>> updateActions,
                                      @Nonnull final List<UpdateAction<Category>> expectedResult) {

        final List<UpdateAction<Category>> result = sortCategoryAssetActions(updateActions);

        assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> sortCategoryAssetActionsTestCases() {
        final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("name")).build();
        final UpdateAction<Category> removeAsset = RemoveAsset.ofKey("1");
        final UpdateAction<Category> addAsset = AddAsset.of(assetDraft);
        final UpdateAction<Category> changeAssetOrder = ChangeAssetOrder.of(emptyList());
        final UpdateAction<Category> setAssetDescription = SetAssetDescription.ofKey("1", ofEnglish("desc"));
        final UpdateAction<Category> setAssetTags = SetAssetTags.ofKey("1", emptySet());
        final UpdateAction<Category> setAssetSources = SetAssetSources.ofKey("1", emptyList());
        final UpdateAction<Category> setAssetCustomType = SetAssetCustomType
            .ofKey("1", CustomFieldsDraft.ofTypeIdAndJson("1", emptyMap()));
        final UpdateAction<Category> setAssetCustomField = SetAssetCustomField
            .ofJsonValueWithKey("1", "name", JsonNodeFactory.instance.objectNode());


        final List<UpdateAction<Category>> expectedList = asList(removeAsset, setAssetDescription, changeAssetOrder,
            addAsset);

        final String case1 = "empty list";
        final String case2 = "one element";

        // Different permutations
        final String case3 =
            "removeAsset, addAsset, changeAssetOrder, setAssetDescription";
        final String case4 =
            "removeAsset, addAsset, setAssetDescription, changeAssetOrder";
        final String case5 =
            "removeAsset, changeAssetOrder, addAsset, setAssetDescription";
        final String case6 =
            "removeAsset, changeAssetOrder, setAssetDescription, addAsset";
        final String case7 =
            "removeAsset, setAssetDescription, addAsset, changeAssetOrder";
        final String case8 =
            "removeAsset, setAssetDescription, changeAssetOrder, addAsset";

        final String case9 =
            "addAsset, removeAsset, changeAssetOrder, setAssetDescription";
        final String case10 =
            "addAsset, removeAsset, setAssetDescription, changeAssetOrder";
        final String case11 =
            "addAsset, setAssetDescription, changeAssetOrder, removeAsset";
        final String case12 =
            "addAsset, setAssetDescription, removeAsset, changeAssetOrder";
        final String case13 =
            "addAsset, changeAssetOrder, removeAsset, setAssetDescription";
        final String case14 =
            "addAsset, changeAssetOrder, setAssetDescription, removeAsset";

        final String case15 =
            "changeAssetOrder, removeAsset, addAsset, setAssetDescription";
        final String case16 =
            "changeAssetOrder, removeAsset, setAssetDescription, addAsset";
        final String case17 =
            "changeAssetOrder, setAssetDescription, addAsset, removeAsset";
        final String case18 =
            "changeAssetOrder, setAssetDescription, removeAsset, addAsset";
        final String case19 =
            "changeAssetOrder, addAsset, removeAsset, setAssetDescription";
        final String case20 =
            "changeAssetOrder, addAsset, setAssetDescription, removeAsset";

        final String case21 =
            "setAssetDescription, removeAsset, addAsset, changeAssetOrder";
        final String case22 =
            "setAssetDescription, removeAsset, changeAssetOrder, addAsset";
        final String case23 =
            "setAssetDescription, changeAssetOrder, addAsset, removeAsset";
        final String case24 =
            "setAssetDescription, changeAssetOrder, removeAsset, addAsset";
        final String case25 =
            "setAssetDescription, addAsset, removeAsset, changeAssetOrder";
        final String case26 =
            "setAssetDescription, addAsset, changeAssetOrder, removeAsset";

        // case with repetition
        final String case27 =
            "setAssetDescription, addAsset, changeAssetOrder, removeAsset, addAsset";

        // case with multiple asset set actions
        final String case28 =
            "setAssetDescription, addAsset, setAssetTags, changeAssetOrder, removeAsset, setAssetSources,"
                + " setAssetCustomType, setAssetCustomField";

        return Stream.of(
            Arguments.of(case1, emptyList(), emptyList()),
            Arguments.of(case2, singletonList(removeAsset), singletonList(removeAsset)),

            Arguments.of(case3, asList(removeAsset, addAsset, changeAssetOrder, setAssetDescription), expectedList),
            Arguments.of(case4, asList(removeAsset, addAsset, setAssetDescription, changeAssetOrder), expectedList),
            Arguments.of(case5, asList(removeAsset, changeAssetOrder, addAsset, setAssetDescription), expectedList),
            Arguments.of(case6, asList(removeAsset, changeAssetOrder, setAssetDescription, addAsset), expectedList),
            Arguments.of(case7, asList(removeAsset, setAssetDescription, addAsset, changeAssetOrder), expectedList),
            Arguments.of(case8, asList(removeAsset, setAssetDescription, changeAssetOrder, addAsset), expectedList),

            Arguments.of(case9, asList(addAsset, removeAsset, changeAssetOrder, setAssetDescription), expectedList),
            Arguments.of(case10, asList(addAsset, removeAsset, setAssetDescription, changeAssetOrder), expectedList),
            Arguments.of(case11, asList(addAsset, setAssetDescription, changeAssetOrder, removeAsset), expectedList),
            Arguments.of(case12, asList(addAsset, setAssetDescription, removeAsset, changeAssetOrder), expectedList),
            Arguments.of(case13, asList(addAsset, changeAssetOrder, removeAsset, setAssetDescription), expectedList),
            Arguments.of(case14, asList(addAsset, changeAssetOrder, setAssetDescription, removeAsset), expectedList),

            Arguments.of(case15, asList(changeAssetOrder, removeAsset, addAsset, setAssetDescription), expectedList),
            Arguments.of(case16, asList(changeAssetOrder, removeAsset, setAssetDescription, addAsset), expectedList),
            Arguments.of(case17, asList(changeAssetOrder, setAssetDescription, addAsset, removeAsset), expectedList),
            Arguments.of(case18, asList(changeAssetOrder, setAssetDescription, removeAsset, addAsset), expectedList),
            Arguments.of(case19, asList(changeAssetOrder, addAsset, removeAsset, setAssetDescription), expectedList),
            Arguments.of(case20, asList(changeAssetOrder, addAsset, setAssetDescription, removeAsset), expectedList),

            Arguments.of(case21, asList(setAssetDescription, removeAsset, addAsset, changeAssetOrder), expectedList),
            Arguments.of(case22, asList(setAssetDescription, removeAsset, changeAssetOrder, addAsset), expectedList),
            Arguments.of(case23, asList(setAssetDescription, changeAssetOrder, addAsset, removeAsset), expectedList),
            Arguments.of(case24, asList(setAssetDescription, changeAssetOrder, removeAsset, addAsset), expectedList),
            Arguments.of(case25, asList(setAssetDescription, addAsset, removeAsset, changeAssetOrder), expectedList),
            Arguments.of(case26, asList(setAssetDescription, addAsset, changeAssetOrder, removeAsset), expectedList),

            Arguments.of(case27,
                //Test case:
                asList(setAssetDescription, addAsset, changeAssetOrder, removeAsset, addAsset),
                //expected result:
                asList(removeAsset, setAssetDescription, changeAssetOrder, addAsset, addAsset)),

            Arguments.of(case28,
                //Test case:
                asList(setAssetDescription, addAsset, setAssetTags, changeAssetOrder,
                removeAsset, setAssetSources, setAssetCustomType, setAssetCustomField),
                //expected result:
                asList(removeAsset, setAssetDescription, setAssetTags, setAssetSources, setAssetCustomType,
                    setAssetCustomField, changeAssetOrder, addAsset))
        );
    }
}
