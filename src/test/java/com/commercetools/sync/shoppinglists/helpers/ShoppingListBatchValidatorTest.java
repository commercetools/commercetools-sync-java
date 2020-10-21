package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.LINE_ITEM_DRAFT_IS_NULL;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.LINE_ITEM_DRAFT_SKU_NOT_SET;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_IS_NULL;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_NAME_NOT_SET;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.TEXT_LINE_ITEM_DRAFT_IS_NULL;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.TEXT_LINE_ITEM_DRAFT_NAME_NOT_SET;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListBatchValidatorTest {

    private ShoppingListSyncOptions syncOptions;
    private ShoppingListSyncStatistics syncStatistics;
    private List<String> errorCallBackMessages;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = ShoppingListSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) ->
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
    void validateAndCollectReferencedKeys_WithNullShoppingListDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(SHOPPING_LIST_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithShoppingListDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithShoppingListDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn(EMPTY);
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.empty());
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithShoppingListDraftWithNullName_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_NAME_NOT_SET, shoppingListDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithShoppingListDraftWithEmptyName_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.of());
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_NAME_NOT_SET, shoppingListDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithLineItemDraftIsNull_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
        when(shoppingListDraft.getLineItems()).thenReturn(singletonList(null));

        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(LINE_ITEM_DRAFT_IS_NULL, 0, shoppingListDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithLineItemDraftWithNullSku_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
        when(shoppingListDraft.getLineItems()).thenReturn(singletonList(LineItemDraftBuilder.ofSku("",
            1L).build()));

        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(LINE_ITEM_DRAFT_SKU_NOT_SET, 0, shoppingListDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithTextLineItemDraftIsNull_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
        when(shoppingListDraft.getLineItems()).thenReturn(singletonList(LineItemDraftBuilder.ofSku("123",
            1L).build()));
        when(shoppingListDraft.getTextLineItems()).thenReturn(singletonList(null));
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(TEXT_LINE_ITEM_DRAFT_IS_NULL, 0, shoppingListDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithLineItemAndTextLineItemError_ShouldHaveOneCallback() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
        when(shoppingListDraft.getLineItems()).thenReturn(singletonList(LineItemDraftBuilder.ofSku("",
            1L).build()));
        when(shoppingListDraft.getTextLineItems()).thenReturn(singletonList(null));

        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format("%s,%s", format(LINE_ITEM_DRAFT_SKU_NOT_SET, 0, shoppingListDraft.getKey()),
                format(TEXT_LINE_ITEM_DRAFT_IS_NULL,0, shoppingListDraft.getKey())));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithTextLineItemDraftWithEmptyName_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
        when(shoppingListDraft.getLineItems()).thenReturn(singletonList(LineItemDraftBuilder.ofSku("123",
            1L).build()));
        when(shoppingListDraft.getTextLineItems()).thenReturn(
            singletonList(TextLineItemDraftBuilder.of(LocalizedString.empty(),
                1L).build()));
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(TEXT_LINE_ITEM_DRAFT_NAME_NOT_SET, 0, shoppingListDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithTextLineItemDraftWithNullName_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(shoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("validDraftName"));
        when(shoppingListDraft.getLineItems()).thenReturn(singletonList(LineItemDraftBuilder.ofSku("123",
            1L).build()));
        when(shoppingListDraft.getTextLineItems()).thenReturn(
            singletonList(TextLineItemDraftBuilder.of(null,
                1L).build()));
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(TEXT_LINE_ITEM_DRAFT_NAME_NOT_SET, 0, shoppingListDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
        final ShoppingListDraft validShoppingListDraft = mock(ShoppingListDraft.class);
        when(validShoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(validShoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("name"));
        when(validShoppingListDraft.getCustom())
            .thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()));
        LineItemDraft lineItem = mock(LineItemDraft.class);
        when(lineItem.getCustom()).thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("lineItemTypeKey",
            emptyMap()));
        when(lineItem.getSku()).thenReturn("validSku");
        when(validShoppingListDraft.getLineItems()).thenReturn(singletonList(lineItem));
        TextLineItemDraft textLineItem = mock(TextLineItemDraft.class);
        when(textLineItem.getCustom()).thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("textLineItemTypeKey",
            emptyMap()));
        when(textLineItem.getName()).thenReturn(LocalizedString.ofEnglish("validName"));
        when(validShoppingListDraft.getTextLineItems()).thenReturn(singletonList(textLineItem));
        Customer customer = mock(Customer.class);
        when(customer.getKey()).thenReturn("customerKey");
        final ResourceIdentifier<Customer> customerResourceIdentifier = ResourceIdentifier.ofKey(customer.getKey());
        when(validShoppingListDraft.getCustomer()).thenReturn(customerResourceIdentifier);

        final ShoppingListDraft validShoppingListDraftWithoutReferences = mock(ShoppingListDraft.class);
        when(validShoppingListDraftWithoutReferences.getKey()).thenReturn("validDraftKey");
        when(validShoppingListDraftWithoutReferences.getName()).thenReturn(LocalizedString.ofEnglish("name"));
        when(validShoppingListDraftWithoutReferences.getLineItems()).thenReturn(null);
        when(validShoppingListDraftWithoutReferences.getTextLineItems()).thenReturn(null);

        final ShoppingListDraft invalidShoppingListDraft = mock(ShoppingListDraft.class);

        final ShoppingListBatchValidator shoppingListBatchValidator =
            new ShoppingListBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<ShoppingListDraft>, ShoppingListBatchValidator.ReferencedKeys> pair
            = shoppingListBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validShoppingListDraft, invalidShoppingListDraft, validShoppingListDraftWithoutReferences));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, invalidShoppingListDraft.getName()));
        assertThat(pair.getLeft())
            .containsExactlyInAnyOrder(validShoppingListDraft, validShoppingListDraftWithoutReferences);
        assertThat(pair.getRight().getTypeKeys())
            .containsExactlyInAnyOrder("typeKey", "lineItemTypeKey", "textLineItemTypeKey");
        assertThat(pair.getRight().getCustomerKeys())
            .containsExactlyInAnyOrder("customerKey");
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyKeys_ShouldNotCollectKeys() {
        final ShoppingListDraft validShoppingListDraft = mock(ShoppingListDraft.class);
        when(validShoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(validShoppingListDraft.getName()).thenReturn(LocalizedString.ofEnglish("name"));
        when(validShoppingListDraft.getCustom())
            .thenReturn(CustomFieldsDraft.ofTypeKeyAndJson(EMPTY, emptyMap()));
        LineItemDraft lineItem = mock(LineItemDraft.class);
        when(lineItem.getCustom()).thenReturn(CustomFieldsDraft.ofTypeKeyAndJson(EMPTY,
            emptyMap()));
        when(lineItem.getSku()).thenReturn("validSku");
        when(validShoppingListDraft.getLineItems()).thenReturn(singletonList(lineItem));
        TextLineItemDraft textLineItem = mock(TextLineItemDraft.class);
        when(textLineItem.getCustom()).thenReturn(CustomFieldsDraft.ofTypeKeyAndJson(EMPTY,
            emptyMap()));
        when(textLineItem.getName()).thenReturn(LocalizedString.ofEnglish("validName"));
        when(validShoppingListDraft.getTextLineItems()).thenReturn(singletonList(textLineItem));
        final ResourceIdentifier<Customer> customerResourceIdentifier = ResourceIdentifier.ofKey(EMPTY);
        when(validShoppingListDraft.getCustomer()).thenReturn(customerResourceIdentifier);

        final ShoppingListBatchValidator shoppingListBatchValidator =
            new ShoppingListBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<ShoppingListDraft>, ShoppingListBatchValidator.ReferencedKeys> pair
            = shoppingListBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validShoppingListDraft));

        assertThat(pair.getLeft()).contains(validShoppingListDraft);
        assertThat(pair.getRight().getCustomerKeys()).isEmpty();
        assertThat(pair.getRight().getTypeKeys()).isEmpty();
        assertThat(errorCallBackMessages).hasSize(0);
    }

    @Nonnull
    private Set<ShoppingListDraft> getValidDrafts(@Nonnull final List<ShoppingListDraft> shoppingListDrafts) {
        final ShoppingListBatchValidator shoppingListBatchValidator =
            new ShoppingListBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<ShoppingListDraft>, ShoppingListBatchValidator.ReferencedKeys> pair =
            shoppingListBatchValidator.validateAndCollectReferencedKeys(shoppingListDrafts);
        return pair.getLeft();
    }
}
