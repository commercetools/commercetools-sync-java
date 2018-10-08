package com.commercetools.sync.integration.services.impl;


import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.updateactions.ChangeName;
import io.sphere.sdk.types.queries.TypeQuery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.types.utils.TypeITUtils.FIELD_DEFINITION_1;
import static com.commercetools.sync.integration.types.utils.TypeITUtils.TYPE_DESCRIPTION_1;
import static com.commercetools.sync.integration.types.utils.TypeITUtils.TYPE_KEY_1;
import static com.commercetools.sync.integration.types.utils.TypeITUtils.TYPE_NAME_1;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TypeServiceImplIT {
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

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
        typeService = new TypeServiceImpl(typeSyncOptions);
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
    public void fetchCachedTypeId_WithNewlyCreatedTypeAfterCaching_ShouldNotFetchNewType() {
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
    public void fetchMatchingTypesByKeys_WithEmptySetOfKeys_ShouldReturnEmptyList() {
        final Set<String> typeKeys = new HashSet<>();
        final List<Type> matchingTypes = typeService.fetchMatchingTypesByKeys(typeKeys)
                .toCompletableFuture()
                .join();

        assertThat(matchingTypes).isEmpty();
    }

    @Test
    public void fetchMatchingTypesByKeys_WithNonExistingKeys_ShouldReturnEmptyList() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add("type_key_1");
        typeKeys.add("type_key_2");

        final List<Type> matchingTypes = typeService.fetchMatchingTypesByKeys(typeKeys)
                .toCompletableFuture()
                .join();

        assertThat(matchingTypes).isEmpty();
    }

    @Test
    public void fetchMatchingTypesByKeys_WithAnyExistingKeys_ShouldReturnAListOfTypes() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add(OLD_TYPE_KEY);

        final List<Type> matchingTypes = typeService.fetchMatchingTypesByKeys(typeKeys)
                .toCompletableFuture()
                .join();

        assertThat(matchingTypes).isNotEmpty();
        assertThat(matchingTypes).hasSize(1);
    }

    @Test
    public void createType_WithValidType_ShouldCreateType() {
        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                TYPE_NAME_1,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                .build();

        final Type createdType = typeService.createType(newTypeDraft)
                .toCompletableFuture().join();

        assertThat(createdType).isNotNull();

        final Optional<Type> typeOptional = CTP_TARGET_CLIENT
                .execute(TypeQuery.of()
                        .withPredicates(typeQueryModel -> typeQueryModel.key().is(createdType.getKey())))
                .toCompletableFuture().join().head();

        assertThat(typeOptional).isNotEmpty();
        final Type fetchedType = typeOptional.get();
        assertThat(fetchedType.getKey()).isEqualTo(newTypeDraft.getKey());
        assertThat(fetchedType.getDescription()).isEqualTo(createdType.getDescription());
        assertThat(fetchedType.getName()).isEqualTo(createdType.getName());
        assertThat(fetchedType.getFieldDefinitions()).isEqualTo(createdType.getFieldDefinitions());
    }

    @Test
    public void createType_WithInvalidType_ShouldNotCreateType() {
        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
                TYPE_KEY_1,
                null,
                ResourceTypeIdsSetBuilder.of().addCategories().build())
                .description(TYPE_DESCRIPTION_1)
                .fieldDefinitions(singletonList(FIELD_DEFINITION_1))
                .build();

        typeService.createType(newTypeDraft)
                .exceptionally(exception -> {
                    assertThat(exception).isNotNull();
                    assertThat(exception.getMessage()).contains("Request body does not contain valid JSON.");
                    return null;
                })
                .toCompletableFuture().join();
    }

    @Test
    public void updateType_WithValidChanges_ShouldUpdateTypeCorrectly() {
        final Optional<Type> typeOptional = CTP_TARGET_CLIENT
                .execute(TypeQuery.of()
                        .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
                .toCompletableFuture().join().head();
        assertThat(typeOptional).isNotNull();

        final ChangeName changeNameUpdateAction = ChangeName.of(LocalizedString.ofEnglish("new_type_name"));

        final Type updatedType = typeService.updateType(typeOptional.get(), singletonList(changeNameUpdateAction))
                .toCompletableFuture().join();
        assertThat(updatedType).isNotNull();

        final Optional<Type> updatedTypeOptional = CTP_TARGET_CLIENT
                .execute(TypeQuery.of()
                        .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
                .toCompletableFuture().join().head();

        assertThat(typeOptional).isNotEmpty();
        final Type fetchedType = updatedTypeOptional.get();
        assertThat(fetchedType.getKey()).isEqualTo(updatedType.getKey());
        assertThat(fetchedType.getDescription()).isEqualTo(updatedType.getDescription());
        assertThat(fetchedType.getName()).isEqualTo(updatedType.getName());
        assertThat(fetchedType.getFieldDefinitions()).isEqualTo(updatedType.getFieldDefinitions());
    }

    @Test
    public void updateType_WithInvalidChanges_ShouldNotUpdateType() {
        final Optional<Type> typeOptional = CTP_TARGET_CLIENT
                .execute(TypeQuery.of()
                        .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
                .toCompletableFuture().join().head();
        assertThat(typeOptional).isNotNull();

        final ChangeName changeNameUpdateAction = ChangeName.of(null);
        typeService.updateType(typeOptional.get(), singletonList(changeNameUpdateAction))
                .exceptionally(exception -> {
                    assertThat(exception).isNotNull();
                    assertThat(exception.getMessage()).contains("Request body does not contain valid JSON.");
                    return null;
                })
                .toCompletableFuture().join();
    }

}
