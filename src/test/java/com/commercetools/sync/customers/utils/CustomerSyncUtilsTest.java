package com.commercetools.sync.customers.utils;

import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.updateactions.SetCustomField;
import io.sphere.sdk.customers.commands.updateactions.SetCustomType;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.commercetools.sync.customers.utils.CustomerSyncUtils.buildActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(customFields.getType()).thenReturn(Type.referenceOfId(CUSTOM_TYPE_ID));

        final Map<String, JsonNode> customFieldsJsonMapMock = new HashMap<>();
        customFieldsJsonMapMock.put(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode(CUSTOM_FIELD_VALUE));
        when(customFields.getFieldsJsonMap()).thenReturn(customFieldsJsonMapMock);

        when(oldCustomer.getCustom()).thenReturn(customFields);
    }

    @Test
    void buildActions_WithDifferentCustomType_ShouldBuildUpdateAction() {
        final CustomFieldsDraft customFieldsDraft =
            CustomFieldsDraftBuilder.ofTypeId("newId")
                                    .addObject("newField", "newValue")
                                    .build();

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .custom(customFieldsDraft)
                                .build();

        final List<UpdateAction<Customer>> actions =
            buildActions(oldCustomer, newCustomer, CustomerSyncOptionsBuilder.of(mock(SphereClient.class)).build());

        assertThat(actions).containsExactly(
            SetCustomType.ofTypeIdAndJson(customFieldsDraft.getType().getId(), customFieldsDraft.getFields()));
    }

    @Test
    void buildActions_WithSameCustomTypeWithNewCustomFields_ShouldBuildUpdateAction() {
        final CustomFieldsDraft sameCustomFieldDraftWithNewCustomField =
            CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
                                    .addObject(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE)
                                    .addObject("name_2", "value_2")
                                    .build();

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .custom(sameCustomFieldDraftWithNewCustomField)
                                .build();

        final List<UpdateAction<Customer>> actions =
            buildActions(oldCustomer, newCustomer, CustomerSyncOptionsBuilder.of(mock(SphereClient.class)).build());


        assertThat(actions).containsExactly(
            SetCustomField.ofJson("name_2", JsonNodeFactory.instance.textNode("value_2")));
    }

    @Test
    void buildActions_WithSameCustomTypeWithDifferentCustomFieldValues_ShouldBuildUpdateAction() {

        final CustomFieldsDraft sameCustomFieldDraftWithNewValue =
            CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
                                    .addObject(CUSTOM_FIELD_NAME,
                                        "newValue")
                                    .build();

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .custom(sameCustomFieldDraftWithNewValue)
                                .build();

        final List<UpdateAction<Customer>> actions =
            buildActions(oldCustomer, newCustomer, CustomerSyncOptionsBuilder.of(mock(SphereClient.class)).build());

        assertThat(actions).containsExactly(
            SetCustomField.ofJson(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("newValue")));
    }

    @Test
    void buildActions_WithJustNewCartDiscountHasNullCustomType_ShouldBuildUpdateAction() {
        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .custom(null)
                                .build();

        final List<UpdateAction<Customer>> actions =
            buildActions(oldCustomer, newCustomer, CustomerSyncOptionsBuilder.of(mock(SphereClient.class)).build());

        assertThat(actions).containsExactly(SetCustomType.ofRemoveType());
    }
}
