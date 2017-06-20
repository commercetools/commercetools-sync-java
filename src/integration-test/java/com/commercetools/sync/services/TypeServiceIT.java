package com.commercetools.sync.services;


import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.createCategoriesCustomType;
import static org.assertj.core.api.Assertions.assertThat;

public class TypeServiceIT {
    private TypeService typeService;
    private static final String OLD_TYPE_KEY = "old_type_key";
    private static final String OLD_TYPE_NAME = "old_type_name";
    private static final Locale OLD_TYPE_LOCALE = Locale.ENGLISH;

    /**
     * Deletes types from source and target CTP projects, then it populates target CTP project with test data.
     */
    @Before
    public void setup() {
        deleteTypesFromTargetAndSource();
        createCategoriesCustomType(OLD_TYPE_KEY, OLD_TYPE_LOCALE, OLD_TYPE_NAME, CTP_TARGET_CLIENT);
        typeService = new TypeServiceImpl(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteTypesFromTargetAndSource();
    }

    @Test
    public void fetchCachedTypeId_WithNonExistingType_ShouldNotFetchAType() {
        final Optional<String> typeId = typeService.fetchCachedTypeId("non-existing-type-key")
                                                   .toCompletableFuture()
                                                   .join();
        assertThat(typeId).isEmpty();
    }

    @Test
    public void fetchCachedTypeId_WithExistingType_ShouldFetchTypeAndCache() {
        final Optional<String> typeId = typeService.fetchCachedTypeId(OLD_TYPE_KEY)
                                                  .toCompletableFuture()
                                                  .join();
        assertThat(typeId).isNotEmpty();
    }

    @Test
    public void fetchCachedTypeId_WithNonInvalidatedCache_ShouldFetchTypeAndCache() {
        // Fetch any type to populate cache
        typeService.fetchCachedTypeId("anyTypeKey").toCompletableFuture().join();

        // Create new type
        final String newTypeKey = "new_type_key";
        final TypeDraft draft = TypeDraftBuilder
            .of(newTypeKey, LocalizedString.of(Locale.ENGLISH, "typeName"),
                ResourceTypeIdsSetBuilder.of().addChannels())
            .build();
        CTP_TARGET_CLIENT.execute(TypeCreateCommand.of(draft)).toCompletableFuture().join();

        final Optional<String> newTypeId =
            typeService.fetchCachedTypeId(newTypeKey).toCompletableFuture().join();

        assertThat(newTypeId).isEmpty();
    }

    @Test
    public void fetchCachedTypeId_WithInvalidatedCache_ShouldFetchFreshCopyAndRepopulateCache() {
        // Fetch any type to populate cache
        typeService.fetchCachedTypeId(OLD_TYPE_KEY).toCompletableFuture().join();

        // Create new type
        final String newTypeKey = "new_type_key";
        final TypeDraft draft = TypeDraftBuilder
            .of(newTypeKey, LocalizedString.of(Locale.ENGLISH, "typeName"),
                ResourceTypeIdsSetBuilder.of().addChannels())
            .build();
        CTP_TARGET_CLIENT.execute(TypeCreateCommand.of(draft)).toCompletableFuture().join();

        typeService.invalidateCache();

        final Optional<String> newTypeId =
            typeService.fetchCachedTypeId(newTypeKey).toCompletableFuture().join();

        assertThat(newTypeId).isNotEmpty();
    }
}
