package com.commercetools.sync.integration.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItemDraft;
import io.sphere.sdk.shoppinglists.commands.ShoppingListCreateCommand;
import io.sphere.sdk.shoppinglists.commands.ShoppingListDeleteCommand;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.NumberFieldType;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomers;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Arrays.asList;


public final class ShoppingListITUtils {

    /**
     * Deletes all shopping lists, products and product types from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete test data from.
     */
    public static void deleteShoppingListTestData(@Nonnull final SphereClient ctpClient) {
        deleteShoppingLists(ctpClient);
        deleteCustomers(ctpClient);
        deleteTypes(ctpClient);
        deleteAllProducts(ctpClient);
        deleteProductTypes(ctpClient);
    }

    /**
     * Deletes all ShoppingLists from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the ShoppingLists from.
     */
    public static void deleteShoppingLists(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, ShoppingListQuery.of(), ShoppingListDeleteCommand::of);
    }


    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the ShoppingList in.
     * @param name      the name of the ShoppingList to create.
     * @param key       the key of the ShoppingList to create.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingList(@Nonnull final SphereClient ctpClient, @Nonnull final String name,
                                                  @Nonnull final String key) {

        return createShoppingList(ctpClient, name, key, null, null, null, null);
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient                       defines the CTP project to create the ShoppingList in.
     * @param name                            the name of the ShoppingList to create.
     * @param key                             the key of the ShoppingList to create.
     * @param desc                            the description of the ShoppingList to create.
     * @param anonymousId                     the anonymous ID of the ShoppingList to create.
     * @param slug                            the slug of the ShoppingList to create.
     * @param deleteDaysAfterLastModification the deleteDaysAfterLastModification of the ShoppingList to create.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingList(@Nonnull final SphereClient ctpClient, @Nonnull final String name,
                                                  @Nonnull final String key, @Nullable final String desc,
                                                  @Nullable final String anonymousId, @Nullable final String slug,
                                                  @Nullable final Integer deleteDaysAfterLastModification) {

        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                                                                            .key(key)
                                                                            .description(
                                                                                desc == null ? null : LocalizedString.ofEnglish(
                                                                                    desc))
                                                                            .anonymousId(anonymousId)
                                                                            .slug(
                                                                                slug == null ? null : LocalizedString.ofEnglish(
                                                                                    slug))
                                                                            .deleteDaysAfterLastModification(
                                                                                deleteDaysAfterLastModification)
                                                                            .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the ShoppingList in.
     * @param name      the name of the ShoppingList to create.
     * @param key       the key of the ShoppingList to create.
     * @param customer  the Customer which ShoppingList refers to.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingListWithCustomer(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final String name,
        @Nonnull final String key,
        @Nonnull final Customer customer) {

        final ResourceIdentifier<Customer> customerResourceIdentifier = customer.toResourceIdentifier();
        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                                                                            .key(key)
                                                                            .customer(customerResourceIdentifier)
                                                                            .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient     defines the CTP project to create the ShoppingList in.
     * @param name          the name of the ShoppingList to create.
     * @param key           the key of the ShoppingList to create.
     * @param textLineItems the list of TextLineItemDraft which ShoppingList contains.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingListWithTextLineItems(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final String name,
        @Nonnull final String key,
        @Nonnull final List<TextLineItemDraft> textLineItems) {

        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                                                                            .key(key)
                                                                            .textLineItems(textLineItems)
                                                                            .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the ShoppingList in.
     * @param name      the name of the ShoppingList to create.
     * @param key       the key of the ShoppingList to create.
     * @param lineItems the list of LineItemDraft which ShoppingList contains.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingListWithLineItems(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final String name,
        @Nonnull final String key,
        @Nonnull final List<LineItemDraft> lineItems) {

        final ShoppingListDraft shoppingListDraft = ShoppingListDraftBuilder.of(LocalizedString.ofEnglish(name))
                                                                            .key(key)
                                                                            .lineItems(lineItems)
                                                                            .build();

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }

    public static ImmutablePair<ShoppingList, ShoppingListDraft> createSampleShoppingListCarrotCake(
        @Nonnull final SphereClient ctpClient) {

        createIngredientProducts(ctpClient);

        final ShoppingListDraft shoppingListDraft =
            ShoppingListDraftBuilder
                .of(LocalizedString.ofEnglish("Carrot Cake"))
                .key("shopping-list-key")
                .slug(LocalizedString.ofEnglish("carrot-cake"))
                .description(LocalizedString.ofEnglish("Carrot cake recipe - ingredients"))
                .anonymousId("public-carrot-cake-shopping-list")
                .deleteDaysAfterLastModification(30)
                .custom(createSampleTypes(ctpClient))
                .lineItems(buildIngredientsLineItemDrafts())
                .build();

        final ShoppingList shoppingList = createShoppingList(ctpClient, shoppingListDraft);
        return ImmutablePair.of(shoppingList, shoppingListDraft);
    }

    @Nonnull
    private static CustomFieldsDraft createSampleTypes(@Nonnull SphereClient ctpClient) {
        final TypeDraft shoppingListTypeDraft = TypeDraftBuilder
            .of("custom-type-shopping-list", LocalizedString.ofEnglish("name"),
                ResourceTypeIdsSetBuilder.of()
                                         .add(ShoppingList.resourceTypeId()))
            .fieldDefinitions(Arrays.asList(
                FieldDefinition.of(StringFieldType.of(), "nutrition",
                    LocalizedString.ofEnglish("nutrition per serving"), false),
                FieldDefinition.of(NumberFieldType.of(), "servings",
                    LocalizedString.ofEnglish("servings"), false)))
            .build();

        final TypeDraft lineItemTypeDraft = TypeDraftBuilder
            .of("custom-type-line-items", LocalizedString.ofEnglish("name"),
                ResourceTypeIdsSetBuilder.of()
                                         .addLineItems())
            .fieldDefinitions(Arrays.asList(
                FieldDefinition.of(StringFieldType.of(), "ingredient",
                    LocalizedString.ofEnglish("ingredient"), false),
                FieldDefinition.of(StringFieldType.of(), "amount",
                    LocalizedString.ofEnglish("amount"), false)))
            .build();

        CompletableFuture
            .allOf(
                ctpClient.execute(TypeCreateCommand.of(shoppingListTypeDraft)).toCompletableFuture(),
                ctpClient.execute(TypeCreateCommand.of(lineItemTypeDraft)).toCompletableFuture())
            .join();

        final Map<String, JsonNode> servingsFields = new HashMap<>();
        servingsFields.put("nutrition",
            JsonNodeFactory.instance.textNode("Per servings: 475 cal, 11g protein, 28g, fat, 44g carb"));
        servingsFields.put("servings", JsonNodeFactory.instance.numberNode(12));
        // todo: explain recipe steps with text line items

        return CustomFieldsDraft.ofTypeKeyAndJson(shoppingListTypeDraft.getKey(),
            servingsFields);
    }

    @Nonnull
    private static void createIngredientProducts(@Nonnull SphereClient ctpClient) {
        final ProductType productType = ctpClient
            .execute(
                ProductTypeCreateCommand.of(ProductTypeDraft.ofAttributeDefinitionDrafts(
                "productTypeKey",
                "productTypeName",
                "desc", null)))
            .toCompletableFuture().join();

        final ProductDraft productDraft = ProductDraftBuilder
            .of(productType,
                LocalizedString.ofEnglish("product1"),
                LocalizedString.ofEnglish("product1"),
                ProductVariantDraftBuilder.of().sku("SKU-1").build())
            .variants(asList(
                ProductVariantDraftBuilder.of().sku("SKU-2").build(),
                ProductVariantDraftBuilder.of().sku("SKU-3").build()))
            .publish(true)
            .build();

        final ProductDraft productDraft2 = ProductDraftBuilder
            .of(productType,
                LocalizedString.ofEnglish("product2"),
                LocalizedString.ofEnglish("product2"),
                ProductVariantDraftBuilder.of().sku("SKU-4").build())
            .variants(asList(
                ProductVariantDraftBuilder.of().sku("SKU-5").build(),
                ProductVariantDraftBuilder.of().sku("SKU-6").build()))
            .publish(true)
            .build();

        CompletableFuture
            .allOf(
                ctpClient.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture(),
                ctpClient.execute(ProductCreateCommand.of(productDraft2)).toCompletableFuture())
            .join();
    }

    @Nonnull
    private static List<LineItemDraft> buildIngredientsLineItemDrafts() {

        final LineItemDraft item1 = LineItemDraftBuilder
            .ofSku("SKU-1", 1L)
            .custom(buildIngredientCustomType("carrots", "280g"))
            .build();

        final LineItemDraft item2 = LineItemDraftBuilder
            .ofSku("SKU-2", 7L)
            .custom(buildIngredientCustomType("eggs", "7"))
            .build();

        final LineItemDraft item3 = LineItemDraftBuilder
            .ofSku("SKU-3", 1L)
            .custom(buildIngredientCustomType("sugar", "100g"))
            .build();

        final LineItemDraft item4 = LineItemDraftBuilder
            .ofSku("SKU-4", 1L)
            .custom(buildIngredientCustomType("flour", "70g"))
            .build();

        final LineItemDraft item5 = LineItemDraftBuilder
            .ofSku("SKU-5", 1L)
            .custom(buildIngredientCustomType("baking powder", "1 tsp"))
            .build();

        final LineItemDraft item6 = LineItemDraftBuilder
            .ofSku("SKU-6", 1L)
            .custom(buildIngredientCustomType("cinnamon", "2 tsp"))
            .build();

        return Arrays.asList(item1, item2, item3, item4, item5, item6);
    }

    @Nonnull
    private static CustomFieldsDraft buildIngredientCustomType(
        @Nonnull final String ingredient,
        @Nonnull final String amount) {

        final Map<String, JsonNode> map = new HashMap<>();
        map.put("ingredient", JsonNodeFactory.instance.textNode(ingredient));
        map.put("amount", JsonNodeFactory.instance.textNode(amount));

        return CustomFieldsDraft.ofTypeKeyAndJson("custom-type-line-items", map);
    }


    /**
     * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient         defines the CTP project to create the ShoppingList in.
     * @param shoppingListDraft the draft of the shopping list to create.
     * @return the created ShoppingList.
     */
    public static ShoppingList createShoppingList(@Nonnull final SphereClient ctpClient,
                                                  @Nonnull final ShoppingListDraft shoppingListDraft) {

        return executeBlocking(ctpClient.execute(ShoppingListCreateCommand.of(shoppingListDraft)));
    }


    private ShoppingListITUtils() {
    }
}
