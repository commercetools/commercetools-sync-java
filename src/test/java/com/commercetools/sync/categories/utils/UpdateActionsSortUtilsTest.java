package com.commercetools.sync.categories.utils;

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
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

import static com.commercetools.sync.categories.utils.UpdateActionsSortUtils.sortAssetActions;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateActionsSortUtilsTest {
    @ParameterizedTest(name = "[#sortCategoryAssetActions]: {0}")
    @MethodSource("sortAssetActionsTestCases")
    void sortAssetActionsTest(@Nonnull final String testCaseName,
                              @Nonnull final List<UpdateAction<Category>> updateActions,
                              @Nonnull final List<UpdateAction<Category>> expectedResult) {

        final List<UpdateAction<Category>> result = sortAssetActions(updateActions);

        assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> sortAssetActionsTestCases() {
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

        final String case29 = "changeAssetOrder, setAssetSources";
        final String case30 = "setAssetSources, changeAssetOrder";

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
                    setAssetCustomField, changeAssetOrder, addAsset)),

            Arguments.of(case29,
                asList(changeAssetOrder, setAssetSources),
                asList(setAssetSources, changeAssetOrder)),

            Arguments.of(case30,
                asList(setAssetSources, changeAssetOrder),
                asList(setAssetSources, changeAssetOrder))
        );
    }
}
