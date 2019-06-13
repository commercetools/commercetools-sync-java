package com.commercetools.sync.integration.ctpprojectsource.types;

import com.commercetools.sync.types.TypeSync;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.queries.TypeQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.populateSourceProject;
import static com.commercetools.sync.integration.commons.utils.TypeITUtils.populateTargetProject;
import static org.assertj.core.api.Assertions.assertThat;

class TypeSyncIT {

    /**
     * Deletes types from source and target CTP projects.
     * Populates source and target CTP projects with test data.
     */
    @BeforeEach
    void setup() {
        deleteTypesFromTargetAndSource();
        populateSourceProject();
        populateTargetProject();
    }

    /**
     * Deletes all the test data from the {@code CTP_SOURCE_CLIENT} and the {@code CTP_SOURCE_CLIENT} projects that
     * were set up in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteTypesFromTargetAndSource();
    }

    @Test
    void sync_WithoutUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<Type> types = CTP_SOURCE_CLIENT
            .execute(TypeQuery.of())
            .toCompletableFuture().join().getResults();

        final List<TypeDraft> typeDrafts = types
            .stream()
            .map(type -> TypeDraftBuilder.of(type.getKey(), type.getName(), type.getResourceTypeIds())
                                         .description(type.getDescription())
                                         .fieldDefinitions(type.getFieldDefinitions()))
            .map(TypeDraftBuilder::build)
            .collect(Collectors.toList());

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        // test
        final TypeSyncStatistics typeSyncStatistics = typeSync
            .sync(typeDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(typeSyncStatistics).hasValues(2, 1, 0, 0);
        assertThat(typeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 types were processed in total"
                + " (1 created, 0 updated and 0 failed to sync).");

    }

    @Test
    void sync_WithUpdates_ShouldReturnProperStatistics() {
        // preparation
        final List<Type> types = CTP_SOURCE_CLIENT
            .execute(TypeQuery.of())
            .toCompletableFuture().join().getResults();

        final List<TypeDraft> typeDrafts = types
            .stream()
            .map(type -> TypeDraftBuilder.of(type.getKey(),
                LocalizedString.ofEnglish("updated_name"), type.getResourceTypeIds())
                                         .description(type.getDescription())
                                         .fieldDefinitions(type.getFieldDefinitions()))
            .map(TypeDraftBuilder::build)
            .collect(Collectors.toList());


        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((error, throwable) -> {
                errorMessages.add(error);
                exceptions.add(throwable);
            })
            .build();

        final TypeSync typeSync = new TypeSync(typeSyncOptions);

        // test
        final TypeSyncStatistics typeSyncStatistics = typeSync
            .sync(typeDrafts)
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(typeSyncStatistics).hasValues(2, 1, 1, 0);
        assertThat(typeSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 types were processed in total"
                + " (1 created, 1 updated and 0 failed to sync).");
    }
}
