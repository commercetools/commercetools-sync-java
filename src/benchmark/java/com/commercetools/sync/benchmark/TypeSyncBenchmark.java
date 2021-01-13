package com.commercetools.sync.benchmark;

import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_AND_UPDATES;
import static com.commercetools.sync.benchmark.BenchmarkUtils.CREATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.NUMBER_OF_RESOURCE_UNDER_TEST;
import static com.commercetools.sync.benchmark.BenchmarkUtils.THRESHOLD_EXCEEDED_ERROR;
import static com.commercetools.sync.benchmark.BenchmarkUtils.TYPE_SYNC;
import static com.commercetools.sync.benchmark.BenchmarkUtils.UPDATES_ONLY;
import static com.commercetools.sync.benchmark.BenchmarkUtils.saveNewResult;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.FIELD_DEFINITION_NAME_1;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.deleteTypes;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.types.TypeSync;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.queries.TypeQuery;
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

  @AfterEach
  void tearDown() {
    deleteTypes(CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteTypes(CTP_TARGET_CLIENT);
    typeSyncOptions = buildSyncOptions();
  }

  @Nonnull
  private TypeSyncOptions buildSyncOptions() {
    final QuadConsumer<SyncException, Optional<TypeDraft>, Optional<Type>, List<UpdateAction<Type>>>
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
    final List<TypeDraft> typeDrafts = buildTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final TypeSyncStatistics syncStatistics = executeBlocking(typeSync.sync(typeDrafts));
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was ~13 seconds)
    final int threshold = 26000; // double of the highest benchmark
    assertThat(totalTime)
        .withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
        .isLessThan(threshold);

    // Assert actual state of CTP project (total number of existing types)
    final CompletableFuture<Integer> totalNumberOfTypes =
        CTP_TARGET_CLIENT
            .execute(TypeQuery.of())
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();

    executeBlocking(totalNumberOfTypes);
    assertThat(totalNumberOfTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, NUMBER_OF_RESOURCE_UNDER_TEST, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    saveNewResult(TYPE_SYNC, CREATES_ONLY, totalTime);
  }

  @Test
  void sync_ExistingTypes_ShouldUpdateTypes() throws IOException {
    // preparation
    final List<TypeDraft> typeDrafts = buildTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    // Create drafts to target project with different field type name
    CompletableFuture.allOf(
            typeDrafts.stream()
                .map(TypeDraftBuilder::of)
                .map(TypeSyncBenchmark::applyFieldDefinitionNameChange)
                .map(TypeDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.execute(TypeCreateCommand.of(draft)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final TypeSyncStatistics syncStatistics = executeBlocking(typeSync.sync(typeDrafts));
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was ~13 seconds)
    final int threshold = 26000; // double of the highest benchmark
    assertThat(totalTime)
        .withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
        .isLessThan(threshold);

    // Assert actual state of CTP project (number of updated types)
    final CompletableFuture<Integer> totalNumberOfUpdatedTypes =
        CTP_TARGET_CLIENT
            .execute(
                TypeQuery.of()
                    .withPredicates(p -> p.fieldDefinitions().name().is(FIELD_DEFINITION_NAME_1)))
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();

    executeBlocking(totalNumberOfUpdatedTypes);
    assertThat(totalNumberOfUpdatedTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert actual state of CTP project (total number of existing types)
    final CompletableFuture<Integer> totalNumberOfTypes =
        CTP_TARGET_CLIENT
            .execute(TypeQuery.of())
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();
    executeBlocking(totalNumberOfTypes);
    assertThat(totalNumberOfTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, 0, NUMBER_OF_RESOURCE_UNDER_TEST, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    saveNewResult(TYPE_SYNC, UPDATES_ONLY, totalTime);
  }

  @Test
  void sync_WithSomeExistingTypes_ShouldSyncTypes() throws IOException {
    // preparation
    final List<TypeDraft> typeDrafts = buildTypeDrafts(NUMBER_OF_RESOURCE_UNDER_TEST);
    final int halfNumberOfDrafts = typeDrafts.size() / 2;
    final List<TypeDraft> firstHalf = typeDrafts.subList(0, halfNumberOfDrafts);

    // Create first half of drafts to target project with different field definition name
    CompletableFuture.allOf(
            firstHalf.stream()
                .map(TypeDraftBuilder::of)
                .map(TypeSyncBenchmark::applyFieldDefinitionNameChange)
                .map(TypeDraftBuilder::build)
                .map(draft -> CTP_TARGET_CLIENT.execute(TypeCreateCommand.of(draft)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();

    final TypeSync typeSync = new TypeSync(typeSyncOptions);

    // benchmark
    final long beforeSyncTime = System.currentTimeMillis();
    final TypeSyncStatistics syncStatistics = executeBlocking(typeSync.sync(typeDrafts));
    final long totalTime = System.currentTimeMillis() - beforeSyncTime;

    // assert on threshold (based on history of benchmarks; highest was ~12 seconds)
    final int threshold = 24000; // double of the highest benchmark
    assertThat(totalTime)
        .withFailMessage(format(THRESHOLD_EXCEEDED_ERROR, totalTime, threshold))
        .isLessThan(threshold);

    // Assert actual state of CTP project (number of updated types)
    final CompletableFuture<Integer> totalNumberOfUpdatedTypesWithOldFieldDefinitionName =
        CTP_TARGET_CLIENT
            .execute(
                TypeQuery.of()
                    .withPredicates(
                        p -> p.fieldDefinitions().name().is(FIELD_DEFINITION_NAME_1 + "_old")))
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();

    executeBlocking(totalNumberOfUpdatedTypesWithOldFieldDefinitionName);
    assertThat(totalNumberOfUpdatedTypesWithOldFieldDefinitionName).isCompletedWithValue(0);

    // Assert actual state of CTP project (total number of existing types)
    final CompletableFuture<Integer> totalNumberOfTypes =
        CTP_TARGET_CLIENT
            .execute(TypeQuery.of())
            .thenApply(PagedQueryResult::getTotal)
            .thenApply(Long::intValue)
            .toCompletableFuture();
    executeBlocking(totalNumberOfTypes);
    assertThat(totalNumberOfTypes).isCompletedWithValue(NUMBER_OF_RESOURCE_UNDER_TEST);

    // Assert statistics
    assertThat(syncStatistics)
        .hasValues(NUMBER_OF_RESOURCE_UNDER_TEST, halfNumberOfDrafts, halfNumberOfDrafts, 0);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    saveNewResult(TYPE_SYNC, CREATES_AND_UPDATES, totalTime);
  }

  @Nonnull
  private static List<TypeDraft> buildTypeDrafts(final int numberOfTypes) {
    return IntStream.range(0, numberOfTypes)
        .mapToObj(
            i ->
                TypeDraftBuilder.of(
                        format("key__%d", i),
                        LocalizedString.ofEnglish(format("name__%d", i)),
                        ResourceTypeIdsSetBuilder.of().addCategories().build())
                    .description(LocalizedString.ofEnglish(format("description__%d", i)))
                    .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
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
                    FieldDefinition.of(
                        fieldDefinition.getType(),
                        fieldDefinition.getName() + "_old",
                        fieldDefinition.getLabel(),
                        fieldDefinition.isRequired(),
                        fieldDefinition.getInputHint()))
            .collect(Collectors.toList());

    return builder.fieldDefinitions(list);
  }
}
