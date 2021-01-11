package com.commercetools.sync.commons;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.TypeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MockUtils {
  /**
   * Creates a mock instance of {@link CustomFieldsDraft} with the key 'StepCategoryTypeKey' and two
   * custom fields 'invisibleInShop' and'backgroundColor'.
   *
   * <p>The 'invisibleInShop' field is of type {@code boolean} and has value {@code false} and the
   * the 'backgroundColor' field is of type {@code localisedString} and has the values {"de": "rot",
   * "en": "red"}
   *
   * @return a mock instance of {@link CustomFieldsDraft} with some hardcoded custom fields and key.
   */
  public static CustomFieldsDraft getMockCustomFieldsDraft() {
    final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
    customFieldsJsons.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
    customFieldsJsons.put(
        "backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));
    return CustomFieldsDraft.ofTypeIdAndJson("StepCategoryTypeId", customFieldsJsons);
  }

  /**
   * Returns mock {@link CustomFields} instance. Executing {@link CustomFields#getType()} on
   * returned instance will return {@link Reference} of given {@code typeId} with mock {@link Type}
   * instance of {@code typeId} and {@code typeKey} (getters of key and id would return given
   * values). Executing {@link CustomFields#getFieldsJsonMap()} on returned instance will return
   * {@link Map} populated with given {@code fieldName} and {@code fieldValue}
   *
   * @param typeId custom type id
   * @param fieldName custom field name
   * @param fieldValue custom field value
   * @return mock instance of {@link CustomFields}
   */
  public static CustomFields getMockCustomFields(
      final String typeId, final String fieldName, final JsonNode fieldValue) {
    final CustomFields customFields = mock(CustomFields.class);
    final Type type = mock(Type.class);
    when(type.getId()).thenReturn(typeId);
    when(customFields.getFieldsJsonMap()).thenReturn(mockFields(fieldName, fieldValue));
    when(customFields.getType()).thenReturn(Type.referenceOfId(typeId).filled(type));
    return customFields;
  }

  private static Map<String, JsonNode> mockFields(final String name, final JsonNode value) {
    final HashMap<String, JsonNode> fields = new HashMap<>();
    fields.put(name, value);
    return fields;
  }

  public static CategoryService mockCategoryService(
      @Nonnull final Set<Category> existingCategories, @Nullable final Category createdCategory) {
    final CategoryService mockCategoryService = mock(CategoryService.class);
    when(mockCategoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(CompletableFuture.completedFuture(existingCategories));

    final Map<String, String> keyToIds =
        existingCategories.stream().collect(Collectors.toMap(Category::getKey, Category::getId));
    when(mockCategoryService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
    when(mockCategoryService.createCategory(any()))
        .thenReturn(CompletableFuture.completedFuture(ofNullable(createdCategory)));
    return mockCategoryService;
  }

  public static CategoryService mockCategoryService(
      @Nonnull final Set<Category> existingCategories,
      @Nullable final Category createdCategory,
      @Nonnull final Category updatedCategory) {
    final CategoryService mockCategoryService =
        mockCategoryService(existingCategories, createdCategory);
    when(mockCategoryService.updateCategory(any(), any()))
        .thenReturn(completedFuture(updatedCategory));
    return mockCategoryService;
  }

  /**
   * Creates a mock {@link TypeService} that returns a dummy type id of value "typeId" instance
   * whenever the following method is called on the service:
   *
   * <ul>
   *   <li>{@link TypeService#fetchCachedTypeId(String)}
   * </ul>
   *
   * @return the created mock of the {@link TypeService}.
   */
  public static TypeService getMockTypeService() {
    final TypeService typeService = mock(TypeService.class);
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(completedFuture(Optional.of("typeId")));
    when(typeService.cacheKeysToIds(anySet()))
        .thenReturn(completedFuture(Collections.singletonMap("typeKey", "typeId")));
    return typeService;
  }

  /**
   * Creates a mock {@link Type} with the supplied {@code id} and {@code key}.
   *
   * @param id the id of the type mock.
   * @param key the key of the type mock.
   * @return a mock product variant with the supplied prices and assets.
   */
  @Nonnull
  public static Type getTypeMock(@Nonnull final String id, @Nonnull final String key) {
    final Type mockCustomType = mock(Type.class);
    when(mockCustomType.getId()).thenReturn(id);
    when(mockCustomType.getKey()).thenReturn(key);
    return mockCustomType;
  }

  /**
   * Creates a mock {@link Asset} with the supplied {@link Type} reference for it's custom field.
   *
   * @param typeReference the type reference to attach to the custom field of the created asset.
   * @return a mock asset with the supplied type reference on it's custom field.
   */
  @Nonnull
  public static Asset getAssetMockWithCustomFields(@Nullable final Reference<Type> typeReference) {
    // Mock Custom with expanded type reference
    final CustomFields mockCustomFields = mock(CustomFields.class);
    when(mockCustomFields.getType()).thenReturn(typeReference);

    // Mock asset with custom fields
    final Asset asset = mock(Asset.class);
    when(asset.getCustom()).thenReturn(mockCustomFields);
    return asset;
  }

  /**
   * Creates a mock {@link CustomerService} that returns a dummy customer id of value "customerId"
   * instance whenever the following method is called on the service:
   *
   * <ul>
   *   <li>{@link CustomerService#fetchCachedCustomerId(String)}
   * </ul>
   *
   * @return the created mock of the {@link CustomerService}.
   */
  public static CustomerService getMockCustomerService() {
    final CustomerService customerService = mock(CustomerService.class);
    when(customerService.fetchCachedCustomerId(anyString()))
        .thenReturn(completedFuture(Optional.of("customerId")));
    when(customerService.cacheKeysToIds(anySet()))
        .thenReturn(completedFuture(Collections.singletonMap("customerKey", "customerId")));
    return customerService;
  }

  /**
   * Creates a mock {@link Customer} with the supplied {@code id} and {@code key}.
   *
   * @param id the id of the created mock {@link Customer}.
   * @param key the key of the created mock {@link CustomerGroup}.
   * @return a mock customerGroup with the supplied id and key.
   */
  public static Customer getMockCustomer(final String id, final String key) {
    final Customer customer = mock(Customer.class);
    when(customer.getId()).thenReturn(id);
    when(customer.getKey()).thenReturn(key);
    return customer;
  }
}
