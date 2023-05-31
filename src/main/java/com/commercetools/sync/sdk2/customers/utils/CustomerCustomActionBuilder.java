package com.commercetools.sync.sdk2.customers.utils;

import com.commercetools.api.models.customer.CustomerSetCustomFieldActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomTypeActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomerCustomActionBuilder
    implements GenericCustomActionBuilder<CustomerUpdateAction> {

  private static final CustomerCustomActionBuilder builder = new CustomerCustomActionBuilder();

  private CustomerCustomActionBuilder() {
    super();
  }

  @Nonnull
  public static CustomerCustomActionBuilder of() {
    return builder;
  }

  @Nonnull
  @Override
  public CustomerUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String objectId) {

    return CustomerSetCustomTypeActionBuilder.of().build();
  }

  @Nonnull
  @Override
  public CustomerUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {

    final CustomerSetCustomTypeActionBuilder actionBuilder =
        CustomerSetCustomTypeActionBuilder.of()
            .type(TypeResourceIdentifierBuilder.of().id(customTypeId).build());

    if (customFieldsJsonMap != null && !customFieldsJsonMap.isEmpty()) {
      final FieldContainerBuilder customFields = FieldContainerBuilder.of();
      customFieldsJsonMap.forEach(customFields::addValue);
      actionBuilder.fields(customFields.build());
    }

    return actionBuilder.build();
  }

  @Nonnull
  @Override
  public CustomerUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {

    return CustomerSetCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .build();
  }
}
