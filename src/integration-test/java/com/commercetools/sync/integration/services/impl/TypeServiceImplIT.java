package com.commercetools.sync.integration.services.impl;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyTypesGet;
import com.commercetools.api.client.ByProjectKeyTypesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.error.DuplicateFieldErrorBuilder;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.integration.commons.utils.CategoryITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_TARGET_CLIENT);
    CategoryITUtils.ensureCategoriesCustomType(
        OLD_TYPE_KEY, OLD_TYPE_LOCALE, OLD_TYPE_NAME, TestClientUtils.CTP_TARGET_CLIENT);

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();
    typeService = new TypeServiceImpl(typeSyncOptions);
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
    final ProjectApiRoot spyClient = Mockito.spy(TestClientUtils.CTP_TARGET_CLIENT);
    when(spyClient.types()).thenReturn(mock(ByProjectKeyTypesRequestBuilder.class));
    final ByProjectKeyTypesGet getMock = mock(ByProjectKeyTypesGet.class);
    when(spyClient.types().get()).thenReturn(getMock);
    when(getMock.withWhere(any(String.class))).thenReturn(getMock);
    when(getMock.withPredicateVar(any(String.class), any())).thenReturn(getMock);
    when(getMock.withLimit(any(Integer.class))).thenReturn(getMock);
    when(getMock.withWithTotal(any(Boolean.class))).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException(500, "", null, "", null)))
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
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void fetchMatchingTypesByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedTypeIds() {
    final Set<Type> fetchedTypes =
        typeService.fetchMatchingTypesByKeys(singleton(OLD_TYPE_KEY)).toCompletableFuture().join();
    assertThat(fetchedTypes).hasSize(1);

    final Type queriedType =
        TestClientUtils.CTP_TARGET_CLIENT
            .types()
            .withKey(OLD_TYPE_KEY)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(queriedType).isNotNull();

    // Change type old_type_key on ctp
    final String newKey = "new_type_key";
    typeService
        .updateType(
            queriedType,
            Collections.singletonList(TypeChangeKeyActionBuilder.of().key(newKey).build()))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedTypeId =
        typeService.fetchCachedTypeId(OLD_TYPE_KEY).toCompletableFuture().join();

    assertThat(cachedTypeId).isNotEmpty();
    assertThat(cachedTypeId).contains(queriedType.getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createType_WithValidType_ShouldCreateTypeAndCacheId() {
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key(TYPE_KEY_1)
            .name(TYPE_NAME_1)
            .description(TYPE_DESCRIPTION_1)
            .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .build();

    final ProjectApiRoot spyClient = Mockito.spy(TestClientUtils.CTP_TARGET_CLIENT);
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

    final Type queriedType =
        TestClientUtils.CTP_TARGET_CLIENT
            .types()
            .withKey(TYPE_KEY_1)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(createdType)
        .hasValueSatisfying(
            created -> {
              assertThat(created.getKey()).isEqualTo(queriedType.getKey());

              assertThat(created.getDescription()).isEqualTo(queriedType.getDescription());
              assertThat(created.getName()).isEqualTo(queriedType.getName());
              assertThat(created.getFieldDefinitions())
                  .isEqualTo(queriedType.getFieldDefinitions());
            });

    // Assert that the created type is cached
    final Optional<String> typeId =
        spyTypeService.fetchCachedTypeId(TYPE_KEY_1).toCompletableFuture().join();
    assertThat(typeId).isPresent();
  }

  @Test
  void createType_WithInvalidType_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key("")
            .name(TYPE_NAME_1)
            .description(TYPE_DESCRIPTION_1)
            .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .build();

    final TypeSyncOptions options =
        TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
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
        TypeDraftBuilder.of()
            .key(OLD_TYPE_KEY)
            .name(TYPE_NAME_1)
            .description(TYPE_DESCRIPTION_1)
            .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .build();

    final TypeSyncOptions options =
        TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
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
        .singleElement(as(STRING))
        .contains("A duplicate value");

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(CompletionException.class);
              final BadRequestException errorResponseException =
                  (BadRequestException) exception.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  errorResponseException.getErrorResponse().getErrors().stream()
                      .map(
                          error -> {
                            assertThat(error.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return DuplicateFieldErrorBuilder.of((DuplicateFieldError) error)
                                .build();
                          })
                      .collect(toList());
              return fieldErrors.size() == 1;
            });
  }

  @Test
  void updateType_WithValidChanges_ShouldUpdateTypeCorrectly() {
    final Type queriedType =
        TestClientUtils.CTP_TARGET_CLIENT
            .types()
            .withKey(OLD_TYPE_KEY)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(queriedType).isNotNull();

    final TypeChangeNameAction changeNameUpdateAction =
        TypeChangeNameActionBuilder.of().name(ofEnglish("new_type_name")).build();

    final Type updatedType =
        typeService
            .updateType(queriedType.get(), singletonList(changeNameUpdateAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedType).isNotNull();

    final Type queriedTypeUpdated =
        TestClientUtils.CTP_TARGET_CLIENT
            .types()
            .withKey(OLD_TYPE_KEY)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(queriedTypeUpdated).isNotNull();
    assertThat(queriedTypeUpdated.getKey()).isEqualTo(updatedType.getKey());
    assertThat(queriedTypeUpdated.getDescription()).isEqualTo(updatedType.getDescription());
    assertThat(queriedTypeUpdated.getName()).isEqualTo(updatedType.getName());
    assertThat(queriedTypeUpdated.getFieldDefinitions())
        .isEqualTo(updatedType.getFieldDefinitions());
  }

  @Test
  void updateType_WithInvalidChanges_ShouldCompleteExceptionally() {
    final Type queriedType =
        TestClientUtils.CTP_TARGET_CLIENT
            .types()
            .withKey(OLD_TYPE_KEY)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(queriedType).isNotNull();

    final TypeChangeNameAction changeNameUpdateAction =
        TypeChangeNameActionBuilder.of().name(LocalizedString.ofEnglish("")).build();
    typeService
        .updateType(queriedType, singletonList(changeNameUpdateAction))
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
}
