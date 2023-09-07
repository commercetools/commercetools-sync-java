package com.commercetools.sync.commons.utils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerSetCustomFieldAction;
import com.commercetools.api.models.customer.CustomerSetCustomTypeAction;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.models.Custom;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.models.CustomerCustomTypeAdapter;
import com.commercetools.sync.customers.utils.CustomerCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithCustomerResource_ShouldBuildCustomerUpdateAction() {
    final String newCustomTypeId = UUID.randomUUID().toString();

    final CustomerUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                CustomerCustomTypeAdapter.of(mock(Customer.class)),
                CustomerCustomActionBuilder.of(),
                null,
                CustomerCustomTypeAdapter::getId,
                Custom::getTypeId,
                customerResource -> null,
                CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(CustomerSetCustomTypeAction.class);
    assertThat((CustomerSetCustomTypeAction) updateAction)
        .satisfies(
            customerSetCustomTypeAction -> {
              assertThat(customerSetCustomTypeAction.getType())
                  .isEqualTo(TypeResourceIdentifierBuilder.of().id(newCustomTypeId).build());
              assertThat(customerSetCustomTypeAction.getFields().values()).isEqualTo(emptyMap());
            });
  }

  @Test
  void buildRemoveCustomTypeAction_WithCustomerResource_ShouldBuildCustomerUpdateAction() {
    final CustomerUpdateAction updateAction =
        CustomerCustomActionBuilder.of().buildRemoveCustomTypeAction(null, null);

    assertThat(updateAction).isInstanceOf(CustomerSetCustomTypeAction.class);
    assertThat((CustomerSetCustomTypeAction) updateAction)
        .satisfies(
            customerSetCustomTypeAction -> {
              assertThat(customerSetCustomTypeAction.getType()).isEqualTo(null);
              assertThat(customerSetCustomTypeAction.getFields()).isEqualTo(null);
            });
  }

  @Test
  void buildSetCustomFieldAction_WithCustomerResource_ShouldBuildCustomerUpdateAction() {
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
    final String customFieldName = "name";

    final CustomerUpdateAction updateAction =
        CustomerCustomActionBuilder.of()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(CustomerSetCustomFieldAction.class);
    assertThat((CustomerSetCustomFieldAction) updateAction)
        .satisfies(
            customerSetCustomFieldAction -> {
              assertThat(customerSetCustomFieldAction.getName()).isEqualTo(customFieldName);
              assertThat(customerSetCustomFieldAction.getValue()).isEqualTo(customFieldValue);
            });
  }
}
