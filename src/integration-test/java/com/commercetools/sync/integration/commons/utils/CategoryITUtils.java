package com.commercetools.sync.integration.commons.utils;

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
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.types.BooleanFieldType;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedStringFieldType;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.queries.TypeQueryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class CategoryITUtils {
    public static final String OLD_CATEGORY_CUSTOM_TYPE_KEY = "oldCategoryCustomTypeKey";
    private static final String OLD_CATEGORY_CUSTOM_TYPE_NAME = "old_type_name";
    private static final Locale LOCALE = Locale.ENGLISH;
    public static final String LOCALISED_STRING_CUSTOM_FIELD_NAME = "backgroundColor";
    public static final String BOOLEAN_CUSTOM_FIELD_NAME = "invisibleInShop";
    private static final String ROOT_CATEGORY_EXTERNAL_ID = "rootCategoryExternalId";
    private static final String ROOT_CATEGORY_ORDER_HINT = "0.1";

    /**
     * Builds a list of the supplied number ({@code numberOfCategories}) of CategoryDraft objects that can be used for
     * integration tests to mock existing categories in a target CTP project for example. All the newly created category
     * drafts will have {@code parentCategory} as a parent.
     *
     * @param numberOfCategories the number of category drafts to create.
     * @param parentCategory     the parent of the drafts.
     * @return a list of CategoryDrafts.
     */
    public static List<CategoryDraft> getMockCategoryDrafts(@Nullable final Category parentCategory,
                                                            final int numberOfCategories) {
        List<CategoryDraft> categoryDrafts = new ArrayList<>();
        for (int i = 0; i < numberOfCategories; i++) {
            final LocalizedString name = LocalizedString.of(Locale.ENGLISH, format("draft%s", i));
            final LocalizedString slug = LocalizedString.of(Locale.ENGLISH, format("slug%s", i));
            final LocalizedString description = LocalizedString.of(Locale.ENGLISH, format("desc%s", i));
            final String externalId = format("externalId%s", i + 1);
            final String orderHint = format("0.%s", i);
            final CategoryDraft categoryDraft = CategoryDraftBuilder.of(name, slug)
                                                                    .parent(parentCategory)
                                                                    .description(description)
                                                                    .externalId(externalId)
                                                                    .orderHint(orderHint)
                                                                    .custom(getMockCustomFieldsDraft())
                                                                    .build();
            categoryDrafts.add(categoryDraft);
        }
        return categoryDrafts;
    }

    /**
     * Builds a list of the supplied number ({@code numberOfCategories}) of CategoryDraft objects (with a customized
     * prefix string for the name, slug and description) that can be used for integration tests to mock existing
     * categories in a target CTP project for example. All the newly created category drafts will have
     * {@code parentCategory} as a parent.
     *
     * @param numberOfCategories the number of category drafts to create.
     * @param parentCategory     the parent of the drafts.
     * @return a list of CategoryDrafts.
     */
    public static List<CategoryDraft> getMockCategoryDraftsWithPrefix(@Nonnull final Locale locale,
                                                                      @Nonnull final String prefix,
                                                                      @Nullable final Category parentCategory,
                                                                      final int numberOfCategories) {
        List<CategoryDraft> categoryDrafts = new ArrayList<>();
        for (CategoryDraft categoryDraft : getMockCategoryDrafts(parentCategory, numberOfCategories)) {
            final LocalizedString newCategoryName = LocalizedString.of(locale,
                format("%s%s", prefix, categoryDraft.getName().get(locale)));
            final LocalizedString newCategorySlug = LocalizedString.of(locale,
                format("%s%s", prefix, categoryDraft.getSlug().get(locale)));
            final LocalizedString newCategoryDescription = LocalizedString.of(locale,
                format("%s%s", prefix, categoryDraft.getDescription().get(locale)));
            final CategoryDraftBuilder categoryDraftBuilder = CategoryDraftBuilder.of(categoryDraft)
                                                                                  .name(newCategoryName)
                                                                                  .slug(newCategorySlug)
                                                                                  .description(newCategoryDescription);
            categoryDrafts.add(categoryDraftBuilder.build());
        }
        return categoryDrafts;
    }

    /**
     * Creates a mock instance of {@link CustomFieldsDraft} with the key defined by {@code OLD_CATEGORY_CUSTOM_TYPE_KEY}
     * and two custom fields 'invisibleInShop' and'backgroundColor'.
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
     *
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
     * {@code ctpClient},
     *
     * <p>Note: the method creates the given categories in parallel. So it expects them all to be in the same hierarchy
     * level.
     *
     * @param ctpClient      defines the CTP project to create the categories on.
     * @param categoryDrafts the drafts to build the categories from.
     */
    public static void createCategories(@Nonnull final SphereClient ctpClient,
                                        @Nonnull final List<CategoryDraft> categoryDrafts) {
        final List<CompletableFuture<Category>> futures = new ArrayList<>();
        for (CategoryDraft categoryDraft : categoryDrafts) {
            final CategoryCreateCommand categoryCreateCommand = CategoryCreateCommand.of(categoryDraft);
            final CompletableFuture<Category> categoryCompletableFuture =
                ctpClient.execute(categoryCreateCommand).toCompletableFuture();
            futures.add(categoryCompletableFuture);

        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                         .toCompletableFuture().join();
    }

    /**
     * it also creates a custom type for which the created categories can use for their custom
     * fields.
     *
     * @param ctpClient defines the CTP project to create the root category in.
     */
    public static Category createRootCategory(@Nonnull final SphereClient ctpClient) {
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, LOCALE, OLD_CATEGORY_CUSTOM_TYPE_NAME, ctpClient);
        final CategoryDraft rootCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "rootCategory"),
                LocalizedString.of(Locale.ENGLISH, "root-category", Locale.GERMAN, "root-category"))
            .description(LocalizedString.of(Locale.ENGLISH, "root Category"))
            .externalId(ROOT_CATEGORY_EXTERNAL_ID)
            .orderHint(ROOT_CATEGORY_ORDER_HINT)
            .build();
        return ctpClient.execute(CategoryCreateCommand.of(rootCategoryDraft))
                        .toCompletableFuture()
                        .join();
    }

    /**
     * This method blocks to create a category custom Type on the CTP project defined by the supplied
     * {@code ctpClient}, with the supplied data.
     *
     * @param typeKey   the type key
     * @param locale    the locale to be used for specifying the type name and field definitions names.
     * @param name      the name of the custom type.
     * @param ctpClient defines the CTP project to create the type on.
     */
    public static void createCategoriesCustomType(@Nonnull final String typeKey,
                                                  @Nonnull final Locale locale,
                                                  @Nonnull final String name,
                                                  @Nonnull final SphereClient ctpClient) {
        if (!typeExists(typeKey, ctpClient)) {
            final TypeDraft typeDraft = TypeDraftBuilder
                .of(typeKey, LocalizedString.of(locale, name), ResourceTypeIdsSetBuilder.of().addCategories())
                .fieldDefinitions(buildCategoryCustomTypeFieldDefinitions(locale))
                .build();
            ctpClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();
        }
    }

    private static boolean typeExists(@Nonnull final String typeKey, @Nonnull final SphereClient ctpClient) {
        final Optional<Type> typeOptional = ctpClient
            .execute(TypeQueryBuilder.of().predicates(QueryPredicate.of("key=\"" + typeKey + "\"")).build())
            .toCompletableFuture()
            .join().head();
        return typeOptional.isPresent();
    }

    /**
     * Builds a list of two field definitions; one for a {@link LocalizedStringFieldType} and one for a
     * {@link BooleanFieldType}. The JSON of the created field definition list looks as follows:
     *
     * <p>"fieldDefinitions": [
     * {
     * "name": "backgroundColor",
     * "label": {
     * "en": "backgroundColor"
     * },
     * "required": false,
     * "type": {
     * "name": "LocalizedString"
     * },
     * "inputHint": "SingleLine"
     * },
     * {
     * "name": "invisibleInShop",
     * "label": {
     * "en": "invisibleInShop"
     * },
     * "required": false,
     * "type": {
     * "name": "Boolean"
     * },
     * "inputHint": "SingleLine"
     * }
     * ]
     *
     * @param locale defines the locale for which the field definition names are going to be bound to.
     * @return the list of field definitions.
     */
    private static List<FieldDefinition> buildCategoryCustomTypeFieldDefinitions(@Nonnull final Locale locale) {
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
    public static void deleteRootCategoriesFromTargetAndSource() {
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteRootCategory(CTP_SOURCE_CLIENT);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} categories from CTP
     * projects defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the categories from.
     */
    public static void deleteRootCategory(@Nonnull final SphereClient ctpClient) {
        ctpClient.execute(CategoryQuery.of().byExternalId(ROOT_CATEGORY_EXTERNAL_ID))
                 .thenAccept(result -> result.head()
                                             .ifPresent(category -> ctpClient
                                                 .execute(CategoryDeleteCommand.of(category))
                                                 .toCompletableFuture().join()))
                 .toCompletableFuture().join();
    }

    /**
     * Takes a list of Categories that are supposed to have their custom type and parent category reference expanded
     * inorder to be able to fetch the keys and replace the reference ids with the correspnding keys and then return
     * a new list of category drafts with their references containing keys instead of the ids.
     *
     * @param categories the categories to replace their reference ids with keys
     * @return a list of category drafts with keys instead of ids for references.
     */
    public static List<CategoryDraft> replaceReferenceIdsWithKeys(@Nonnull final List<Category> categories) {
        return categories
            .stream()
            .map(category -> {
                CustomFieldsDraft customFieldsDraft = null;
                Reference<Category> parentReference = null;

                if (category.getCustom() != null && category.getCustom().getType().getObj() != null) {
                    customFieldsDraft = CustomFieldsDraft
                        .ofTypeIdAndJson(category.getCustom().getType().getObj().getKey(),
                            category.getCustom().getFieldsJsonMap());
                }

                if (category.getParent() != null && category.getParent().getObj() != null) {
                    parentReference = Category.referenceOfId(category.getParent().getObj().getExternalId());
                }
                return CategoryDraftBuilder.of(category)
                                           .custom(customFieldsDraft)
                                           .parent(parentReference)
                                           .build();
            })
            .collect(Collectors.toList());
    }
}
