package com.commercetools.sync.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.CategoryChangeNameActionBuilder;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommonTypeUpdateActionUtilsTest {
  private static final String LOCALE = Locale.GERMAN.toLanguageTag();
  private static final String MOCK_OLD_CATEGORY_NAME = "categoryName";

  @Test
  void buildUpdateActionForLocalizedStrings_WithDifferentFieldValues_ShouldBuildUpdateAction() {
    final LocalizedString oldLocalisedString =
        LocalizedStringBuilder.of().addValue(LOCALE, "Apfel").build();
    final LocalizedString newLocalisedString =
        LocalizedStringBuilder.of().addValue(LOCALE, "Milch").build();

    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForLocalizedStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

    CategoryUpdateAction categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
  }

  @Test
  void buildUpdateActionForLocalizedStrings_WithEmptyOldFields_ShouldBuildUpdateAction() {
    final LocalizedString oldLocalisedString = LocalizedStringBuilder.of().build();
    final LocalizedString newLocalisedString =
        LocalizedStringBuilder.of().addValue(LOCALE, "Milch").build();
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForLocalizedStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

    CategoryUpdateAction categoryUpdateAction = updateActionForLocalizedStrings.orElse(null);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
  }

  @Test
  void buildUpdateActionForLocalizedStrings_WithEmptyFields_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();
    final Optional<CategoryUpdateAction> updateActionForLocalizedStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            LocalizedStringBuilder.of().build(),
            LocalizedStringBuilder.of().build(),
            () -> mockUpdateAction);

    assertThat(updateActionForLocalizedStrings).isNotNull();
    assertThat(updateActionForLocalizedStrings).isNotPresent();
  }

  @Test
  void buildUpdateActionForLocalizedStrings_WithSameValues_ShouldNotBuildUpdateAction() {
    final LocalizedString oldLocalisedString =
        LocalizedStringBuilder.of().addValue(LOCALE, "Milch").build();
    final LocalizedString newLocalisedString =
        LocalizedStringBuilder.of().addValue(LOCALE, "Milch").build();
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForLocalizedStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

    assertThat(updateActionForLocalizedStrings).isNotNull();
    assertThat(updateActionForLocalizedStrings).isNotPresent();
  }

  @Test
  void
      buildUpdateActionForLocalizedStrings_WithSameValuesButDifferentFieldOrder_ShouldNotBuildUpdateAction() {
    final LocalizedString oldLocalisedString =
        LocalizedStringBuilder.of()
            .addValue(LOCALE, "Milch")
            .addValue(Locale.FRENCH.toLanguageTag(), "Lait")
            .addValue(Locale.ENGLISH.toLanguageTag(), "Milk")
            .build();

    final LocalizedString newLocalisedString =
        LocalizedStringBuilder.of()
            .addValue(LOCALE, "Milch")
            .addValue(Locale.ENGLISH.toLanguageTag(), "Milk")
            .addValue(Locale.FRENCH.toLanguageTag(), "Lait")
            .build();

    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            oldLocalisedString, newLocalisedString, () -> mockUpdateAction);

    assertThat(updateActionForReferences).isNotNull();
    assertThat(updateActionForReferences).isNotPresent();
  }

  @Test
  void buildUpdateActionForLocalizedStrings_WithNullValues_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(null, null, () -> mockUpdateAction);

    assertThat(updateActionForReferences).isNotNull();
    assertThat(updateActionForReferences).isNotPresent();
  }

  @Test
  void buildUpdateActionForLocalizedStrings_WithNullUpdateAction_ShouldNotBuildUpdateAction() {
    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(null, null, () -> null);
    assertThat(updateActionForReferences).isNotNull();
    assertThat(updateActionForReferences).isNotPresent();
  }

  @Test
  void buildUpdateActionForReferences_WithDifferentTypeReferenceIds_ShouldBuildUpdateAction() {
    final Reference oldCategoryReference = CategoryReferenceBuilder.of().id("1").build();
    final Reference newCategoryReference = CategoryReferenceBuilder.of().id("2").build();

    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            oldCategoryReference, newCategoryReference, () -> mockUpdateAction);

    CategoryUpdateAction categoryUpdateAction = updateActionForReferences.orElse(null);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
  }

  @Test
  void buildUpdateActionForReferences_WithOneNullReference_ShouldBuildUpdateAction() {
    final Reference categoryReference = CategoryReferenceBuilder.of().id("1").build();

    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            null, categoryReference, () -> mockUpdateAction);

    CategoryUpdateAction categoryUpdateAction = updateActionForReferences.orElse(null);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
  }

  @Test
  void buildUpdateActionForReferences_WithSameCategoryReferences_ShouldNotBuildUpdateAction() {
    final Reference categoryReference = CategoryReferenceBuilder.of().id("1").build();

    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(
            categoryReference, categoryReference, () -> mockUpdateAction);

    assertThat(updateActionForReferences).isNotNull();
    assertThat(updateActionForReferences).isNotPresent();
  }

  @Test
  void buildUpdateActionForReferences_WithNullReferences_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();
    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(null, null, () -> mockUpdateAction);

    assertThat(updateActionForReferences).isNotNull();
    assertThat(updateActionForReferences).isNotPresent();
  }

  @Test
  void buildUpdateActionForReferences_WithNullUpdateAction_ShouldNotBuildUpdateAction() {
    final Optional<CategoryUpdateAction> updateActionForReferences =
        CommonTypeUpdateActionUtils.buildUpdateAction(null, null, () -> null);

    assertThat(updateActionForReferences).isNotNull();
    assertThat(updateActionForReferences).isNotPresent();
  }

  @Test
  void buildUpdateActionForStrings_WithDifferentStrings_ShouldBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction("1", "2", () -> mockUpdateAction);

    CategoryUpdateAction categoryUpdateAction = updateActionForStrings.orElse(null);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
  }

  @Test
  void buildUpdateActionForStrings_WithOneNullString_ShouldBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();
    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction(null, "2", () -> mockUpdateAction);

    CategoryUpdateAction categoryUpdateAction = updateActionForStrings.orElse(null);
    assertThat(categoryUpdateAction).isNotNull();
    assertThat(categoryUpdateAction.getAction()).isEqualTo("changeName");
  }

  @Test
  void buildUpdateActionForStrings_WithSameStrings_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();
    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction("1", "1", () -> mockUpdateAction);

    assertThat(updateActionForStrings).isNotNull();
    assertThat(updateActionForStrings).isNotPresent();
  }

  @Test
  void buildUpdateActionForStrings_WithNullStrings_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction(null, null, () -> mockUpdateAction);

    assertThat(updateActionForStrings).isNotNull();
    assertThat(updateActionForStrings).isNotPresent();
  }

  @Test
  void buildUpdateActionForStrings_WithNullUpdateAction_ShouldNotBuildUpdateAction() {
    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateAction(null, null, () -> null);

    assertThat(updateActionForStrings).isNotNull();
    assertThat(updateActionForStrings).isNotPresent();
  }

  @Test
  void buildUpdateActionForReferences_WithSameValues_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateActionForReferences(
            CategoryReferenceBuilder.of().id("foo").build(),
            CategoryResourceIdentifierBuilder.of().id("foo").build(),
            () -> mockUpdateAction);

    assertThat(updateActionForStrings).isNotNull();
    assertThat(updateActionForStrings).isNotPresent();
  }

  @Test
  void buildUpdateActionForReferences_WithDiffValues_ShouldBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();
    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateActionForReferences(
            CategoryReferenceBuilder.of().id("foo").build(),
            CategoryResourceIdentifierBuilder.of().id("bar").build(),
            () -> mockUpdateAction);

    assertThat(updateActionForStrings).isNotNull();
    assertThat(updateActionForStrings).contains(mockUpdateAction);
  }

  @Test
  void
      buildUpdateActionForReferences_WithSameValuesDifferentInterface_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();
    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateActionForReferences(
            CategoryReferenceBuilder.of().id("foo").build(),
            CustomerResourceIdentifierBuilder.of().id("foo").build(),
            () -> mockUpdateAction);

    assertThat(updateActionForStrings).isNotNull();
    assertThat(updateActionForStrings).isEmpty();
  }

  @Test
  void
      buildUpdateActionForReferences_WithDiffValuesDifferentInterface_ShouldNotBuildUpdateAction() {
    final CategoryUpdateAction mockUpdateAction =
        CategoryChangeNameActionBuilder.of()
            .name(LocalizedStringBuilder.of().addValue(LOCALE, MOCK_OLD_CATEGORY_NAME).build())
            .build();

    final Optional<CategoryUpdateAction> updateActionForStrings =
        CommonTypeUpdateActionUtils.buildUpdateActionForReferences(
            CategoryReferenceBuilder.of().id("foo").build(),
            CustomerResourceIdentifierBuilder.of().id("bar").build(),
            () -> mockUpdateAction);

    assertThat(updateActionForStrings).isNotNull();
    assertThat(updateActionForStrings).contains(mockUpdateAction);
  }

  @Test
  void areResourceIdentifiersEqual_WithDiffValues_ShouldBeFalse() {
    final boolean result =
        CommonTypeUpdateActionUtils.areResourceIdentifiersEqual(
            CategoryReferenceBuilder.of().id("foo").build(),
            CategoryResourceIdentifierBuilder.of().id("bar").build());

    assertThat(result).isFalse();
  }

  @Test
  void areResourceIdentifiersEqual_WithSameValues_ShouldBeTrue() {
    final boolean result =
        CommonTypeUpdateActionUtils.areResourceIdentifiersEqual(
            CategoryReferenceBuilder.of().id("foo").build(),
            CategoryResourceIdentifierBuilder.of().id("foo").build());

    assertThat(result).isTrue();
  }

  @Test
  void areResourceIdentifiersEqual_WithDiffValuesDifferentInterface_ShouldBeFalse() {
    final boolean result =
        CommonTypeUpdateActionUtils.areResourceIdentifiersEqual(
            CategoryReferenceBuilder.of().id("foo").build(),
            CustomerResourceIdentifierBuilder.of().id("bar").build());

    assertThat(result).isFalse();
  }

  @Test
  void areResourceIdentifiersEqual_WithSameValuesDifferentInterface_ShouldBeTrue() {
    final boolean result =
        CommonTypeUpdateActionUtils.areResourceIdentifiersEqual(
            CategoryReferenceBuilder.of().id("foo").build(),
            CustomerResourceIdentifierBuilder.of().id("foo").build());

    assertThat(result).isTrue();
  }
}
