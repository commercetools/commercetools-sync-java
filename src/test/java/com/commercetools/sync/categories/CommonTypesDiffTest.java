package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.categories.CommonTypesDiff.buildUpdateActionForLocalizedStrings;
import static com.commercetools.sync.categories.CommonTypesDiff.buildUpdateActionForReferences;
import static com.commercetools.sync.categories.CommonTypesDiff.buildUpdateActionForStrings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommonTypesDiffTest {
    private final static Category MOCK_EXISTING_CATEGORY = mock(Category.class);
    private final static Locale LOCALE = Locale.GERMAN;
    private final static String MOCK_CATEGORY_REFERENCE_TYPE = "type";
    private final static String MOCK_EXISTING_CATEGORY_PARENT_ID = "1";
    private final static String MOCK_EXISTING_CATEGORY_NAME = "categoryName";

    @BeforeClass
    public static void setup() {
        when(MOCK_EXISTING_CATEGORY.getName()).thenReturn(LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        when(MOCK_EXISTING_CATEGORY.getParent()).thenReturn(Reference.of(MOCK_CATEGORY_REFERENCE_TYPE, MOCK_EXISTING_CATEGORY_PARENT_ID));
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithDifferentFieldValues_ShouldBuildUpdateAction() {
        final LocalizedString existingLocalisedString = LocalizedString.of(LOCALE, "Apfel");
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithEmptyExistingFields_ShouldBuildUpdateAction() {
        final LocalizedString existingLocalisedString = LocalizedString.of(new HashMap<>());
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithEmptyFields_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(LocalizedString.of(new HashMap<>()),
                        LocalizedString.of(new HashMap<>()), mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithSameValues_ShouldNotBuildUpdateAction() {
        final LocalizedString existingLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForLocalizedStrings =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        assertThat(updateActionForLocalizedStrings).isNotNull();
        assertThat(updateActionForLocalizedStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithSameValuesButDifferentFieldOrder_ShouldNotBuildUpdateAction() {
        final LocalizedString existingLocalisedString = LocalizedString.of(LOCALE, "Milch")
                .plus(Locale.FRENCH, "Lait")
                .plus(Locale.ENGLISH, "Milk");

        final LocalizedString newLocalisedString = LocalizedString.of(LOCALE, "Milch")
                .plus(Locale.ENGLISH, "Milk")
                .plus(Locale.FRENCH, "Lait");

        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForLocalizedStrings(existingLocalisedString, newLocalisedString, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForLocalizedStrings_WithNullValues_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForLocalizedStrings(null, null, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForReferences_WithDifferentTypeReferenceIds_ShouldBuildUpdateAction() {
        final Reference<Category> existingCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        Reference<Category> newCategoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "2");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(existingCategoryReference, newCategoryReference, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForReferences.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForReferences_WithOneNullReference_ShouldBuildUpdateAction() {
        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(null, categoryReference, mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForReferences.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForReferences_WithSameCategoryReferences_ShouldNotBuildUpdateAction() {
        final Reference<Category> categoryReference = Reference.ofResourceTypeIdAndId(MOCK_CATEGORY_REFERENCE_TYPE, "1");
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(categoryReference, categoryReference, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForReferences_WithNullReferences_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForReferences =
                buildUpdateActionForReferences(null, null, mockUpdateAction);

        assertThat(updateActionForReferences).isNotNull();
        assertThat(updateActionForReferences).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_WithDifferentStrings_ShouldBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings("1", "2", mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForStrings_WithOneNullString_ShouldBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings(null, "2", mockUpdateAction);

        UpdateAction<Category> categoryUpdateAction = updateActionForStrings.orElse(null);
        assertThat(categoryUpdateAction).isNotNull();
        assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
    }

    @Test
    public void buildUpdateActionForStrings_WithSameStrings_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));

        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings("1", "1", mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }

    @Test
    public void buildUpdateActionForStrings_WithNullStrings_ShouldNotBuildUpdateAction() {
        final UpdateAction<Category> mockUpdateAction = ChangeName.of(
                LocalizedString.of(LOCALE, MOCK_EXISTING_CATEGORY_NAME));
        final Optional<UpdateAction<Category>> updateActionForStrings =
                buildUpdateActionForStrings(null, null, mockUpdateAction);

        assertThat(updateActionForStrings).isNotNull();
        assertThat(updateActionForStrings).isNotPresent();
    }
}
