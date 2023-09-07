package com.commercetools.sync.shoppinglists.helpers;

import static java.lang.String.format;
import static java.util.Collections.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerResourceIdentifier;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingListBatchValidatorTest {

  private ShoppingListSyncOptions syncOptions;
  private ShoppingListSyncStatistics syncStatistics;
  private List<String> errorCallBackMessages;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);

    syncOptions =
        ShoppingListSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorCallBackMessages.add(exception.getMessage()))
            .build();
    syncStatistics = mock(ShoppingListSyncStatistics.class);
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(emptyList());

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullShoppingListDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithShoppingListDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET,
                shoppingListDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithShoppingListDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn(EMPTY);
    when(shoppingListDraft.getName()).thenReturn(LocalizedString.empty());
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET,
                shoppingListDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithShoppingListDraftWithNullName_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_NAME_NOT_SET,
                shoppingListDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithShoppingListDraftWithEmptyName_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(shoppingListDraft.getName()).thenReturn(LocalizedString.of());
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_NAME_NOT_SET,
                shoppingListDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithLineItemDraftIsNull_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
    when(shoppingListDraft.getLineItems()).thenReturn(singletonList(null));

    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.LINE_ITEM_DRAFT_IS_NULL, 0, shoppingListDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithLineItemDraftWithNullSku_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
    when(shoppingListDraft.getLineItems())
        .thenReturn(
            singletonList(ShoppingListLineItemDraftBuilder.of().sku("").quantity(1L).build()));

    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.LINE_ITEM_DRAFT_SKU_NOT_SET,
                0,
                shoppingListDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithTextLineItemDraftIsNull_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
    when(shoppingListDraft.getLineItems())
        .thenReturn(
            singletonList(ShoppingListLineItemDraftBuilder.of().sku("123").quantity(1L).build()));
    when(shoppingListDraft.getTextLineItems()).thenReturn(singletonList(null));
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.TEXT_LINE_ITEM_DRAFT_IS_NULL,
                0,
                shoppingListDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithLineItemAndTextLineItemError_ShouldHaveOneCallback() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
    when(shoppingListDraft.getLineItems())
        .thenReturn(
            singletonList(ShoppingListLineItemDraftBuilder.of().sku("").quantity(1L).build()));
    when(shoppingListDraft.getTextLineItems()).thenReturn(singletonList(null));

    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            format(
                "%s,%s",
                String.format(
                    ShoppingListBatchValidator.LINE_ITEM_DRAFT_SKU_NOT_SET,
                    0,
                    shoppingListDraft.getKey()),
                String.format(
                    ShoppingListBatchValidator.TEXT_LINE_ITEM_DRAFT_IS_NULL,
                    0,
                    shoppingListDraft.getKey())));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithTextLineItemDraftWithEmptyName_ShouldHaveValidationErrorAndEmptyResult() {
    final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
    when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
    when(shoppingListDraft.getLineItems())
        .thenReturn(
            singletonList(ShoppingListLineItemDraftBuilder.of().sku("123").quantity(1L).build()));
    when(shoppingListDraft.getTextLineItems())
        .thenReturn(
            singletonList(
                TextLineItemDraftBuilder.of().name(LocalizedString.empty()).quantity(1L).build()));
    final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.TEXT_LINE_ITEM_DRAFT_NAME_NOT_SET,
                0,
                shoppingListDraft.getKey()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
    final ShoppingListDraft validShoppingListDraft = mock(ShoppingListDraft.class);
    when(validShoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(validShoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("name"));
    when(validShoppingListDraft.getCustom())
        .thenReturn(
            CustomFieldsDraftBuilder.of()
                .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key("typeKey"))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    final ShoppingListLineItemDraft lineItem = mock(ShoppingListLineItemDraft.class);
    when(lineItem.getCustom())
        .thenReturn(
            CustomFieldsDraftBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.key("lineItemTypeKey"))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    when(lineItem.getSku()).thenReturn("validSku");
    when(validShoppingListDraft.getLineItems()).thenReturn(List.of(lineItem));
    final TextLineItemDraft textLineItem = mock(TextLineItemDraft.class);
    when(textLineItem.getCustom())
        .thenReturn(
            CustomFieldsDraftBuilder.of()
                .type(
                    typeResourceIdentifierBuilder ->
                        typeResourceIdentifierBuilder.key("textLineItemTypeKey"))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    when(textLineItem.getName()).thenReturn(LocalizedString.ofEnglish("validName"));
    when(validShoppingListDraft.getTextLineItems()).thenReturn(singletonList(textLineItem));
    final Customer customer = mock(Customer.class);
    when(customer.getKey()).thenReturn("customerKey");
    final CustomerResourceIdentifier customerResourceIdentifier =
        CustomerResourceIdentifierBuilder.of().key(customer.getKey()).build();
    when(validShoppingListDraft.getCustomer()).thenReturn(customerResourceIdentifier);

    final ShoppingListDraft validShoppingListDraftWithoutReferences = mock(ShoppingListDraft.class);
    when(validShoppingListDraftWithoutReferences.getKey()).thenReturn("validDraftKey");
    when(validShoppingListDraftWithoutReferences.getName())
        .thenReturn(LocalizedString.ofEnglish("name"));
    when(validShoppingListDraftWithoutReferences.getLineItems()).thenReturn(null);
    when(validShoppingListDraftWithoutReferences.getTextLineItems()).thenReturn(null);

    final ShoppingListDraft invalidShoppingListDraft = mock(ShoppingListDraft.class);

    final ShoppingListBatchValidator shoppingListBatchValidator =
        new ShoppingListBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ShoppingListDraft>, ShoppingListBatchValidator.ReferencedKeys> pair =
        shoppingListBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(
                validShoppingListDraft,
                invalidShoppingListDraft,
                validShoppingListDraftWithoutReferences));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            String.format(
                ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET,
                invalidShoppingListDraft.getName()));
    assertThat(pair.getLeft())
        .containsExactlyInAnyOrder(validShoppingListDraft, validShoppingListDraftWithoutReferences);
    assertThat(pair.getRight().getTypeKeys())
        .containsExactlyInAnyOrder("typeKey", "lineItemTypeKey", "textLineItemTypeKey");
    assertThat(pair.getRight().getCustomerKeys()).containsExactlyInAnyOrder("customerKey");
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyKeys_ShouldNotCollectKeys() {
    final ShoppingListDraft validShoppingListDraft = mock(ShoppingListDraft.class);
    when(validShoppingListDraft.getKey()).thenReturn("validDraftKey");
    when(validShoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("name"));
    when(validShoppingListDraft.getCustom())
        .thenReturn(
            CustomFieldsDraftBuilder.of()
                .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(EMPTY))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    final ShoppingListLineItemDraft lineItem = mock(ShoppingListLineItemDraft.class);
    when(lineItem.getCustom())
        .thenReturn(
            CustomFieldsDraftBuilder.of()
                .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(EMPTY))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    when(lineItem.getSku()).thenReturn("validSku");
    when(validShoppingListDraft.getLineItems()).thenReturn(singletonList(lineItem));
    final TextLineItemDraft textLineItem = mock(TextLineItemDraft.class);
    when(textLineItem.getCustom())
        .thenReturn(
            CustomFieldsDraftBuilder.of()
                .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(EMPTY))
                .fields(fieldContainerBuilder -> fieldContainerBuilder.values(emptyMap()))
                .build());
    when(textLineItem.getName()).thenReturn(LocalizedString.ofEnglish("validName"));
    when(validShoppingListDraft.getTextLineItems()).thenReturn(singletonList(textLineItem));
    final CustomerResourceIdentifier customerResourceIdentifier =
        CustomerResourceIdentifierBuilder.of().key(EMPTY).build();
    when(validShoppingListDraft.getCustomer()).thenReturn(customerResourceIdentifier);

    final ShoppingListBatchValidator shoppingListBatchValidator =
        new ShoppingListBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ShoppingListDraft>, ShoppingListBatchValidator.ReferencedKeys> pair =
        shoppingListBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validShoppingListDraft));

    assertThat(pair.getLeft()).contains(validShoppingListDraft);
    assertThat(pair.getRight().getCustomerKeys()).isEmpty();
    assertThat(pair.getRight().getTypeKeys()).isEmpty();
    assertThat(errorCallBackMessages).hasSize(0);
  }

  @Nonnull
  private Set<ShoppingListDraft> getValidDrafts(
      @Nonnull final List<ShoppingListDraft> shoppingListDrafts) {
    final ShoppingListBatchValidator shoppingListBatchValidator =
        new ShoppingListBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<ShoppingListDraft>, ShoppingListBatchValidator.ReferencedKeys> pair =
        shoppingListBatchValidator.validateAndCollectReferencedKeys(shoppingListDrafts);
    return pair.getLeft();
  }
}
