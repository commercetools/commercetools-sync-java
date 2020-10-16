package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.commercetools.sync.commons.MockUtils.getTypeMock;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomer;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListsReferenceResolutionUtilsTest {

    @Test
    void mapToShoppingListDrafts_WithExpandedReferences_ShouldReturnResourceIdentifiersWithKeys() {
        final Type mockCustomType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");
        final Customer mockCustomer = getMockCustomer(UUID.randomUUID().toString(), "customerKey");
        final ProductVariant mockProductVariant = mock(ProductVariant.class);
        when(mockProductVariant.getSku()).thenReturn("variant-sku");

        final List<ShoppingList> mockShoppingLists = new ArrayList<>();
        mockShoppingLists.add(null);

        for (int i = 0; i < 3; i++) {
            final ShoppingList mockShoppingList = mock(ShoppingList.class);

            final Reference<Customer> customerReference =
                Reference.ofResourceTypeIdAndObj(Customer.referenceTypeId(), mockCustomer);
            when(mockShoppingList.getCustomer()).thenReturn(customerReference);

            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference =
                Reference.ofResourceTypeIdAndObj(Type.referenceTypeId(), mockCustomType);

            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);

            final LineItem mockLineItem = mock(LineItem.class);
            when(mockLineItem.getVariant()).thenReturn(mockProductVariant);
            when(mockLineItem.getCustom()).thenReturn(mockCustomFields);

            when(mockShoppingList.getLineItems())
                .thenReturn(singletonList(mockLineItem));

            final TextLineItem mockTextLineItem = mock(TextLineItem.class);
            when(mockTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("textLineItemName"));
            when(mockTextLineItem.getCustom()).thenReturn(mockCustomFields);

            when(mockShoppingList.getTextLineItems()).thenReturn(singletonList(mockTextLineItem));

            mockShoppingLists.add(mockShoppingList);
        }

        final List<ShoppingListDraft> shoppingListDrafts = mapToShoppingListDrafts(mockShoppingLists);

        assertThat(shoppingListDrafts).hasSize(3);
        shoppingListDrafts.forEach(draft -> {
            assertThat(draft.getCustomer().getKey()).isEqualTo("customerKey");
            assertThat(draft.getCustom().getType().getKey()).isEqualTo("customTypeKey");

            assertThat(draft.getLineItems()).containsExactly(
                LineItemDraftBuilder
                    .ofSku("variant-sku", 0L)
                    .custom(CustomFieldsDraft.ofTypeKeyAndJson("customTypeKey", emptyMap()))
                    .build());

            assertThat(draft.getTextLineItems()).containsExactly(
                TextLineItemDraftBuilder
                    .of(LocalizedString.ofEnglish("textLineItemName"), 0L)
                    .custom(CustomFieldsDraft.ofTypeKeyAndJson("customTypeKey", emptyMap()))
                    .build());
        });
    }

    @Test
    void mapToShoppingListDrafts_WithNonExpandedReferences_ShouldReturnResourceIdentifiersWithKeys() {
        final String customTypeId = UUID.randomUUID().toString();
        final String customerId = UUID.randomUUID().toString();


        final List<ShoppingList> mockShoppingLists = new ArrayList<>();
        mockShoppingLists.add(null);

        for (int i = 0; i < 3; i++) {
            final ShoppingList mockShoppingList = mock(ShoppingList.class);

            final Reference<Customer> customerReference =
                Reference.ofResourceTypeIdAndId(Customer.referenceTypeId(), customerId);
            when(mockShoppingList.getCustomer()).thenReturn(customerReference);

            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndId(Type.referenceTypeId(),
                customTypeId);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);

            final LineItem mockLineItemWithNullVariant = mock(LineItem.class);
            when(mockLineItemWithNullVariant.getVariant()).thenReturn(null);

            when(mockShoppingList.getLineItems())
                .thenReturn(singletonList(mockLineItemWithNullVariant));

            final TextLineItem mockTextLineItem = mock(TextLineItem.class);
            when(mockTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("textLineItemName"));
            when(mockTextLineItem.getCustom()).thenReturn(mockCustomFields);

            when(mockShoppingList.getTextLineItems()).thenReturn(singletonList(mockTextLineItem));

            mockShoppingLists.add(mockShoppingList);
        }

        final List<ShoppingListDraft> shoppingListDrafts = mapToShoppingListDrafts(mockShoppingLists);

        assertThat(shoppingListDrafts).hasSize(3);
        shoppingListDrafts.forEach(draft -> {
            assertThat(draft.getCustomer().getId()).isEqualTo(customerId);
            assertThat(draft.getCustom().getType().getKey()).isNull();

            assertThat(draft.getLineItems()).isEqualTo(emptyList());

            assertThat(draft.getTextLineItems()).containsExactly(
                TextLineItemDraftBuilder
                    .of(LocalizedString.ofEnglish("textLineItemName"), 0L)
                    .custom(CustomFieldsDraft.ofTypeIdAndJson(customTypeId, emptyMap()))
                    .build());
        });
    }

    @Test
    void mapToShoppingListDrafts_WithOtherFields_ShouldReturnDraftsCorrectly() {

        final ShoppingList mockShoppingList = mock(ShoppingList.class);
        when(mockShoppingList.getName()).thenReturn(LocalizedString.ofEnglish("name"));
        when(mockShoppingList.getDescription()).thenReturn(LocalizedString.ofEnglish("desc"));
        when(mockShoppingList.getKey()).thenReturn("key");
        when(mockShoppingList.getSlug()).thenReturn(LocalizedString.ofEnglish("slug"));
        when(mockShoppingList.getDeleteDaysAfterLastModification()).thenReturn(2);

        when(mockShoppingList.getCustomer()).thenReturn(null);
        when(mockShoppingList.getCustom()).thenReturn(null);
        when(mockShoppingList.getLineItems()).thenReturn(null);
        when(mockShoppingList.getTextLineItems()).thenReturn(null);

        final List<ShoppingListDraft> shoppingListDrafts =
            mapToShoppingListDrafts(singletonList(mockShoppingList));

        assertThat(shoppingListDrafts).containsExactly(
            ShoppingListDraftBuilder
                .of(LocalizedString.ofEnglish("name"))
                .description(LocalizedString.ofEnglish("desc"))
                .key("key")
                .slug(LocalizedString.ofEnglish("slug"))
                .deleteDaysAfterLastModification(2)
                .build()
        );
    }

    @Test
    void buildShoppingListQuery_Always_ShouldReturnQueryWithAllNeededReferencesExpanded() {
        assertThat(buildShoppingListQuery().expansionPaths())
            .containsExactly(
                ExpansionPath.of("customer"),
                ExpansionPath.of("custom.type"),
                ExpansionPath.of("lineItems[*].variant"),
                ExpansionPath.of("lineItems[*].custom.type"),
                ExpansionPath.of("textLineItems[*].custom.type"));
    }
}
