package com.commercetools.sync.integration.sdk2.ctpprojectsource.types;

import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TypeITUtils.*;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.sync.sdk2.types.TypeSync;
import com.commercetools.sync.sdk2.types.TypeSyncOptions;
import com.commercetools.sync.sdk2.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.sdk2.types.helpers.TypeSyncStatistics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;

class TypeSyncIT {

  private static final TypeDraft TYPE_DRAFT_1 =
      TypeDraftBuilder.of()
          .key(TYPE_KEY_1)
          .name(TYPE_NAME_1)
          .description(TYPE_DESCRIPTION_1)
          .fieldDefinitions(Arrays.asList(FIELD_DEFINITION_1, FIELD_DEFINITION_2))
          .resourceTypeIds(ResourceTypeId.CATEGORY)
          .build();
  private static final TypeDraft TYPE_DRAFT_2 =
      TypeDraftBuilder.of()
          .key(TYPE_KEY_2)
          .name(TYPE_NAME_2)
          .description(TYPE_DESCRIPTION_2)
          .fieldDefinitions(singletonList(FIELD_DEFINITION_2))
          .resourceTypeIds(ResourceTypeId.CATEGORY)
          .build();

  @BeforeEach
  void before() {
    deleteTypes(CTP_TARGET_CLIENT);
    deleteTypes(CTP_SOURCE_CLIENT);
    try {
      // The removal of a FieldDefinition deletes asynchronously all Custom Fields using the
      // FieldDefinition as well.
      // Here with one second break we are slowing down the ITs a little bit so CTP could remove the
      // custom fields.
      Thread.sleep(1000);
    } catch (InterruptedException expected) {
    }
    ensureTypeByTypeDraft(TYPE_DRAFT_2, CTP_TARGET_CLIENT);
    ensureTypeByTypeDraft(TYPE_DRAFT_1, CTP_SOURCE_CLIENT);
    ensureTypeByTypeDraft(TYPE_DRAFT_2, CTP_SOURCE_CLIENT);
  }

  @AfterEach
  void after() {
    deleteTypes(CTP_TARGET_CLIENT);
    deleteTypes(CTP_SOURCE_CLIENT);
  }

  /**
   * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT}
   * projects that were set up in this test class.
   */
  @AfterAll
  static void tearDown() {
    deleteTypesFromTargetAndSource();
  }

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {
    // preparation
    final List<Type> types =
        CTP_SOURCE_CLIENT
            .types()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<TypeDraft> typeDrafts =
        types.stream()
            .map(
                type ->
                    TypeDraftBuilder.of()
                        .key(type.getKey())
                        .name(type.getName())
                        .resourceTypeIds(type.getResourceTypeIds())
                        .description(type.getDescription())
                        .fieldDefinitions(type.getFieldDefinitions()))
            .map(TypeDraftBuilder::build)
            .collect(Collectors.toList());

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(typeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(typeSyncStatistics).hasValues(2, 1, 0, 0);
    assertThat(typeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 types were processed in total"
                + " (1 created, 0 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithUpdates_ShouldReturnProperStatistics() {
    // preparation
    final List<Type> types =
        CTP_SOURCE_CLIENT
            .types()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<TypeDraft> typeDrafts =
        types.stream()
            .map(
                type ->
                    TypeDraftBuilder.of()
                        .key(type.getKey())
                        .name(LocalizedString.ofEnglish("updated_name"))
                        .resourceTypeIds(type.getResourceTypeIds())
                        .description(type.getDescription())
                        .fieldDefinitions(type.getFieldDefinitions()))
            .map(TypeDraftBuilder::build)
            .collect(Collectors.toList());

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(typeDrafts).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(typeSyncStatistics).hasValues(2, 1, 1, 0);
    assertThat(typeSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 types were processed in total"
                + " (1 created, 1 updated and 0 failed to sync).");
  }
}
