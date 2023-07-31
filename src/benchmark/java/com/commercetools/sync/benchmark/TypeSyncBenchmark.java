package com.commercetools.sync.benchmark;

import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_NAME_1;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.types.TypeSync;
import com.commercetools.sync.sdk2.types.TypeSyncOptions;
import com.commercetools.sync.sdk2.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.sdk2.types.helpers.TypeSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypeSyncBenchmark {

  private TypeSyncOptions typeSyncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  private static final int TYPE_BENCHMARKS_CREATE_ACTION_THRESHOLD =
      26_000; // (based on history of benchmarks; highest was ~13 seconds)
  private static final int TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD =
      26_000; // (based on history of benchmarks; highest was ~13 seconds)

  private static final int TYPE_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD =
      24_000; // (based on history of benchmarks; highest was ~12 seconds)

  @AfterEach
  void tearDown() {
    deleteTypes(CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
    deleteTypes(CTP_TARGET_CLIENT);
    try {
      // The removal of a FieldDefinition deletes asynchronously all Custom Fields using the
      // FieldDefinition as well.
      // Here with one second break we are slowing down the ITs a little bit so CTP could remove the
      // custom fields.
      Thread.sleep(5000);
    } catch (InterruptedException expected) {
    }
    clearSyncTestCollections();
    typeSyncOptions = buildSyncOptions();
  }

  @Nonnull
  private TypeSyncOptions buildSyncOptions() {
    final QuadConsumer<SyncException, Optional<TypeDraft>, Optional<Type>, List<TypeUpdateAction>>
        errorCallBack =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };
    final TriConsumer<SyncException, Optional<TypeDraft>, Optional<Type>> warningCallBack =
        (exception, newResource, oldResource) ->
            warningCallBackMessages.add(exception.getMessage());
    return TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallBack)
        .warningCallback(warningCallBack)
        .build();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
  }

  @Test
  void sync_NewTypes_ShouldCreateTypes() throws IOException {
    // preparation
    final List<TypeDraft> typeDrafts = buildTypeDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final TypeSyncStatistics syncStatistics =
        typeSync.sync(typeDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            String.format(BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR, totalTime, TYPE_BENCHMARKS_CREATE_ACTION_THRESHOLD))
        .isLessThan(TYPE_BENCHMARKS_CREATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (total number of existing types)
    final Long totalNumberOfTypes =
        CTP_TARGET_CLIENT
            .types()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(TypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfTypes).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    assertThat(syncStatistics)
        .hasValues(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST, BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(BenchmarkUtils.TYPE_SYNC, BenchmarkUtils.CREATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_ExistingTypes_ShouldUpdateTypes() throws IOException {
    // preparation
    final List<TypeDraft> typeDrafts = buildTypeDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    // Create drafts to target project with different field type name
    CompletableFuture.allOf(
            typeDrafts.stream()
                .map(TypeDraftBuilder::of)
                .map(TypeSyncBenchmark::applyFieldDefinitionNameChange)
                .map(TypeDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.types().post(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final TypeSyncStatistics syncStatistics =
        typeSync.sync(typeDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            String.format(BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR, totalTime, TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD))
        .isLessThan(TYPE_BENCHMARKS_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated types)
    final Long totalNumberOfUpdatedTypes =
        CTP_TARGET_CLIENT
            .types()
            .get()
            .withWhere("fieldDefinitions(name=:definitionName)")
            .withPredicateVar("definitionName", FIELD_DEFINITION_NAME_1)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(TypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfUpdatedTypes).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing types)
    final Long totalNumberOfTypes =
        CTP_TARGET_CLIENT
            .types()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(TypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();
    assertThat(totalNumberOfTypes).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST, 0, BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(BenchmarkUtils.TYPE_SYNC, BenchmarkUtils.UPDATES_ONLY, totalTime);
    }
  }

  @Test
  void sync_WithSomeExistingTypes_ShouldSyncTypes() throws IOException {
    // preparation
    final List<TypeDraft> typeDrafts = buildTypeDrafts(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);
    final int halfNumberOfDrafts = typeDrafts.size() / 2;
    final List<TypeDraft> firstHalf = typeDrafts.subList(0, halfNumberOfDrafts);

    // Create first half of drafts to target project with different field definition name
    CompletableFuture.allOf(
            firstHalf.stream()
                .map(TypeDraftBuilder::of)
                .map(TypeSyncBenchmark::applyFieldDefinitionNameChange)
                .map(TypeDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.types().post(draft).execute())
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final TypeSyncStatistics syncStatistics =
        typeSync.sync(typeDrafts).toCompletableFuture().join();
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    assertThat(totalTime)
        .withFailMessage(
            String.format(
                BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR,
                totalTime,
                TYPE_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD))
        .isLessThan(TYPE_BENCHMARKS_CREATE_AND_UPDATE_ACTION_THRESHOLD);

    // Assert actual state of CTP project (number of updated types)
    final Long totalNumberOfUpdatedTypesWithOldFieldDefinitionName =
        CTP_TARGET_CLIENT
            .types()
            .get()
            .withWhere("fieldDefinitions(name=:definitionName)")
            .withPredicateVar("definitionName", FIELD_DEFINITION_NAME_1 + "_old")
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(TypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfUpdatedTypesWithOldFieldDefinitionName).isEqualTo(0);

    // Assert actual state of CTP project (total number of existing types)
    final Long totalNumberOfTypes =
        CTP_TARGET_CLIENT
            .types()
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(TypePagedQueryResponse::getTotal)
            .toCompletableFuture()
            .join();

    assertThat(totalNumberOfTypes).isEqualTo(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST, halfNumberOfDrafts, halfNumberOfDrafts, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    if (BenchmarkUtils.SUBMIT_BENCHMARK_RESULT) {
      BenchmarkUtils.saveNewResult(BenchmarkUtils.TYPE_SYNC, BenchmarkUtils.CREATES_AND_UPDATES, totalTime);
    }
  }

  @Nonnull
  private static List<TypeDraft> buildTypeDrafts(final int numberOfTypes) {
    return IntStream.range(0, numberOfTypes)
        .mapToObj(
            i ->
                TypeDraftBuilder.of()
                    .key(format("key__%d", i))
                    .name(LocalizedString.ofEnglish(format("name__%d", i)))
                    .resourceTypeIds(ResourceTypeId.CATEGORY)
                    .description(LocalizedString.ofEnglish(format("description__%d", i)))
                    .fieldDefinitions(FIELD_DEFINITION_1)
                    .build())
        .collect(Collectors.toList());
  }

  @Nonnull
  private static TypeDraftBuilder applyFieldDefinitionNameChange(
      @Nonnull final TypeDraftBuilder builder) {
    final List<FieldDefinition> list =
        builder.getFieldDefinitions().stream()
            .map(
                fieldDefinition ->
                    FieldDefinitionBuilder.of()
                        .type(fieldDefinition.getType())
                        .name(fieldDefinition.getName() + "_old")
                        .label(fieldDefinition.getLabel())
                        .required(fieldDefinition.getRequired())
                        .inputHint(fieldDefinition.getInputHint())
                        .build())
            .collect(Collectors.toList());

    return builder.fieldDefinitions(list);
  }
}
