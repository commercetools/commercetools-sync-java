package com.commercetools.sync.integration.services.impl;


import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.updateactions.ChangeKey;
import io.sphere.sdk.types.commands.updateactions.ChangeName;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
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
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TypeServiceImplIT {
    private TypeService typeService;
    private static final String OLD_TYPE_KEY = "old_type_key";
    private static final String OLD_TYPE_NAME = "old_type_name";
    private static final Locale OLD_TYPE_LOCALE = Locale.ENGLISH;

    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Deletes types from source and target CTP projects, then it populates target CTP project with test data.
     */
    @Before
    public void setup() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();

        deleteTypesFromTargetAndSource();
        createCategoriesCustomType(OLD_TYPE_KEY, OLD_TYPE_LOCALE, OLD_TYPE_NAME, CTP_TARGET_CLIENT);

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                      .errorCallback((errorMessage, exception) -> {
                                                                          errorCallBackMessages.add(errorMessage);
                                                                          errorCallBackExceptions.add(exception);
                                                                      })
                                                                      .build();
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
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchCachedTypeId_WithExistingType_ShouldFetchTypeAndCache() {
        final Optional<String> typeId = typeService.fetchCachedTypeId(OLD_TYPE_KEY)
                                                   .toCompletableFuture()
                                                   .join();
        assertThat(typeId).isNotEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
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
    public void fetchMatchingTypesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
        final Set<String> typeKeys = new HashSet<>();
        final Set<Type> matchingTypes = typeService.fetchMatchingTypesByKeys(typeKeys)
                                                   .toCompletableFuture()
                                                   .join();

        assertThat(matchingTypes).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingTypesByKeys_WithNonExistingKeys_ShouldReturnEmptySet() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add("type_key_1");
        typeKeys.add("type_key_2");

        final Set<Type> matchingTypes = typeService.fetchMatchingTypesByKeys(typeKeys)
                                                   .toCompletableFuture()
                                                   .join();

        assertThat(matchingTypes).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingTypesByKeys_WithAnyExistingKeys_ShouldReturnASetOfTypes() {
        final Set<String> typeKeys = new HashSet<>();
        typeKeys.add(OLD_TYPE_KEY);

        final Set<Type> matchingTypes = typeService.fetchMatchingTypesByKeys(typeKeys)
                                                   .toCompletableFuture()
                                                   .join();

        assertThat(matchingTypes).isNotEmpty();
        assertThat(matchingTypes).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    // TODO: GITHUB ISSUE#331
    @Ignore
    @Test
    public void fetchMatchingTypesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadeGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(TypeQuery.class)))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
                .thenCallRealMethod();

        final TypeSyncOptions spyOptions =
                TypeSyncOptionsBuilder.of(spyClient)
                                      .errorCallback((errorMessage, exception) -> {
                                          errorCallBackMessages.add(errorMessage);
                                          errorCallBackExceptions.add(exception);
                                      })
                                      .build();

        final TypeService spyTypeService = new TypeServiceImpl(spyOptions);


        final Set<String> keys = new HashSet<>();
        keys.add(OLD_TYPE_KEY);
        final Set<Type> fetchedTypes = spyTypeService.fetchMatchingTypesByKeys(keys)
                                                     .toCompletableFuture().join();
        assertThat(fetchedTypes).hasSize(0);
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0))
                .isEqualToIgnoringCase(format("Failed to fetch types with keys: '%s'. Reason: %s",
                        keys.toString(), errorCallBackExceptions.get(0)));
    }

    @Test
    public void fetchMatchingTypesByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedTypeIds() {
        final Set<Type> fetchedProductTypes = typeService.fetchMatchingTypesByKeys(singleton(OLD_TYPE_KEY))
                                                         .toCompletableFuture().join();
        assertThat(fetchedProductTypes).hasSize(1);

        final Optional<Type> typeOptional = CTP_TARGET_CLIENT
                .execute(TypeQuery.of().withPredicates(queryModel -> queryModel.key().is(OLD_TYPE_KEY)))
                .toCompletableFuture()
                .join()
                .head();

        assertThat(typeOptional).isNotNull();

        // Change type old_type_key on ctp
        final String newKey = "new_type_key";
        typeService.updateType(typeOptional.get(), Collections.singletonList(ChangeKey.of(newKey)))
                   .toCompletableFuture()
                   .join();

        // Fetch cached id by old key
        final Optional<String> cachedTypeId = typeService.fetchCachedTypeId(OLD_TYPE_KEY)
                                                         .toCompletableFuture()
                                                         .join();

        assertThat(cachedTypeId).isNotEmpty();
        assertThat(cachedTypeId).contains(typeOptional.get().getId());
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
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

        final Optional<Type> createdType = typeService.createType(newTypeDraft)
                                            .toCompletableFuture().join();

        assertThat(createdType).isNotNull();

        final Optional<Type> fetchedType = CTP_TARGET_CLIENT
            .execute(TypeQuery.of()
                              .withPredicates(typeQueryModel ->
                                  typeQueryModel.key().is(createdType.get().getKey())))
            .toCompletableFuture().join().head();

        assertThat(fetchedType).hasValueSatisfying(oldType ->
            assertThat(createdType).hasValueSatisfying(newType -> {
                assertThat(newType.getKey()).isEqualTo(newTypeDraft.getKey());
                assertThat(newType.getDescription()).isEqualTo(oldType.getDescription());
                assertThat(newType.getName()).isEqualTo(oldType.getName());
                assertThat(newType.getFieldDefinitions()).isEqualTo(oldType.getFieldDefinitions());
            }));
    }

    @Test
    public void createType_WithInvalidType_ShouldCompleteExceptionally() {
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
    public void updateType_WithInvalidChanges_ShouldCompleteExceptionally() {
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

    @Test
    public void fetchType_WithExistingTypeKey_ShouldFetchType() {
        final Optional<Type> typeOptional = CTP_TARGET_CLIENT
            .execute(TypeQuery.of()
                              .withPredicates(typeQueryModel -> typeQueryModel.key().is(OLD_TYPE_KEY)))
            .toCompletableFuture().join().head();
        assertThat(typeOptional).isNotNull();

        final Optional<Type> fetchedTypeOptional =
            executeBlocking(typeService.fetchType(OLD_TYPE_KEY));
        assertThat(fetchedTypeOptional).isEqualTo(typeOptional);
    }

    @Test
    public void fetchType_WithBlankKey_ShouldNotFetchType() {
        final Optional<Type> fetchedTypeOptional =
            executeBlocking(typeService.fetchType(StringUtils.EMPTY));
        assertThat(fetchedTypeOptional).isEmpty();
    }

    @Test
    public void fetchType_WithNullKey_ShouldNotFetchType() {
        final Optional<Type> fetchedTypeOptional =
            executeBlocking(typeService.fetchType(null));
        assertThat(fetchedTypeOptional).isEmpty();
    }

    @Test
    public void fetchType_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadeGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(TypeQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
            .thenCallRealMethod();
        final TypeSyncOptions spyOptions = TypeSyncOptionsBuilder.of(spyClient)
                                                                         .errorCallback(
                                                                             (errorMessage, exception) -> {
                                                                                 errorCallBackMessages
                                                                                     .add(errorMessage);
                                                                                 errorCallBackExceptions
                                                                                     .add(exception);
                                                                             })
                                                                         .build();
        final TypeService spyTypeService = new TypeServiceImpl(spyOptions);

        final Optional<Type> fetchedTypeOptional =
            executeBlocking(spyTypeService.fetchType(OLD_TYPE_KEY));
        assertThat(fetchedTypeOptional).isEmpty();
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualToIgnoringCase(format("Failed to fetch types with keys: '%s'. Reason: %s",
                OLD_TYPE_KEY, errorCallBackExceptions.get(0)));
    }

}
