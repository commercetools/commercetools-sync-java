package com.commercetools.sync.integration.categories.utils;

import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.BooleanFieldType;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedStringFieldType;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.fetchAndProcess;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class CategoryITUtils {
    public static final String OLD_CATEGORY_CUSTOM_TYPE_KEY = "oldCategoryCustomTypeKey";
    public static final String OLD_CATEGORY_CUSTOM_TYPE_NAME = "old_type_name";
    public static final Locale OLD_CATEGORY_CUSTOM_TYPE_LOCALE = Locale.ENGLISH;
    public static final String LOCALISED_STRING_CUSTOM_FIELD_NAME = "backgroundColor";
    public static final String BOOLEAN_CUSTOM_FIELD_NAME = "invisibleInShop";

    /**
     * Builds a list of CategoryDraft objects that can be used for integration tests to mock existing categories in
     * a target CTP project for example.
     *
     * @return a list of CategoryDrafts.
     */
    public static List<CategoryDraft> getMockCategoryDrafts() {
        return Arrays.asList(getMockCategoryDraft(Locale.GERMAN, "draft", "slug", "1"),
            getMockCategoryDraft(Locale.GERMAN, "draft1", "slug1", "2"),
            getMockCategoryDraft(Locale.GERMAN, "draft2", "slug2", "3"),
            getMockCategoryDraft(Locale.GERMAN, "draft3", "slug3", "4"),
            getMockCategoryDraft(Locale.GERMAN, "draft4", "slug4", "5"),
            getMockCategoryDraft(Locale.GERMAN, "draft5", "slug5", "6"),
            getMockCategoryDraft(Locale.GERMAN, "draft6", "slug6", "7"),
            getMockCategoryDraft(Locale.GERMAN, "draft7", "slug7", "8"),
            getMockCategoryDraft(Locale.ENGLISH, "draft8", "slug8", "9"),
            getMockCategoryDraft(Locale.GERMAN, "draft9", "slug9", "10"),
            getMockCategoryDraft(Locale.GERMAN, "draft10", "slug10", "11"),
            getMockCategoryDraft(Locale.GERMAN, "draft11", "slug11", "12"),
            getMockCategoryDraft(Locale.GERMAN, "draft12", "slug12", "13"),
            getMockCategoryDraft(Locale.GERMAN, "draft13", "slug13", "14"),
            getMockCategoryDraft(Locale.GERMAN, "draft14", "slug14", "15"),
            getMockCategoryDraft(Locale.GERMAN, "draft15", "slug15", "16"),
            getMockCategoryDraft(Locale.GERMAN, "draft16", "slug16", "17"),
            getMockCategoryDraft(Locale.GERMAN, "draft17", "slug17", "18"));
    }

    /**
     * Builds a list of CategoryDraft objects (with a customised {@code prefix} to each category's name with the
     * supplied {@code locale}. They can be used for integration tests to mock existing categories in
     * a target CTP project for example.
     *
     * @return a list of CategoryDrafts.
     */
    public static List<CategoryDraft> getMockCategoryDraftsWithNamePrefix(@Nonnull final Locale locale,
                                                                           @Nonnull final String prefix) {
        List<CategoryDraft> categoryDrafts = new ArrayList<>();
        for (CategoryDraft categoryDraft : getMockCategoryDrafts()) {
            final LocalizedString newCategoryName = LocalizedString.of(locale,
                format("%s%s", prefix, categoryDraft.getName().get(locale)));
            final CategoryDraftBuilder categoryDraftBuilder = CategoryDraftBuilder.of(categoryDraft)
                                                                                  .name(newCategoryName);
            categoryDrafts.add(categoryDraftBuilder.build());
        }
        return categoryDrafts;
    }

    /**
     * Given a {@code locale}, {@code name}, {@code slug} and {@code externalId}; this method creates a mock of
     * {@link CategoryDraft} with all those supplied fields. All the supplied arguments are given as {@link String} and
     * the method internally converts them to their required types. For example, for all the fields that require a
     * {@link LocalizedString} as a value type; the method creates an instance of a {@link LocalizedString} with
     * the given {@link String} and {@link Locale}.
     *
     * @param locale     the locale to create with all the {@link LocalizedString} instances.
     * @param name       the name of the category.
     * @param slug       the slug of the category.
     * @param externalId the external id of the category.
     * @return an instance {@link CategoryDraft} with all the given fields set in the given {@link Locale}.
     */
    public static CategoryDraft getMockCategoryDraft(@Nonnull final Locale locale,
                                                     @Nonnull final String name,
                                                     @Nonnull final String slug,
                                                     @Nullable final String externalId) {

        return CategoryDraftBuilder.of(LocalizedString.of(locale, name), LocalizedString.of(locale, slug))
                                   .externalId(externalId)
                                   .custom(getMockCustomFieldsDraft())
                                   .build();
    }

    /**
     * Creates a mock instance of {@link CustomFieldsDraft} with the key 'StepCategoryTypeKey' and two custom fields
     * 'invisibleInShop' and'backgroundColor'.
     *
     * <p>The 'invisibleInShop' field is of type {@code boolean} and has value {@code false} and the
     * the 'backgroundColor' field is of type {@code localisedString} and has the values {"de": "rot", "en": "red"}
     *
     * @return a mock instance of {@link CustomFieldsDraft} with some hardcoded custom fields and key.
     */
    public static CustomFieldsDraft getMockCustomFieldsDraft() {
        return CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons());
    }

    /**
     * Builds a mock {@link Map} for the custom fields to their {@link JsonNode} values that looks like that in JSON
     * format:
     *
     * <p>"fields": {"invisibleInShop": false, "backgroundColor": { "en": "red", "de": "rot"}}
     * @return a Map of the custom fields to their JSON values with mock data.
     */
    public static Map<String, JsonNode> getMockCustomFieldsJsons() {
        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        customFieldsJsons
            .put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));
        return customFieldsJsons;
    }

    /**
     * This method blocks to create the supplied {@code categoryDrafts} in the CTP project defined by the supplied
     * {@code sphereClient}, it also creates a custom type for which the created categories can use for their custom
     * fields.
     * @param sphereClient defines the CTP project to create the categories on.
     * @param categoryDrafts the drafts to build the categories from.
     */
    public static void createCategories(@Nonnull final SphereClient sphereClient,
                                        @Nonnull final List<CategoryDraft> categoryDrafts) {
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, OLD_CATEGORY_CUSTOM_TYPE_LOCALE,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, sphereClient);
        final List<CompletableFuture<Category>> futures = new ArrayList<>();
        for (CategoryDraft categoryDraft : categoryDrafts) {
            final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
            final CompletableFuture<Category> categoryCompletableFuture =
                sphereClient.execute(categoryCreateCommand).toCompletableFuture();
            futures.add(categoryCompletableFuture);

        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                         .toCompletableFuture().join();
    }

    /**
     * This method blocks to create a category custom Type on the CTP project defined by the supplied
     * {@code sphereClient}, with the supplied data.
     * @param typeKey the type key
     * @param locale the locale to be used for specifying the type name and field definitions names.
     * @param name the name of the custom type.
     * @param sphereClient defines the CTP project to create the type on.
     */
    public static void createCategoriesCustomType(@Nonnull final String typeKey,
                                                  @Nonnull final Locale locale,
                                                  @Nonnull final String name,
                                                  @Nonnull final SphereClient sphereClient) {
        final TypeDraft typeDraft = TypeDraftBuilder
            .of(typeKey, LocalizedString.of(locale, name), ResourceTypeIdsSetBuilder.of().addCategories())
            .fieldDefinitions(buildFieldDefinitions(locale))
            .build();
        sphereClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();
    }

    /**
     * Builds a list of two field definitions; one for a {@link LocalizedStringFieldType} and one for a
     * {@link BooleanFieldType}. The JSON of the created field definition list looks as follows:
     *
     * <p>"fieldDefinitions": [
     {
     "name": "backgroundColor",
     "label": {
     "en": "backgroundColor"
     },
     "required": false,
     "type": {
     "name": "LocalizedString"
     },
     "inputHint": "SingleLine"
     },
     {
     "name": "invisibleInShop",
     "label": {
     "en": "invisibleInShop"
     },
     "required": false,
     "type": {
     "name": "Boolean"
     },
     "inputHint": "SingleLine"
     }
     ]
     * @param locale defines the locale for which the field definition names are going to be bound to.
     * @return the list of field definitions.
     */
    public static List<FieldDefinition> buildFieldDefinitions(@Nonnull final Locale locale) {
        return asList(
            FieldDefinition
                .of(LocalizedStringFieldType.of(), LOCALISED_STRING_CUSTOM_FIELD_NAME,
                    LocalizedString.of(locale, LOCALISED_STRING_CUSTOM_FIELD_NAME), false),
            FieldDefinition
                .of(BooleanFieldType.of(), BOOLEAN_CUSTOM_FIELD_NAME,
                    LocalizedString.of(locale, BOOLEAN_CUSTOM_FIELD_NAME), false));

    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} categories from CTP
     * projects {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteCategoriesFromTargetAndSource() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteAllCategories(CTP_SOURCE_CLIENT);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} categories from CTP
     * projects defined by the {@code sphereClient}.
     *
     * @param sphereClient defines the CTP project to delete the categories from.
     */
    public static void deleteAllCategories(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, CategoryQuery.of().withLimit(QUERY_MAX_LIMIT), CategoryDeleteCommand::of);
    }
}
