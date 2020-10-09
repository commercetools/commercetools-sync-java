package com.commercetools.sync.commons.utils;

import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.utils.CustomerCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.updateactions.SetCustomField;
import io.sphere.sdk.customers.commands.updateactions.SetCustomType;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CustomerCustomUpdateActionUtilsTest {

    @Test
    void buildTypedSetCustomTypeUpdateAction_WithCustomerResource_ShouldBuildCustomerUpdateAction() {
        final String newCustomTypeId = UUID.randomUUID().toString();

        final UpdateAction<Customer> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(newCustomTypeId, new HashMap<>(),
                mock(Customer.class), CustomerCustomActionBuilder.of(), null, Customer::getId,
                customerResource -> customerResource.toReference().getTypeId(), customerResource -> null,
                CustomerSyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", emptyMap(), ofId(newCustomTypeId));
    }

    @Test
    void buildRemoveCustomTypeAction_WithCustomerResource_ShouldBuildCustomerUpdateAction() {
        final UpdateAction<Customer> updateAction =
            CustomerCustomActionBuilder.of().buildRemoveCustomTypeAction(null, null);

        assertThat(updateAction).isInstanceOf(SetCustomType.class);
        assertThat((SetCustomType) updateAction).hasValues("setCustomType", null, ofId(null));
    }

    @Test
    void buildSetCustomFieldAction_WithCustomerResource_ShouldBuildCustomerUpdateAction() {
        final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
        final String customFieldName = "name";

        final UpdateAction<Customer> updateAction = CustomerCustomActionBuilder.of()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

        assertThat(updateAction).isInstanceOf(SetCustomField.class);
        assertThat((SetCustomField) updateAction).hasValues("setCustomField", customFieldName, customFieldValue);
    }
}
