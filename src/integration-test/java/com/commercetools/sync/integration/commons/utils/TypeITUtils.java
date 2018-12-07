package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.types.queries.TypeQueryBuilder;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singletonList;

public final class TypeITUtils {
    public static final String TYPE_KEY_1 = "key_1";
    public static final String TYPE_KEY_2 = "key_2";

    public static final LocalizedString TYPE_NAME_1 = LocalizedString.ofEnglish("name_1");
    public static final LocalizedString TYPE_NAME_2 = LocalizedString.ofEnglish("name_2");

    public static final String FIELD_DEFINITION_NAME_1 = "field_name_1";
    private static final String FIELD_DEFINITION_NAME_2 = "field_name_2";
    private static final String FIELD_DEFINITION_NAME_3 = "field_name_3";

    public static final LocalizedString FIELD_DEFINITION_LABEL_1 = LocalizedString.ofEnglish("label_1");
    private static final LocalizedString FIELD_DEFINITION_LABEL_2 = LocalizedString.ofEnglish("label_2");
    private static final LocalizedString FIELD_DEFINITION_LABEL_3 = LocalizedString.ofEnglish("label_3");

    public static final LocalizedString TYPE_DESCRIPTION_1 = LocalizedString.ofEnglish("description_1");
    public static final LocalizedString TYPE_DESCRIPTION_2 = LocalizedString.ofEnglish("description_2");

    public static final FieldDefinition FIELD_DEFINITION_1 = FieldDefinition.of(
            StringFieldType.of(),
            FIELD_DEFINITION_NAME_1,
            FIELD_DEFINITION_LABEL_1,
            true,
            TextInputHint.SINGLE_LINE);
    public static final FieldDefinition FIELD_DEFINITION_2 = FieldDefinition.of(
            StringFieldType.of(),
            FIELD_DEFINITION_NAME_2,
            FIELD_DEFINITION_LABEL_2,
            true,
            TextInputHint.SINGLE_LINE);
    public static final FieldDefinition FIELD_DEFINITION_3 = FieldDefinition.of(
            StringFieldType.of(),
            FIELD_DEFINITION_NAME_3,
            FIELD_DEFINITION_LABEL_3,
            true,
            TextInputHint.SINGLE_LINE);

    private static final TypeDraft typeDraft1 = TypeDraftBuilder
        .of(TYPE_KEY_1,
            TYPE_NAME_1,
            ResourceTypeIdsSetBuilder.of().addCategories().build())
        .description(TYPE_DESCRIPTION_1)
        .fieldDefinitions(Arrays.asList(FIELD_DEFINITION_1, FIELD_DEFINITION_2))
        .build();
    private static final TypeDraft typeDraft2 = TypeDraftBuilder
        .of(TYPE_KEY_2,
            TYPE_NAME_2,
            ResourceTypeIdsSetBuilder.of().addCategories().build())
        .description(TYPE_DESCRIPTION_2)
        .fieldDefinitions(singletonList(FIELD_DEFINITION_2))
        .build();

    /**
     * Deletes all types from CTP project, represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the types will be deleted from.
     */
    public static void deleteTypes(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, TypeQuery.of(), TypeDeleteCommand::of);
    }

    /**
     * Deletes all types from CTP projects defined by {@code CTP_SOURCE_CLIENT} and
     * {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTypesFromTargetAndSource() {
        deleteTypes(CTP_SOURCE_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    /**
     * Populate source CTP project.
     * Creates type with key TYPE_KEY_1, TYPE_NAME_1, TYPE_DESCRIPTION_1 and
     * fields FIELD_DEFINITION_1, FIELD_DEFINITION_2.
     * Creates type with key TYPE_KEY_2, TYPE_NAME_2, TYPE_DESCRIPTION_2 and
     * fields FIELD_DEFINITION_1.
     */
    public static void populateSourceProject() {
        CTP_SOURCE_CLIENT.execute(TypeCreateCommand.of(typeDraft1)).toCompletableFuture().join();
        CTP_SOURCE_CLIENT.execute(TypeCreateCommand.of(typeDraft2)).toCompletableFuture().join();
    }

    /**
     * Populate source CTP project.
     * Creates type with key TYPE_KEY_1, TYPE_NAME_1, TYPE_DESCRIPTION_1 and
     * fields FIELD_DEFINITION_1, FIELD_DEFINITION_2.
     */
    public static void populateTargetProject() {
        CTP_TARGET_CLIENT.execute(TypeCreateCommand.of(typeDraft1)).toCompletableFuture().join();
    }

    /**
     * Tries to fetch type of {@code key} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests.
     * @param key          key of requested type.
     * @return {@link Optional} which may contain type of {@code key}.
     */
    public static Optional<Type> getTypeByKey(
        @Nonnull final SphereClient sphereClient,
        @Nonnull final String key) {

        final TypeQuery query = TypeQueryBuilder
            .of()
            .plusPredicates(queryModel -> queryModel.key().is(key))
            .build();

        return sphereClient.execute(query).toCompletableFuture().join().head();
    }

    private TypeITUtils() {

    }
}
