package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_KEY_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.TYPE_NAME_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.deleteTypes;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.errors.DuplicateFieldError;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.updateactions.ChangeKey;
import io.sphere.sdk.types.commands.updateactions.ChangeName;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypeServiceImplIT {
  private TypeService typeService;
  private static final String OLD_TYPE_KEY = "old_type_key";
  private static final String OLD_TYPE_NAME = "old_type_name";
  private static final Locale OLD_TYPE_LOCALE = Locale.ENGLISH;

  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /** Deletes types from the target CTP project, then it populates the project with test data. */
  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();

    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteTypes(CTP_TARGET_CLIENT);
    createCategoriesCustomType(OLD_TYPE_KEY, OLD_TYPE_LOCALE, OLD_TYPE_NAME, CTP_TARGET_CLIENT);

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();
    typeService = new TypeServiceImpl(typeSyncOptions);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteTypes(CTP_TARGET_CLIENT);
  }

  @Test
  void fetchCachedTypeId_WithNonExistingType_ShouldNotFetchAType() {
    final Optional<String> typeId =
        typeService.fetchCachedTypeId("non-existing-type-key").toCompletableFuture().join();
    assertThat(typeId).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchCachedTypeId_WithExistingType_ShouldFetchTypeAndCache() {
    final Optional<String> typeId =
        typeService.fetchCachedTypeId(OLD_TYPE_KEY).toCompletableFuture().join();
    assertThat(typeId).isNotEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingTypesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
    final Set<String> typeKeys = new HashSet<>();
    final Set<Type> matchingTypes =
        typeService.fetchMatchingTypesByKeys(typeKeys).toCompletableFuture().join();

    assertThat(matchingTypes).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingTypesByKeys_WithNonExistingKeys_ShouldReturnEmptySet() {
    final Set<String> typeKeys = new HashSet<>();
    typeKeys.add("type_key_1");
    typeKeys.add("type_key_2");

    final Set<Type> matchingTypes =
        typeService.fetchMatchingTypesByKeys(typeKeys).toCompletableFuture().join();

    assertThat(matchingTypes).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingTypesByKeys_WithAnyExistingKeys_ShouldReturnASetOfTypes() {
    final Set<String> typeKeys = new HashSet<>();
    typeKeys.add(OLD_TYPE_KEY);

    final Set<Type> matchingTypes =
        typeService.fetchMatchingTypesByKeys(typeKeys).toCompletableFuture().join();

    assertThat(matchingTypes).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingTypesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
        .thenCallRealMethod();

    final TypeSyncOptions spyOptions =
        TypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TypeService spyTypeService = new TypeServiceImpl(spyOptions);

    final Set<String> keys = new HashSet<>();
    keys.add(OLD_TYPE_KEY);

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyTypeService.fetchMatchingTypesByKeys(keys))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void fetchMatchingTypesByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedTypeIds() {
    final Set<Type> fetchedTypes =
        typeService.fetchMatchingTypesByKeys(singleton(OLD_TYPE_KEY)).toCompletableFuture().join();
    assertThat(fetchedTypes).hasSize(1);

    final Optional<Type> typeOptional =
        CTP_TARGET_CLIENT
            .execute(TypeQuery.of().withPredicates(queryModel -> queryModel.key().is(OLD_TYPE_KEY)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(typeOptional).isNotNull();

    // Change type old_type_key on ctp
    final String newKey = "new_type_key";
    typeService
        .updateType(typeOptional.get(), Collections.singletonList(ChangeKey.of(newKey)))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedTypeId =
        typeService.fetchCachedTypeId(OLD_TYPE_KEY).toCompletableFuture().join();

    assertThat(cachedTypeId).isNotEmpty();
    assertThat(cachedTypeId).contains(typeOptional.get().getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createType_WithValidType_ShouldCreateTypeAndCacheId() {
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of(
                TYPE_KEY_1, TYPE_NAME_1, ResourceTypeIdsSetBuilder.of().addCategories().build())
            .description(TYPE_DESCRIPTION_1)
            .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
            .build();

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final TypeSyncOptions spyOptions =
        TypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TypeService spyTypeService = new TypeServiceImpl(spyOptions);

    // test
    final Optional<Type> createdType =
        spyTypeService.createType(newTypeDraft).toCompletableFuture().join();

    final Optional<Type> queriedOptional =
        CTP_TARGET_CLIENT
            .execute(
                TypeQuery.of()
                    .withPredicates(typeQueryModel -> typeQueryModel.key().is(TYPE_KEY_1)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(queriedOptional)
        .hasValueSatisfying(
            queried ->
                assertThat(createdType)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(created.getKey()).isEqualTo(queried.getKey());
                          assertThat(created.getDescription()).isEqualTo(queried.getDescription());
                          assertThat(created.getName()).isEqualTo(queried.getName());
                          assertThat(created.getFieldDefinitions())
                              .isEqualTo(queried.getFieldDefinitions());
                        }));

    // Assert that the created type is cached
    final Optional<String> typeId =
        spyTypeService.fetchCachedTypeId(TYPE_KEY_1).toCompletableFuture().join();
    assertThat(typeId).isPresent();
    verify(spyClient, times(0)).execute(any(TypeQuery.class));
  }

  @Test
  void createType_WithInvalidType_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of("", TYPE_NAME_1, ResourceTypeIdsSetBuilder.of().addCategories().build())
            .description(TYPE_DESCRIPTION_1)
            .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
            .build();

    final TypeSyncOptions options =
        TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TypeService typeService = new TypeServiceImpl(options);

    // test
    final Optional<Type> result = typeService.createType(newTypeDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
  }

  @Test
  void createType_WithDuplicateKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of(
                OLD_TYPE_KEY, TYPE_NAME_1, ResourceTypeIdsSetBuilder.of().addCategories().build())
            .description(TYPE_DESCRIPTION_1)
            .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
            .build();

    final TypeSyncOptions options =
        TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TypeService typeService = new TypeServiceImpl(options);

    // test
    final Optional<Type> result = typeService.createType(newTypeDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(msg -> assertThat(msg).contains("A duplicate value"));

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponseException =
                  (ErrorResponseException) exception;

              final List<DuplicateFieldError> fieldErrors =
                  errorResponseException.getErrors().stream()
                      .map(
                          sphereError -> {
                            assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                            return sphereError.as(DuplicateFieldError.class);
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
            });
  }

  @Test
  void updateType_WithValidChanges_ShouldUpdateTypeCorrectly() {
    final Optional<Type> typeOptional =
        CTP_TARGET_CLIENT
            .execute(
                TypeQuery.of()
                    .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(typeOptional).isNotNull();

    final ChangeName changeNameUpdateAction = ChangeName.of(ofEnglish("new_type_name"));

    final Type updatedType =
        typeService
            .updateType(typeOptional.get(), singletonList(changeNameUpdateAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedType).isNotNull();

    final Optional<Type> updatedTypeOptional =
        CTP_TARGET_CLIENT
            .execute(
                TypeQuery.of()
                    .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(typeOptional).isNotEmpty();
    final Type fetchedType = updatedTypeOptional.get();
    assertThat(fetchedType.getKey()).isEqualTo(updatedType.getKey());
    assertThat(fetchedType.getDescription()).isEqualTo(updatedType.getDescription());
    assertThat(fetchedType.getName()).isEqualTo(updatedType.getName());
    assertThat(fetchedType.getFieldDefinitions()).isEqualTo(updatedType.getFieldDefinitions());
  }

  @Test
  void updateType_WithInvalidChanges_ShouldCompleteExceptionally() {
    final Optional<Type> typeOptional =
        CTP_TARGET_CLIENT
            .execute(
                TypeQuery.of()
                    .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(typeOptional).isNotNull();

    final ChangeName changeNameUpdateAction = ChangeName.of(null);
    typeService
        .updateType(typeOptional.get(), singletonList(changeNameUpdateAction))
        .exceptionally(
            exception -> {
              assertThat(exception).isNotNull();
              assertThat(exception.getMessage())
                  .contains("Request body does not contain valid JSON.");
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void fetchType_WithExistingTypeKey_ShouldFetchType() {
    final Optional<Type> typeOptional =
        CTP_TARGET_CLIENT
            .execute(
                TypeQuery.of()
                    .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(typeOptional).isNotNull();

    final Optional<Type> fetchedTypeOptional = executeBlocking(typeService.fetchType(OLD_TYPE_KEY));
    assertThat(fetchedTypeOptional).isEqualTo(typeOptional);
  }

  @Test
  void fetchType_WithBlankKey_ShouldNotFetchType() {
    final Optional<Type> fetchedTypeOptional =
        executeBlocking(typeService.fetchType(StringUtils.EMPTY));
    assertThat(fetchedTypeOptional).isEmpty();
  }

  @Test
  void fetchType_WithNullKey_ShouldNotFetchType() {
    final Optional<Type> fetchedTypeOptional = executeBlocking(typeService.fetchType(null));
    assertThat(fetchedTypeOptional).isEmpty();
  }
}
