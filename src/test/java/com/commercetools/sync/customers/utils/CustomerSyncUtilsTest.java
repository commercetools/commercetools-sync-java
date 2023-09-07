package com.commercetools.sync.customers.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomFieldActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomTypeActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerSyncUtilsTest {

  private static final String CUSTOM_TYPE_ID = "id";
  private static final String CUSTOM_FIELD_NAME = "field";
  private static final String CUSTOM_FIELD_VALUE = "value";

  private Customer oldCustomer;

  @BeforeEach
  void setup() {
    oldCustomer = mock(Customer.class);
    when(oldCustomer.getEmail()).thenReturn("email");

    final CustomFields customFields = mock(CustomFields.class);
    when(customFields.getType()).thenReturn(TypeReferenceBuilder.of().id(CUSTOM_TYPE_ID).build());

    FieldContainerBuilder fieldContainer = new FieldContainerBuilder();
    fieldContainer.addValue(
        CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode(CUSTOM_FIELD_VALUE));
    when(customFields.getFields()).thenReturn(fieldContainer.build());

    when(oldCustomer.getCustom()).thenReturn(customFields);
  }

  @Test
  void buildActions_WithDifferentCustomType_ShouldBuildUpdateAction() {
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id("newId").build())
            .fields(
                FieldContainerBuilder.of()
                    .addValue("newField", JsonNodeFactory.instance.textNode("newValue"))
                    .build())
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").custom(customFieldsDraft).build();

    final List<CustomerUpdateAction> actions =
        CustomerSyncUtils.buildActions(
            oldCustomer,
            newCustomer,
            CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            CustomerSetCustomTypeActionBuilder.of()
                .type(
                    TypeResourceIdentifierBuilder.of()
                        .id(customFieldsDraft.getType().getId())
                        .build())
                .fields(customFieldsDraft.getFields())
                .build());
  }

  @Test
  void buildActions_WithSameCustomTypeWithNewCustomFields_ShouldBuildUpdateAction() {
    final CustomFieldsDraft sameCustomFieldDraftWithNewCustomField =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id(CUSTOM_TYPE_ID).build())
            .fields(
                FieldContainerBuilder.of()
                    .addValue(
                        CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode(CUSTOM_FIELD_VALUE))
                    .addValue("name_2", JsonNodeFactory.instance.textNode("value_2"))
                    .build())
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .custom(sameCustomFieldDraftWithNewCustomField)
            .build();

    final List<CustomerUpdateAction> actions =
        CustomerSyncUtils.buildActions(
            oldCustomer,
            newCustomer,
            CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            CustomerSetCustomFieldActionBuilder.of()
                .name("name_2")
                .value(JsonNodeFactory.instance.textNode("value_2"))
                .build());
  }

  @Test
  void buildActions_WithSameCustomTypeWithDifferentCustomFieldValues_ShouldBuildUpdateAction() {
    final CustomFieldsDraft sameCustomFieldDraftWithNewValue =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id(CUSTOM_TYPE_ID).build())
            .fields(
                FieldContainerBuilder.of()
                    .addValue(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("newValue"))
                    .build())
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .custom(sameCustomFieldDraftWithNewValue)
            .build();

    final List<CustomerUpdateAction> actions =
        CustomerSyncUtils.buildActions(
            oldCustomer,
            newCustomer,
            CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions)
        .containsExactly(
            CustomerSetCustomFieldActionBuilder.of()
                .name(CUSTOM_FIELD_NAME)
                .value(JsonNodeFactory.instance.textNode("newValue"))
                .build());
  }

  @Test
  void buildActions_WithJustNewCartDiscountHasNullCustomType_ShouldBuildUpdateAction() {
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .custom((CustomFieldsDraft) null)
            .build();

    final List<CustomerUpdateAction> actions =
        CustomerSyncUtils.buildActions(
            oldCustomer,
            newCustomer,
            CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());

    assertThat(actions).containsExactly(CustomerSetCustomTypeActionBuilder.of().build());
  }
}
