package com.commercetools.sync.commons.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;

public class CommonTypeUpdateActionUtilsTest {
    private static final Locale LOCALE = Locale.GERMAN;
    private static final String MOCK_CATEGORY_REFERENCE_TYPE = "type";
    private static final String MOCK_OLD_CATEGORY_NAME = "categoryName";

    @Test
    public void buildUpdateActionForLocalizedStrings_WithDifferentFieldValues_ShouldBuildUpdateAction() {
        final LocalizedString oldLocalisedString = LocalizedString.of(LOCALE, "Apfel");
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
            buildUpdateAction(oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithEmptyOldFields_ShouldBuildUpdateAction() {
        final LocalizedString oldLocalisedString = LocalizedString.of(new HashMap<>());
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
            buildUpdateAction(oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithEmptyFields_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
            buildUpdateAction(LocalizedString.of(new HashMap<>()),
                LocalizedString.of(new HashMap<>()), () -> mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithSameValues_ShouldNotBuildUpdateAction() {
        final LocalizedString oldLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
            buildUpdateAction(oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithSameValuesButDifferentFieldOrder_ShouldNotBuildUpdateAction() {
        final LocalizedString oldLocalisedString = LocalizedString.of(LOCALE, "Milch")
                                                                  .plus(Locale.FRENCH, "Lait")
                                                                  .plus(Locale.ENGLISH, "Milk");

        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch")
                                                                  .plus(Locale.ENGLISH, "Milk")
                                                                  .plus(Locale.FRENCH, "Lait");

        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithNullValues_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName
            .of(LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(null, null, () -> mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithNullUpdateAction_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(null, null, () -> null);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForReferences_WithDifferentTypeReferenceIds_ShouldBuildUpdateAction() {
        final Reference<Category> oldCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE,
            "1");
        Reference<Category> newCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "2");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(oldCategoryReference, newCategoryReference, () -> mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForReferences.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForReferences_WithOneNullReference_ShouldBuildUpdateAction() {
        final Reference<Category> categoryReference = Reference
            .ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(null, categoryReference, () -> mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForReferences.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForReferences_WithSameCategoryReferences_ShouldNotBuildUpdateAction() {
        final Reference<Category> categoryReference = Reference
            .ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(categoryReference, categoryReference, () -> mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForReferences_WithNullReferences_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(null, null, () -> mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForReferences_WithNullUpdateAction_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Category>> updateActionForReferences =
            buildUpdateAction(null, null, () -> null);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_WithDifferentStrings_ShouldBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
            buildUpdateAction("1", "2", () -> mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForStrings_WithOneNullString_ShouldBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
            buildUpdateAction(null, "2", () -> mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForStrings_WithSameStrings_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
            buildUpdateAction("1", "1", () -> mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_WithNullStrings_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
            LocalizedString.of(LOCALE, MOCK_OLD_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForStrings =
            buildUpdateAction(null, null, () -> mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_WithNullUpdateAction_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Category>> updateActionForStrings =
            buildUpdateAction(null, null, () -> null);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }
}
