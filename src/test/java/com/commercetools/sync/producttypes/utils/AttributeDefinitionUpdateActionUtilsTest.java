package com.commercetools.sync.producttypes.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeConstraintEnumDraft;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValue;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValueBuilder;
import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.AttributePlainEnumValueBuilder;
import com.commercetools.api.models.product_type.ProductTypeAddLocalizedEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeAddPlainEnumValueActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeAttributeConstraintActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeInputHintActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeIsSearchableActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeLocalizedEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueLabelActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangePlainEnumValueOrderActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeRemoveEnumValuesActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeSetInputTipActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.api.models.product_type.TextInputHint;
import com.commercetools.sync.producttypes.MockBuilderUtils;
import com.commercetools.sync.producttypes.helpers.ResourceToDraftConverters;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AttributeDefinitionUpdateActionUtilsTest {

  private static final AttributePlainEnumValue ENUM_VALUE_A =
      AttributePlainEnumValueBuilder.of().key("a").label("label_a").build();
  private static final AttributePlainEnumValue ENUM_VALUE_B =
      AttributePlainEnumValueBuilder.of().key("b").label("label_b").build();

  private static final AttributeLocalizedEnumValue LOCALIZED_ENUM_VALUE_A =
      AttributeLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("label_a")).build();
  private static final AttributeLocalizedEnumValue LOCALIZED_ENUM_VALUE_B =
      AttributeLocalizedEnumValueBuilder.of().key("b").label(ofEnglish("label_b")).build();

  @Test
  void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().label(ofEnglish("x")).build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().label(ofEnglish("y")).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeLabelUpdateAction(
            attributeDefinition, draft);

    // assertion
    assertThat(result)
        .contains(
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .label(ofEnglish("x"))
                .build());
  }

  @Test
  void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeLabelUpdateAction(
            attributeDefinition, draft);

    // assertion
    assertThat(result).isEmpty();
  }

  @Test
  void buildSetInputTipAction_WithDifferentValues_ShouldReturnAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .inputTip(ofEnglish("foo"))
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().inputTip(ofEnglish("bar")).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildSetInputTipUpdateAction(
            attributeDefinition, draft);

    // assertion
    assertThat(result)
        .contains(
            ProductTypeSetInputTipActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .inputTip(ofEnglish("foo"))
                .build());
  }

  @Test
  void buildSetInputTipAction_WithSameValues_ShouldReturnEmptyOptional() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .inputTip(ofEnglish("foo"))
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().inputTip(ofEnglish("foo")).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildSetInputTipUpdateAction(
            attributeDefinition, draft);

    // assertion
    assertThat(result).isEmpty();
  }

  @Test
  void buildSetInputTipAction_WithSourceNullValues_ShouldReturnAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .inputTip((LocalizedString) null)
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().inputTip(ofEnglish("foo")).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildSetInputTipUpdateAction(
            attributeDefinition, draft);

    // assertion
    assertThat(result)
        .contains(
            ProductTypeSetInputTipActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .inputTip((LocalizedString) null)
                .build());
  }

  @Test
  void buildSetInputTipAction_WithTargetNullValues_ShouldReturnAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .inputTip(ofEnglish("foo"))
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .inputTip((LocalizedString) null)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildSetInputTipUpdateAction(
            attributeDefinition, draft);

    // assertion
    assertThat(result)
        .contains(
            ProductTypeSetInputTipActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .inputTip(ofEnglish("foo"))
                .build());
  }

  @Test
  void buildChangeIsSearchableAction_WithDifferentValues_ShouldReturnAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().isSearchable(true).build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().isSearchable(false).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeIsSearchableUpdateAction(
            attributeDefinition, draft);

    assertThat(result)
        .contains(
            ProductTypeChangeIsSearchableActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .isSearchable(true)
                .build());
  }

  @Test
  void buildChangeIsSearchableAction_WithSameValues_ShouldReturnEmptyOptional() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().isSearchable(true).build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().isSearchable(true).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeIsSearchableUpdateAction(
            attributeDefinition, draft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildChangeIsSearchableAction_WithNullSourceAndNonDefaultTarget_ShouldBuildAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().isSearchable(null).build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().isSearchable(false).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeIsSearchableUpdateAction(
            attributeDefinition, draft);

    assertThat(result)
        .contains(
            ProductTypeChangeIsSearchableActionBuilder.of()
                .attributeName("foo")
                .isSearchable(true)
                .build());
  }

  @Test
  void buildChangeIsSearchableAction_WithNullSourceAndDefaultTarget_ShouldNotBuildAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().isSearchable(null).build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().isSearchable(true).build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeIsSearchableUpdateAction(
            attributeDefinition, draft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildChangeInputHintAction_WithDifferentValues_ShouldReturnAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .inputHint(TextInputHint.SINGLE_LINE)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeInputHintUpdateAction(
            attributeDefinition, draft);

    assertThat(result)
        .contains(
            ProductTypeChangeInputHintActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .newValue(TextInputHint.MULTI_LINE)
                .build());
  }

  @Test
  void buildChangeInputHintAction_WithSameValues_ShouldReturnEmptyOptional() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeInputHintUpdateAction(
            attributeDefinition, draft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildChangeInputHintAction_WithSourceNullValuesAndNonDefaultTargetValue_ShouldBuildAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().inputHint(null).build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .inputHint(TextInputHint.MULTI_LINE)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeInputHintUpdateAction(
            attributeDefinition, draft);

    assertThat(result)
        .contains(
            ProductTypeChangeInputHintActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .newValue(TextInputHint.SINGLE_LINE)
                .build());
  }

  @Test
  void buildChangeInputHintAction_WithSourceNullValuesAndDefaultTargetValue_ShouldNotBuildAction() {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder().inputHint(null).build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .inputHint(TextInputHint.SINGLE_LINE)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeInputHintUpdateAction(
            attributeDefinition, draft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildChangeAttributeConstraintAction_WithDifferentValues_ShouldBuildAction()
      throws UnsupportedOperationException {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .attributeConstraint(AttributeConstraintEnum.COMBINATION_UNIQUE)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeAttributeConstraintUpdateAction(
            attributeDefinition, draft);

    assertThat(result)
        .contains(
            ProductTypeChangeAttributeConstraintActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .newValue(AttributeConstraintEnumDraft.NONE)
                .build());
  }

  @Test
  void buildChangeAttributeConstraintAction_WithSameValues_ShouldReturnEmptyOptional()
      throws UnsupportedOperationException {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .attributeConstraint(AttributeConstraintEnum.COMBINATION_UNIQUE)
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .attributeConstraint(AttributeConstraintEnum.COMBINATION_UNIQUE)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeAttributeConstraintUpdateAction(
            attributeDefinition, draft);

    assertThat(result).isEmpty();
  }

  @Test
  void
      buildChangeAttributeConstraintAction_WithSourceNullValuesAndNonDefaultTarget_ShouldBuildAction()
          throws UnsupportedOperationException {
    // Preparation
    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .attributeConstraint(null)
            .build();

    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
            .build();

    // test
    final Optional<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildChangeAttributeConstraintUpdateAction(
            attributeDefinition, draft);

    assertThat(result)
        .contains(
            ProductTypeChangeAttributeConstraintActionBuilder.of()
                .attributeName(draft.getName())
                .newValue(AttributeConstraintEnumDraft.NONE)
                .build());
  }

  @Test
  void
      buildChangeAttributeConstraintAction_WithSourceSameForAllAndTargetCombinationUnique_ShouldThrowException() {
    // Preparation
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
            .build();

    final AttributeDefinitionDraft draft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .attributeConstraint(AttributeConstraintEnum.COMBINATION_UNIQUE)
            .build();
    // test
    try {
      AttributeDefinitionUpdateActionUtils.buildChangeAttributeConstraintUpdateAction(
          attributeDefinition, draft);
      fail("Expected an exception to be thrown");
    } catch (UnsupportedOperationException e) {
      assertThat(e)
          .hasMessage(
              "Invalid AttributeConstraint update to COMBINATION_UNIQUE. Only following updates are allowed: SameForAll to None and Unique to None.");
    }
  }

  @Test
  void buildActions_WithNewDifferentValues_ShouldReturnActions()
      throws UnsupportedOperationException {
    final AttributeDefinition old =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .label(ofEnglish("label1"))
            .isRequired(false)
            .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
            .inputTip(ofEnglish("inputTip1"))
            .inputHint(TextInputHint.SINGLE_LINE)
            .isSearchable(false)
            .build();

    final AttributeDefinitionDraft newDifferent =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .label(ofEnglish("label2"))
            .isRequired(true)
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .inputTip(ofEnglish("inputTip2"))
            .inputHint(TextInputHint.MULTI_LINE)
            .isSearchable(true)
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildActions(old, newDifferent);

    assertThat(result)
        .containsExactlyInAnyOrder(
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName(old.getName())
                .label(newDifferent.getLabel())
                .build(),
            ProductTypeSetInputTipActionBuilder.of()
                .attributeName(old.getName())
                .inputTip(newDifferent.getInputTip())
                .build(),
            ProductTypeChangeAttributeConstraintActionBuilder.of()
                .attributeName(old.getName())
                .newValue(AttributeConstraintEnumDraft.NONE)
                .build(),
            ProductTypeChangeInputHintActionBuilder.of()
                .attributeName(old.getName())
                .newValue(newDifferent.getInputHint())
                .build(),
            ProductTypeChangeIsSearchableActionBuilder.of()
                .attributeName(old.getName())
                .isSearchable(newDifferent.getIsSearchable())
                .build());
  }

  @Test
  void buildActions_WithSameValues_ShouldReturnEmpty() throws UnsupportedOperationException {
    final AttributeDefinition old = MockBuilderUtils.createMockAttributeDefinitionBuilder().build();

    final AttributeDefinitionDraft newSame =
        ResourceToDraftConverters.toAttributeDefinitionDraftBuilder(old).build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildActions(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildActions_WithStringAttributeTypesWithLabelChanges_ShouldBuildChangeLabelAction()
      throws UnsupportedOperationException {
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder().label(ofEnglish("label1")).build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .label(ofEnglish("label2"))
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildActions(
            attributeDefinition, attributeDefinitionDraft);

    assertThat(result)
        .containsExactly(
            ProductTypeChangeLabelActionBuilder.of()
                .attributeName("foo")
                .label(ofEnglish("label2"))
                .build());
  }

  @Test
  void buildActions_WithChangedSetOfEnumAttributeTypes_ShouldBuildEnumActions()
      throws UnsupportedOperationException {
    // preparation
    final AttributeDefinition oldDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        AttributePlainEnumValueBuilder.of()
                                            .key("c")
                                            .label("c")
                                            .build(),
                                        AttributePlainEnumValueBuilder.of()
                                            .key("d")
                                            .label("d")
                                            .build(),
                                        ENUM_VALUE_B)))
            .build();

    final AttributeDefinitionDraft newDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .enumBuilder()
                                    .values(
                                        ENUM_VALUE_A,
                                        AttributePlainEnumValueBuilder.of()
                                            .key(ENUM_VALUE_B.getKey())
                                            .label("newLabel")
                                            .build(),
                                        AttributePlainEnumValueBuilder.of()
                                            .key("c")
                                            .label("c")
                                            .build())))
            .build();

    // test
    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildActions(oldDefinition, newDraft);

    // assertion
    assertThat(result)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .keys("d")
                .build(),
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .newValue(
                    AttributePlainEnumValueBuilder.of()
                        .key(ENUM_VALUE_B.getKey())
                        .label("newLabel")
                        .build())
                .build(),
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .value(ENUM_VALUE_A)
                .build(),
            ProductTypeChangePlainEnumValueOrderActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .values(
                    asList(
                        ENUM_VALUE_A,
                        AttributePlainEnumValueBuilder.of()
                            .key(ENUM_VALUE_B.getKey())
                            .label("newLabel")
                            .build(),
                        AttributePlainEnumValueBuilder.of().key("c").label("c").build()))
                .build());
  }

  @Test
  void buildActions_WithChangedSetOfLocalizedEnumAttributeTypes_ShouldBuildEnumActions()
      throws UnsupportedOperationException {
    // preparation
    final AttributeDefinition oldDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        asList(
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("c")
                                                .label(ofEnglish("c"))
                                                .build(),
                                            LOCALIZED_ENUM_VALUE_B,
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("d")
                                                .label(ofEnglish("d"))
                                                .build()))))
            .build();

    final AttributeDefinitionDraft newDefinition =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .lenumBuilder()
                                    .values(
                                        asList(
                                            LOCALIZED_ENUM_VALUE_A,
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key(LOCALIZED_ENUM_VALUE_B.getKey())
                                                .label(ofEnglish("newLabel"))
                                                .build(),
                                            AttributeLocalizedEnumValueBuilder.of()
                                                .key("c")
                                                .label(ofEnglish("c"))
                                                .build()))))
            .build();

    // test
    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildActions(oldDefinition, newDefinition);

    // assertion
    assertThat(result)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .keys("d")
                .build(),
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .newValue(
                    AttributeLocalizedEnumValueBuilder.of()
                        .key(LOCALIZED_ENUM_VALUE_B.getKey())
                        .label(ofEnglish("newLabel"))
                        .build())
                .build(),
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .value(LOCALIZED_ENUM_VALUE_A)
                .build(),
            ProductTypeChangeLocalizedEnumValueOrderActionBuilder.of()
                .attributeName(oldDefinition.getName())
                .values(
                    asList(
                        LOCALIZED_ENUM_VALUE_A,
                        AttributeLocalizedEnumValueBuilder.of()
                            .key(LOCALIZED_ENUM_VALUE_B.getKey())
                            .label(ofEnglish("newLabel"))
                            .build(),
                        AttributeLocalizedEnumValueBuilder.of()
                            .key("c")
                            .label(ofEnglish("c"))
                            .build()))
                .build());
  }

  @Test
  void buildActions_WithNewPlainEnum_ShouldReturnAddEnumValueAction() {
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.enumBuilder().values(ENUM_VALUE_A))
            .build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.enumBuilder().values(ENUM_VALUE_A, ENUM_VALUE_B))
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildEnumUpdateActions(
            attributeDefinition, attributeDefinitionDraft);

    assertThat(result)
        .containsExactly(
            ProductTypeAddPlainEnumValueActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .value(ENUM_VALUE_B)
                .build());
  }

  @Test
  void buildActions_WithoutOldPlainEnum_ShouldReturnRemoveEnumValueAction() {
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.enumBuilder().values(ENUM_VALUE_A))
            .build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.enumBuilder().values(emptyList()))
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildEnumUpdateActions(
            attributeDefinition, attributeDefinitionDraft);

    assertThat(result)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .keys("a")
                .build());
  }

  @Test
  void buildActions_WitDifferentPlainEnumValueLabel_ShouldReturnChangeEnumValueLabelAction() {
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.enumBuilder().values(ENUM_VALUE_A))
            .build();

    final AttributePlainEnumValue enumValueDiffLabel =
        AttributePlainEnumValueBuilder.of().key("a").label("label_a_different").build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.enumBuilder().values(enumValueDiffLabel))
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildEnumUpdateActions(
            attributeDefinition, attributeDefinitionDraft);

    assertThat(result)
        .containsExactly(
            ProductTypeChangePlainEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .newValue(enumValueDiffLabel)
                .build());
  }

  @Test
  void buildActions_WithNewLocalizedEnum_ShouldReturnAddLocalizedEnumValueAction() {
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.lenumBuilder().values(LOCALIZED_ENUM_VALUE_A))
            .build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .lenumBuilder()
                        .values(LOCALIZED_ENUM_VALUE_A, LOCALIZED_ENUM_VALUE_B))
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildEnumUpdateActions(
            attributeDefinition, attributeDefinitionDraft);

    assertThat(result)
        .containsExactly(
            ProductTypeAddLocalizedEnumValueActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .value(LOCALIZED_ENUM_VALUE_B)
                .build());
  }

  @Test
  void buildActions_WithoutOldLocalizedEnum_ShouldReturnRemoveLocalizedEnumValueAction() {
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.lenumBuilder().values(LOCALIZED_ENUM_VALUE_A))
            .build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(attributeTypeBuilder -> attributeTypeBuilder.lenumBuilder().values(emptyList()))
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildEnumUpdateActions(
            attributeDefinition, attributeDefinitionDraft);

    assertThat(result)
        .containsExactly(
            ProductTypeRemoveEnumValuesActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .keys("a")
                .build());
  }

  @Test
  void
      buildActions_WithDifferentLocalizedEnumValueLabel_ShouldReturnChangeLocalizedEnumValueLabelAction() {
    final AttributeDefinition attributeDefinition =
        MockBuilderUtils.createMockAttributeDefinitionBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.lenumBuilder().values(LOCALIZED_ENUM_VALUE_A))
            .build();

    final AttributeLocalizedEnumValue localizedEnumValueDiffLabel =
        AttributeLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("label_a_diff")).build();

    final AttributeDefinitionDraft attributeDefinitionDraft =
        MockBuilderUtils.createMockAttributeDefinitionDraftBuilder()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder.lenumBuilder().values(localizedEnumValueDiffLabel))
            .build();

    final List<ProductTypeUpdateAction> result =
        AttributeDefinitionUpdateActionUtils.buildEnumUpdateActions(
            attributeDefinition, attributeDefinitionDraft);

    assertThat(result)
        .containsExactly(
            ProductTypeChangeLocalizedEnumValueLabelActionBuilder.of()
                .attributeName(attributeDefinition.getName())
                .newValue(localizedEnumValueDiffLabel)
                .build());
  }
}
