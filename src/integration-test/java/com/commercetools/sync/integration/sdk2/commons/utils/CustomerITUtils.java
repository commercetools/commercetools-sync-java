package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.common.AddressBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupDraft;
import com.commercetools.api.models.customer_group.CustomerGroupDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.store.Store;
import com.commercetools.api.models.store.StoreDraft;
import com.commercetools.api.models.store.StoreDraftBuilder;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.commands.CustomerDeleteCommand;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class CustomerITUtils {

  public static ImmutablePair<Customer, CustomerDraft> ensureSampleCustomerJohnDoe(
      @Nonnull final ProjectApiRoot ctpClient) {

    final Store storeBerlin = ensureStore(ctpClient, "store-berlin");
    final Store storeHamburg = ensureStore(ctpClient, "store-hamburg");
    final Store storeMunich = ensureStore(ctpClient, "store-munich");

    final Type customTypeGoldMember =
        ensureCustomerCustomType("customer-type-gold", Locale.ENGLISH, "gold customers", ctpClient);

    final CustomerGroup customerGroupGoldMembers =
        ensureCustomerGroup(ctpClient, "gold members", "gold");

    final CustomerDraft customerDraftJohnDoe =
        CustomerDraftBuilder.of()
            .email("john@example.com")
            .password("12345")
            .customerNumber("gold-1")
            .key("customer-key-john-doe")
            .stores(
                asList(
                    StoreResourceIdentifierBuilder.of().key(storeBerlin.getKey()).build(),
                    StoreResourceIdentifierBuilder.of().key(storeHamburg.getKey()).build(),
                    StoreResourceIdentifierBuilder.of().key(storeMunich.getKey()).build()))
            .firstName("John")
            .lastName("Doe")
            .middleName("Jr")
            .title("Mr")
            .salutation("Dear")
            .dateOfBirth(LocalDate.now().minusYears(28))
            .companyName("Acme Corporation")
            .vatId("DE999999999")
            .isEmailVerified(true)
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of()
                    .key(customerGroupGoldMembers.getKey())
                    .build())
            .addresses(
                asList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("berlin")
                        .key("address1")
                        .build(),
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("hamburg")
                        .key("address2")
                        .build(),
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("munich")
                        .key("address3")
                        .build()))
            .defaultBillingAddress(0)
            .billingAddresses(asList(0, 1))
            .defaultShippingAddress(2)
            .shippingAddresses(singletonList(2))
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        TypeResourceIdentifierBuilder.of()
                            .key(customTypeGoldMember.getKey())
                            .build())
                    .build())
            .locale(Locale.ENGLISH.toLanguageTag())
            .build();

    final Customer customer = ensureCustomer(ctpClient, customerDraftJohnDoe);
    return ImmutablePair.of(customer, customerDraftJohnDoe);
  }

  @Nonnull
  public static Customer ensureSampleCustomerJaneDoe(@Nonnull final ProjectApiRoot ctpClient) {
    final CustomerDraft customerDraftJaneDoe =
        CustomerDraftBuilder.of()
            .email("jane@example.com")
            .password("12345")
            .customerNumber("random-1")
            .key("customer-key-jane-doe")
            .firstName("Jane")
            .lastName("Doe")
            .middleName("Jr")
            .title("Miss")
            .salutation("Dear")
            .dateOfBirth(LocalDate.now().minusYears(25))
            .companyName("Acme Corporation")
            .vatId("FR000000000")
            .isEmailVerified(false)
            .addresses(
                asList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("cologne")
                        .key("address1")
                        .build(),
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("berlin")
                        .key("address2")
                        .build()))
            .defaultBillingAddress(0)
            .billingAddresses(singletonList(0))
            .defaultShippingAddress(1)
            .shippingAddresses(singletonList(1))
            .locale(Locale.ENGLISH.toLanguageTag())
            .build();

    return ensureCustomer(ctpClient, customerDraftJaneDoe);
  }

  /**
   * Creates a {@link Customer} in the CTP project defined by the {@code ctpClient} in a blocking
   * fashion, if the customer with the given key exist on the project, it deletes and re-creates the
   * customer.
   *
   * @param ctpClient defines the CTP project to create the CustomerGroup in.
   * @param customerDraft the draft of the customer to create.
   * @return the created customer.
   */
  public static Customer ensureCustomer(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final CustomerDraft customerDraft) {

    try {
      deleteCustomer(ctpClient, customerDraft.getKey());
    } catch (NotFoundException ignored) {
    }

    return createCustomer(ctpClient, customerDraft);
  }

  private static Customer createCustomer(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final CustomerDraft customerDraft) {
    return ctpClient.customers().post(customerDraft).executeBlocking().getBody().getCustomer();
  }

  /**
   * This method blocks to create a customer custom type on the CTP project defined by the supplied
   * {@code ctpClient}, with the supplied data.
   *
   * @param typeKey the type key
   * @param locale the locale to be used for specifying the type name and field definitions names.
   * @param name the name of the custom type.
   * @param ctpClient defines the CTP project to create the type on.
   */
  public static Type ensureCustomerCustomType(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ProjectApiRoot ctpClient) {

    return createTypeIfNotAlreadyExisting(
        typeKey, locale, name, singletonList(ResourceTypeId.CUSTOMER), ctpClient);
  }

  /**
   * Creates a {@link Store} in the CTP project defined by the {@code ctpClient} in a blocking
   * fashion if the store does not exist on the project.
   *
   * @param ctpClient defines the CTP project to create the Store in.
   * @param key the key of the Store to create.
   * @return the created store.
   */
  public static Store ensureStore(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {
    try {
      return ctpClient.stores().withKey(key).get().executeBlocking().getBody();
    } catch (NotFoundException nfe) {
      final StoreDraft storeDraft = StoreDraftBuilder.of().key(key).build();
      return ctpClient.stores().post(storeDraft).executeBlocking().getBody();
    }
  }

  /**
   * Creates a {@link CustomerGroup} in the CTP project defined by the {@code ctpClient} in a
   * blocking fashion if the customer group does not exist on the project.
   *
   * @param ctpClient defines the CTP project to create the CustomerGroup in.
   * @param name the name of the CustomerGroup to create.
   * @param key the key of the CustomerGroup to create.
   * @return the created CustomerGroup.
   */
  public static CustomerGroup ensureCustomerGroup(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String name,
      @Nonnull final String key) {

    try {
      return ctpClient.customerGroups().withKey(key).get().executeBlocking().getBody();
    } catch (NotFoundException nfe) {
      final CustomerGroupDraft customerGroupDraft =
          CustomerGroupDraftBuilder.of().groupName(name).key(key).build();

      return ctpClient.customerGroups().post(customerGroupDraft).executeBlocking().getBody();
    }
  }

  /**
   * Remove customer with given key from project
   *
   * @param ctpClient defines the CTP project to delete.
   * @param key the key of the customer to delete.
   */
  public static void deleteCustomer(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {

    try {
      // in case it does not exist, it will throw 404
      final Long version =
          ctpClient.customers().withKey(key).get().executeBlocking().getBody().getVersion();
      ctpClient.customers().withKey(key).delete().withVersion(version).executeBlocking();
    } catch (NotFoundException ignored) {
    }
  }

  /**
   * Deletes all customers from CTP project, represented by provided {@code ctpClient}.
   *
   * @param ctpClient represents the CTP project the customers will be deleted from.
   */
  public static void deleteCustomers(@Nonnull final ProjectApiRoot ctpClient) {
    QueryUtils.queryAll(
                    ctpClient.customers().get(),
                    customers -> {
                      CompletableFuture.allOf(
                                      customers.stream()
                                              .map(customer -> deleteCustomer(ctpClient, customer))
                                              .map(CompletionStage::toCompletableFuture)
                                              .toArray(CompletableFuture[]::new))
                              .join();
                    })
            .toCompletableFuture()
            .join();
  }

  private static CompletionStage<Customer> deleteCustomer(
          ProjectApiRoot ctpClient, Customer customer) {
    return ctpClient
            .customers()
            .delete(customer)
            .execute()
            .thenApply(ApiHttpResponse::getBody);
  }

  private CustomerITUtils() {}
}
