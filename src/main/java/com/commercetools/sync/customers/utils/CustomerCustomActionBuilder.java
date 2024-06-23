package com.commercetools.sync.customers.utils;

import com.commercetools.api.models.customer.CustomerSetCustomFieldActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomTypeActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
  @SuppressFBWarnings
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

    return CustomerSetCustomTypeActionBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
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
