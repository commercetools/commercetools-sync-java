package com.commercetools.sync.integration.externalsource.types;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.*;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.integration.commons.utils.ITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import com.commercetools.sync.sdk2.types.TypeSync;
import com.commercetools.sync.sdk2.types.TypeSyncOptions;
import com.commercetools.sync.sdk2.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.sdk2.types.helpers.TypeSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypeSyncIT {

  private AtomicInteger concurrentModificationCounter;
  private static final TypeDraft TYPE_DRAFT_1 =
      TypeDraftBuilder.of()
          .key(TYPE_KEY_1)
          .name(TYPE_NAME_1)
          .description(TYPE_DESCRIPTION_1)
          .fieldDefinitions(FIELD_DEFINITION_1, FIELD_DEFINITION_2)
          .resourceTypeIds(ResourceTypeId.CATEGORY)
          .build();

  /**
   * Deletes types from the target CTP projects. Populates the target CTP project with test data.
   */
  @BeforeEach
  void setup() {
    ITUtils.deleteTypes(TestClientUtils.CTP_TARGET_CLIENT);
    try {
      // The removal of a FieldDefinition deletes asynchronously all Custom Fields using the
      // FieldDefinition as well.
      // Here with one second break we are slowing down the ITs a little bit so CTP could remove the
      // custom fields.
      Thread.sleep(1000);
    } catch (InterruptedException expected) {
    }
    ITUtils.ensureTypeByTypeDraft(TYPE_DRAFT_1, TestClientUtils.CTP_TARGET_CLIENT);
  }

  /**
   * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT}
   * projects that were set up in this test class.
   */
  @AfterAll
  static void tearDown() {
    ITUtils.deleteTypes(TestClientUtils.CTP_TARGET_CLIENT);
  }

  @Test
  void sync_WithUpdatedType_ShouldUpdateType() {
    // preparation
    final Optional<Type> oldTypeBefore = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_1);
    assertThat(oldTypeBefore).isNotEmpty();

    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of(TYPE_DRAFT_1)
            .name(TYPE_NAME_2)
            .description(TYPE_DESCRIPTION_2)
            .fieldDefinitions(FIELD_DEFINITION_1)
            .build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

    final Optional<Type> oldTypeAfter = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_1);

    assertThat(oldTypeAfter).isNotEmpty();
    assertThat(oldTypeAfter)
        .hasValueSatisfying(
            type -> {
              assertThat(type.getName()).isEqualTo(TYPE_NAME_2);
              assertThat(type.getDescription()).isEqualTo(TYPE_DESCRIPTION_2);
              assertFieldDefinitionsAreEqual(
                  type.getFieldDefinitions(), singletonList(FIELD_DEFINITION_1));
            });
  }

  @Test
  void sync_WithNewType_ShouldCreateType() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key(TYPE_KEY_2)
            .name(TYPE_NAME_2)
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .description(TYPE_DESCRIPTION_2)
            .fieldDefinitions(FIELD_DEFINITION_1)
            .build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(typeSyncStatistics).hasValues(1, 1, 0, 0);

    final Optional<Type> oldTypeAfter = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_2);

    assertThat(oldTypeAfter).isNotEmpty();
    assertThat(oldTypeAfter)
        .hasValueSatisfying(
            type -> {
              assertThat(type.getName()).isEqualTo(TYPE_NAME_2);
              assertThat(type.getDescription()).isEqualTo(TYPE_DESCRIPTION_2);
              assertFieldDefinitionsAreEqual(
                  type.getFieldDefinitions(), singletonList(FIELD_DEFINITION_1));
            });
  }

  @Test
  void sync_WithUpdatedType_WithNewFieldDefinitions_ShouldUpdateTypeAddingFieldDefinition() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of(TYPE_DRAFT_1)
            .fieldDefinitions(FIELD_DEFINITION_1, FIELD_DEFINITION_2, FIELD_DEFINITION_3)
            .build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

    final Optional<Type> oldTypeAfter = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_1);

    assertThat(oldTypeAfter)
        .hasValueSatisfying(
            type ->
                assertFieldDefinitionsAreEqual(
                    type.getFieldDefinitions(),
                    asList(FIELD_DEFINITION_1, FIELD_DEFINITION_2, FIELD_DEFINITION_3)));
  }

  @Test
  void sync_WithUpdatedType_WithoutOldFieldDefinition_ShouldUpdateTypeRemovingFieldDefinition() {
    final Optional<Type> oldTypeBefore = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_1);
    assertThat(oldTypeBefore).isNotEmpty();

    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of(TYPE_DRAFT_1).fieldDefinitions(FIELD_DEFINITION_1).build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

    final Optional<Type> oldTypeAfter = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_1);

    assertThat(oldTypeAfter)
        .hasValueSatisfying(
            type ->
                assertFieldDefinitionsAreEqual(
                    type.getFieldDefinitions(), singletonList(FIELD_DEFINITION_1)));
  }

  @Test
  void
      sync_WithUpdatedType_ChangingFieldDefinitionOrder_ShouldUpdateTypeChangingFieldDefinitionOrder() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of(TYPE_DRAFT_1)
            .fieldDefinitions(FIELD_DEFINITION_2, FIELD_DEFINITION_3, FIELD_DEFINITION_1)
            .build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

    final Optional<Type> oldTypeAfter = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_1);

    assertThat(oldTypeAfter)
        .hasValueSatisfying(
            type ->
                assertFieldDefinitionsAreEqual(
                    type.getFieldDefinitions(),
                    asList(FIELD_DEFINITION_2, FIELD_DEFINITION_3, FIELD_DEFINITION_1)));
  }

  @Test
  void sync_WithUpdatedType_WithUpdatedFieldDefinition_ShouldUpdateTypeUpdatingFieldDefinition() {
    // preparation
    final FieldDefinition fieldDefinitionUpdated =
        FieldDefinitionBuilder.of()
            .type(FieldTypeBuilder::stringBuilder)
            .name(FIELD_DEFINITION_NAME_1)
            .label(LocalizedString.ofEnglish("label_1_updated"))
            .required(true)
            .inputHint(TypeTextInputHint.MULTI_LINE)
            .build();

    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of(TYPE_DRAFT_1).fieldDefinitions(fieldDefinitionUpdated).build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

    final Optional<Type> oldTypeAfter = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, TYPE_KEY_1);

    assertThat(oldTypeAfter)
        .hasValueSatisfying(
            type ->
                assertFieldDefinitionsAreEqual(
                    type.getFieldDefinitions(), singletonList(fieldDefinitionUpdated)));
  }

  @Test
  void sync_WithEmptyKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key("")
            .name(TYPE_NAME_1)
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .description(TYPE_DESCRIPTION_1)
            .fieldDefinitions(FIELD_DEFINITION_1, FIELD_DEFINITION_2)
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(
            format(
                "TypeDraft with name: %s doesn't have a key. "
                    + "Please make sure all type drafts have keys.",
                newTypeDraft.getName()));

    assertThat(exceptions).hasSize(1).singleElement().isNull();
    assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final TypeDraft newTypeDraft = null;
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages).hasSize(1).singleElement(as(STRING)).isEqualTo("TypeDraft is null.");

    assertThat(exceptions).hasSize(1).singleElement().isNull();

    assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
    // preparation
    // Default batch size is 50 (check TypeSyncOptionsBuilder) so we have 2 batches of 50
    final List<TypeDraft> typeDrafts =
        IntStream.range(0, 100)
            .mapToObj(
                i ->
                    TypeDraftBuilder.of()
                        .key("key__" + i)
                        .name(LocalizedString.ofEnglish("name__" + i))
                        .resourceTypeIds(ResourceTypeId.CATEGORY)
                        .description(LocalizedString.ofEnglish("description__" + i))
                        .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                        .build())
            .collect(Collectors.toList());

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(typeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(typeSyncStatistics).hasValues(100, 100, 0, 0);
  }

  private static void assertFieldDefinitionsAreEqual(
      @Nonnull final List<FieldDefinition> oldFields,
      @Nonnull final List<FieldDefinition> newFields) {

    IntStream.range(0, newFields.size())
        .forEach(
            index -> {
              final FieldDefinition oldFieldDefinition = oldFields.get(index);
              final FieldDefinition newFieldDefinition = newFields.get(index);

              assertThat(oldFieldDefinition.getName()).isEqualTo(newFieldDefinition.getName());
              assertThat(oldFieldDefinition.getLabel()).isEqualTo(newFieldDefinition.getLabel());
              assertThat(oldFieldDefinition.getType()).isEqualTo(newFieldDefinition.getType());
              if (newFieldDefinition.getInputHint() != null) {
                assertThat(oldFieldDefinition.getInputHint())
                    .isEqualTo(newFieldDefinition.getInputHint());
              }
              if (oldFieldDefinition.getType().getClass() == CustomFieldEnumType.class) {
                assertPlainEnumsValuesAreEqual(
                    ((CustomFieldEnumType) oldFieldDefinition.getType()).getValues(),
                    ((CustomFieldEnumType) newFieldDefinition.getType()).getValues());
              } else if (oldFieldDefinition.getType().getClass()
                  == CustomFieldLocalizedEnumType.class) {
                assertLocalizedEnumsValuesAreEqual(
                    ((CustomFieldLocalizedEnumType) oldFieldDefinition.getType()).getValues(),
                    ((CustomFieldLocalizedEnumType) newFieldDefinition.getType()).getValues());
              }
            });
  }

  private static void assertPlainEnumsValuesAreEqual(
      @Nonnull final List<CustomFieldEnumValue> enumValues,
      @Nonnull final List<CustomFieldEnumValue> enumValuesDrafts) {

    IntStream.range(0, enumValuesDrafts.size())
        .forEach(
            index -> {
              final CustomFieldEnumValue enumValue = enumValues.get(index);
              final CustomFieldEnumValue enumValueDraft = enumValuesDrafts.get(index);

              assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
              assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
            });
  }

  private static void assertLocalizedEnumsValuesAreEqual(
      @Nonnull final List<CustomFieldLocalizedEnumValue> enumValues,
      @Nonnull final List<CustomFieldLocalizedEnumValue> enumValuesDrafts) {

    IntStream.range(0, enumValuesDrafts.size())
        .forEach(
            index -> {
              final CustomFieldLocalizedEnumValue enumValue = enumValues.get(index);
              final CustomFieldLocalizedEnumValue enumValueDraft = enumValuesDrafts.get(index);

              assertThat(enumValue.getKey()).isEqualTo(enumValueDraft.getKey());
              assertThat(enumValue.getLabel()).isEqualTo(enumValueDraft.getLabel());
            });
  }

  @Test
  void sync_WithConcurrentModificationException_ShouldRetryToUpdateNewTypeWithSuccess() {
    // Preparation
    final ProjectApiRoot spyClient = buildClientWithConcurrentModificationUpdate();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("typeDraft")
            .name(TYPE_NAME_2)
            .resourceTypeIds(ResourceTypeId.CHANNEL)
            .build();

    ITUtils.ensureTypeByTypeDraft(typeDraft, TestClientUtils.CTP_TARGET_CLIENT);

    final TypeDraft updatedDraft = TypeDraftBuilder.of(typeDraft).name(TYPE_NAME_1).build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(spyClient).build();
    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics statistics =
        typeSync.sync(Collections.singletonList(updatedDraft)).toCompletableFuture().join();

    // assertion
    assertThat(statistics).hasValues(1, 0, 1, 0);

    // Assert CTP state.
    final ApiHttpResponse<Type> queryResult =
        TestClientUtils.CTP_TARGET_CLIENT
            .types()
            .withKey(typeDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join();

    assertThat(queryResult.getBody()).isInstanceOf(Type.class);
    assertThat(queryResult.getBody().getName()).isEqualTo(TYPE_NAME_1);
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdate() {
    // Helps to count invocation of a request and used to decide execution or mocking response
    concurrentModificationCounter = new AtomicInteger(0);

    return withTestClient(
        (uri, method) -> {
          if (uri.contains("types") && ApiHttpMethod.POST.equals(method)) {
            if (concurrentModificationCounter.get() == 0) {
              concurrentModificationCounter.incrementAndGet();
              return CompletableFutureUtils.exceptionallyCompletedFuture(
                  ITUtils.createConcurrentModificationException());
            }
          }
          return null;
        });
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("typeKey")
            .name(ofEnglish("typeName"))
            .resourceTypeIds(ResourceTypeId.CHANNEL)
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.types().post(typeDraft).execute().join();

    final LocalizedString newTypeName = LocalizedString.ofEnglish("typeName_updated");
    final TypeDraft updatedDraft = TypeDraftBuilder.of(typeDraft).name(newTypeName).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics statistics =
        typeSync.sync(Collections.singletonList(updatedDraft)).toCompletableFuture().join();

    // assertion
    assertThat(statistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages).hasSize(1);
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update type with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                typeDraft.getKey()));
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    return withTestClient(
        (uri, method) -> {
          if (uri.contains("types/key=") && ApiHttpMethod.GET.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(ITUtils.createBadGatewayException());
          } else if (uri.contains("types/") && ApiHttpMethod.POST.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                ITUtils.createConcurrentModificationException());
          }
          return null;
        });
  }

  @Test
  void sync__WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("typeKey")
            .name(ofEnglish("typeName"))
            .resourceTypeIds(ResourceTypeId.CHANNEL)
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.types().post(typeDraft).execute().join();

    final LocalizedString newTypeName = LocalizedString.ofEnglish("typeName_updated");
    final TypeDraft updatedDraft = TypeDraftBuilder.of(typeDraft).name(newTypeName).build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);
    // test
    final TypeSyncStatistics statistics =
        typeSync.sync(Collections.singletonList(updatedDraft)).toCompletableFuture().join();

    // Assertion
    assertThat(statistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages).hasSize(1);
    assertThat(errors).hasSize(1);
    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update type with key: '%s'. Reason: Not found when attempting to fetch while "
                    + "retrying after concurrency modification.",
                typeDraft.getKey()));
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {

    return withTestClient(
        (uri, method) -> {
          if (uri.contains("types/key=") && ApiHttpMethod.GET.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(ITUtils.createNotFoundException());
          }
          if (uri.contains("types/") && ApiHttpMethod.POST.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                ITUtils.createConcurrentModificationException());
          }
          return null;
        });
  }

  @Test
  void sync_WithSetOfEnumsAndSetOfLenumsChanges_ShouldUpdateTypeAddingFieldDefinition() {
    // preparation
    final CustomFieldEnumValue enum_value_A =
        CustomFieldEnumValueBuilder.of().key("a").label("a").build();
    final CustomFieldEnumValue enum_value_B =
        CustomFieldEnumValueBuilder.of().key("b").label("b").build();
    final CustomFieldLocalizedEnumValue lEnum_value_A =
        CustomFieldLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("a")).build();
    final CustomFieldLocalizedEnumValue lEnum_value_B =
        CustomFieldLocalizedEnumValueBuilder.of().key("b").label(ofEnglish("b")).build();
    final FieldDefinition withSetOfEnumsOld =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldEnumTypeBuilder.of().values(enum_value_B, enum_value_A).build())
                    .build())
            .name("foo")
            .label(ofEnglish("foo"))
            .required(false)
            .build();

    final FieldDefinition withSetOfLEnumsOld =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of()
                            .values(lEnum_value_B, lEnum_value_A)
                            .build())
                    .build())
            .name("bar")
            .label(ofEnglish("bar"))
            .required(false)
            .build();

    final TypeDraft oldTypeDraft =
        TypeDraftBuilder.of()
            .key("withSetOfEnums")
            .name(ofEnglish("withSetOfEnums"))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .fieldDefinitions(withSetOfEnumsOld, withSetOfLEnumsOld)
            .build();

    TestClientUtils.CTP_TARGET_CLIENT.types().post(oldTypeDraft).execute().join();

    final CustomFieldEnumValue enum_value_C =
        CustomFieldEnumValueBuilder.of().key("c").label("c").build();

    final FieldDefinition withSetOfEnumsNew =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldEnumTypeBuilder.of()
                            .values(enum_value_A, enum_value_B, enum_value_C)
                            .build())
                    .build())
            .name("foo")
            .label(ofEnglish("foo"))
            .required(false)
            .build();

    final CustomFieldLocalizedEnumValue lEnum_value_C =
        CustomFieldLocalizedEnumValueBuilder.of().key("c").label(ofEnglish("c")).build();
    final FieldDefinition withSetOfLEnumsNew =
        FieldDefinitionBuilder.of()
            .type(
                CustomFieldSetTypeBuilder.of()
                    .elementType(
                        CustomFieldLocalizedEnumTypeBuilder.of()
                            .values(lEnum_value_A, lEnum_value_B, lEnum_value_C)
                            .build())
                    .build())
            .name("bar")
            .label(ofEnglish("bar"))
            .required(true)
            .build();

    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key("withSetOfEnums")
            .name(ofEnglish("withSetOfEnums"))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .fieldDefinitions(withSetOfEnumsNew, withSetOfLEnumsNew)
            .build();

    final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(typeSyncStatistics).hasValues(1, 0, 1, 0);

    final Optional<Type> oldTypeAfter = getTypeByKey(TestClientUtils.CTP_TARGET_CLIENT, "withSetOfEnums");

    assertThat(oldTypeAfter)
        .hasValueSatisfying(
            type ->
                assertFieldDefinitionsAreEqual(
                    type.getFieldDefinitions(), asList(withSetOfEnumsNew, withSetOfLEnumsNew)));
  }

  private ProjectApiRoot withTestClient(
      BiFunction<String, ApiHttpMethod, CompletableFuture<ApiHttpResponse<byte[]>>> fn) {
    return ApiRootBuilder.of(
            request -> {
              final String uri = request.getUri() != null ? request.getUri().toString() : "";
              final ApiHttpMethod method = request.getMethod();
              final CompletableFuture<ApiHttpResponse<byte[]>> exceptionResponse =
                  fn.apply(uri, method);
              if (exceptionResponse != null) {
                return exceptionResponse;
              }
              return TestClientUtils.CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
            })
        .withApiBaseUrl(TestClientUtils.CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
        .build(TestClientUtils.CTP_TARGET_CLIENT.getProjectKey());
  }
}
