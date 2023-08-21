package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomers;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.TextLineItemDraft;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.FieldTypeBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ShoppingListITUtils {

  /**
   * Deletes all shopping lists, products and product types from the CTP project defined by the
   * {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete test data from.
   */
  public static void deleteShoppingListTestData(@Nonnull final ProjectApiRoot ctpClient) {
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
  public static void deleteShoppingLists(@Nonnull final ProjectApiRoot ctpClient) {
    Consumer<List<ShoppingList>> shoppingListsConsumer =
        shoppingLists -> {
          CompletableFuture.allOf(
                  shoppingLists.stream()
                      .map(shoppingList -> deleteShoppingList(ctpClient, shoppingList))
                      .map(CompletionStage::toCompletableFuture)
                      .toArray(CompletableFuture[]::new))
              .join();
        };
    QueryUtils.queryAll(ctpClient.shoppingLists().get(), shoppingListsConsumer)
        .handle(
            (result, throwable) -> {
              if (throwable != null && !(throwable instanceof NotFoundException)) {
                return throwable;
              }
              return result;
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<ShoppingList> deleteShoppingList(
      final ProjectApiRoot ctpClient, final ShoppingList shoppingList) {
    return ctpClient
        .shoppingLists()
        .delete(shoppingList)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }

  /**
   * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a
   * blocking fashion.
   *
   * @param ctpClient defines the CTP project to create the ShoppingList in.
   * @param name the name of the ShoppingList to create.
   * @param key the key of the ShoppingList to create.
   * @return the created ShoppingList.
   */
  public static ShoppingList createShoppingList(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String name,
      @Nonnull final String key) {

    return createShoppingList(ctpClient, name, key, null, null, null, null);
  }

  /**
   * Creates a {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a
   * blocking fashion.
   *
   * @param ctpClient defines the CTP project to create the ShoppingList in.
   * @param name the name of the ShoppingList to create.
   * @param key the key of the ShoppingList to create.
   * @param desc the description of the ShoppingList to create.
   * @param anonymousId the anonymous ID of the ShoppingList to create.
   * @param slug the slug of the ShoppingList to create.
   * @param deleteDaysAfterLastModification the deleteDaysAfterLastModification of the ShoppingList
   *     to create.
   * @return the created ShoppingList.
   */
  public static ShoppingList createShoppingList(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String name,
      @Nonnull final String key,
      @Nullable final String desc,
      @Nullable final String anonymousId,
      @Nullable final String slug,
      @Nullable final Long deleteDaysAfterLastModification) {

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish(name))
            .key(key)
            .description(desc == null ? null : ofEnglish(desc))
            .anonymousId(anonymousId)
            .slug(slug == null ? null : ofEnglish(slug))
            .deleteDaysAfterLastModification(deleteDaysAfterLastModification)
            .build();

    return ctpClient
        .shoppingLists()
        .create(shoppingListDraft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join();
  }

  /**
   * Creates a sample {@link ShoppingList} in the CTP project defined by the {@code ctpClient} in a
   * blocking fashion.
   *
   * @param ctpClient defines the CTP project to create the ShoppingList in.
   * @return the created ShoppingList.
   */
  @Nonnull
  public static ImmutablePair<ShoppingList, ShoppingListDraft> createSampleShoppingListCarrotCake(
      @Nonnull final ProjectApiRoot ctpClient) {

    createIngredientProducts(ctpClient);

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("Carrot Cake"))
            .key("shopping-list-key")
            .slug(ofEnglish("carrot-cake"))
            .description(ofEnglish("Carrot cake recipe - ingredients"))
            .anonymousId("public-carrot-cake-shopping-list")
            .deleteDaysAfterLastModification(30L)
            .custom(createSampleTypes(ctpClient))
            .lineItems(buildIngredientsLineItemDrafts())
            .textLineItems(buildRecipeTextLineItemDrafts())
            .build();

    final ShoppingList shoppingList =
        ctpClient
            .shoppingLists()
            .create(shoppingListDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();
    return ImmutablePair.of(shoppingList, shoppingListDraft);
  }

  @Nonnull
  private static CustomFieldsDraft createSampleTypes(@Nonnull final ProjectApiRoot ctpClient) {
    final TypeDraft shoppingListTypeDraft =
        TypeDraftBuilder.of()
            .name(ofEnglish("custom-type-shopping-list"))
            .key("custom-type-shopping-list")
            .name(ofEnglish("name"))
            .resourceTypeIds(ResourceTypeId.SHOPPING_LIST)
            .fieldDefinitions(
                List.of(
                    FieldDefinitionBuilder.of()
                        .type(FieldTypeBuilder::stringBuilder)
                        .name("nutrition")
                        .label(ofEnglish("nutrition per serving"))
                        .required(false)
                        .build(),
                    FieldDefinitionBuilder.of()
                        .type(FieldTypeBuilder::numberBuilder)
                        .name("servings")
                        .label(ofEnglish("servings"))
                        .required(false)
                        .build()))
            .build();

    final TypeDraft lineItemTypeDraft =
        TypeDraftBuilder.of()
            .key("custom-type-line-items")
            .name(ofEnglish("name"))
            .resourceTypeIds(ResourceTypeId.LINE_ITEM)
            .fieldDefinitions(
                List.of(
                    FieldDefinitionBuilder.of()
                        .type(FieldTypeBuilder::stringBuilder)
                        .name("ingredient")
                        .label(ofEnglish("ingredient"))
                        .required(false)
                        .build(),
                    FieldDefinitionBuilder.of()
                        .type(FieldTypeBuilder::stringBuilder)
                        .name("amount")
                        .label(ofEnglish("amount"))
                        .required(false)
                        .build()))
            .build();

    final TypeDraft textLineItemTypeDraft =
        TypeDraftBuilder.of()
            .name(ofEnglish("custom-type-text-line-items"))
            .key("custom-type-text-line-items")
            .description(ofEnglish("description"))
            .resourceTypeIds(ResourceTypeId.SHOPPING_LIST_TEXT_LINE_ITEM)
            .fieldDefinitions(
                FieldDefinitionBuilder.of()
                    .type(FieldTypeBuilder::stringBuilder)
                    .name("utensils")
                    .label(ofEnglish("utensils"))
                    .required(false)
                    .build())
            .build();

    CompletableFuture.allOf(
            ctpClient.types().create(shoppingListTypeDraft).execute(),
            ctpClient.types().create(lineItemTypeDraft).execute(),
            ctpClient.types().create(textLineItemTypeDraft).execute())
        .join();

    final Map<String, Object> servingsFields = new HashMap<>();
    servingsFields.put("nutrition", "Per servings: 475 cal, 11g protein, 28g, fat, 44g carb");

    servingsFields.put("servings", 12L);

    return CustomFieldsDraftBuilder.of()
        .type(
            typeResourceIdentifierBuilder ->
                typeResourceIdentifierBuilder.key(shoppingListTypeDraft.getKey()))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(servingsFields))
        .build();
  }

  private static void createIngredientProducts(@Nonnull final ProjectApiRoot ctpClient) {
    final ProductType productType =
        ctpClient
            .productTypes()
            .create(
                ProductTypeDraftBuilder.of()
                    .key("productTypeKey")
                    .name("productTypeName")
                    .description("desc")
                    .build())
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .slug(ofEnglish("product1"))
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("product1"))
            .description(ofEnglish("product1"))
            .masterVariant(ProductVariantDraftBuilder.of().sku("SKU-1").key("variant1").build())
            .key("product-1-sample-carrot-cake")
            .variants(
                List.of(
                    ProductVariantDraftBuilder.of().sku("SKU-2").key("variant2").build(),
                    ProductVariantDraftBuilder.of().sku("SKU-3").key("variant3").build()))
            .publish(true)
            .build();

    final ProductDraft productDraft2 =
        ProductDraftBuilder.of()
            .slug(ofEnglish("product2"))
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("product2"))
            .description(ofEnglish("product2"))
            .masterVariant(ProductVariantDraftBuilder.of().sku("SKU-4").key("variant4").build())
            .key("product-2-sample-carrot-cake")
            .variants(
                List.of(
                    ProductVariantDraftBuilder.of().sku("SKU-5").key("variant5").build(),
                    ProductVariantDraftBuilder.of().sku("SKU-6").key("variant6").build()))
            .publish(true)
            .build();

    CompletableFuture.allOf(
            ctpClient.products().create(productDraft).execute(),
            ctpClient.products().create(productDraft2).execute())
        .join();
  }

  @Nonnull
  private static List<ShoppingListLineItemDraft> buildIngredientsLineItemDrafts() {

    final ShoppingListLineItemDraft item1 =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-1")
            .quantity(1L)
            .custom(buildIngredientCustomType("carrots", "280g"))
            .build();

    final ShoppingListLineItemDraft item2 =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-2")
            .quantity(7L)
            .custom(buildIngredientCustomType("eggs", "7"))
            .build();

    final ShoppingListLineItemDraft item3 =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-3")
            .quantity(1L)
            .custom(buildIngredientCustomType("sugar", "100g"))
            .build();

    final ShoppingListLineItemDraft item4 =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-4")
            .quantity(1L)
            .custom(buildIngredientCustomType("flour", "70g"))
            .build();

    final ShoppingListLineItemDraft item5 =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-5")
            .quantity(1L)
            .custom(buildIngredientCustomType("baking powder", "1 tsp"))
            .build();

    final ShoppingListLineItemDraft item6 =
        ShoppingListLineItemDraftBuilder.of()
            .sku("SKU-6")
            .quantity(1L)
            .custom(buildIngredientCustomType("cinnamon", "2 tsp"))
            .build();

    return List.of(item1, item2, item3, item4, item5, item6);
  }

  /**
   * Creates an instance of {@link CustomFieldsDraft} with the type key 'custom-type-line-items' and
   * two custom fields 'ingredient' and'amount'.
   *
   * @param ingredient the text field.
   * @param amount the text field.
   * @return an instance of {@link CustomFieldsDraft} with the type key 'custom-type-line-items' and
   *     two custom fields 'ingredient' and'amount'.
   */
  @Nonnull
  public static CustomFieldsDraft buildIngredientCustomType(
      @Nonnull final String ingredient, @Nonnull final String amount) {

    final Map<String, Object> map = new HashMap<>();
    map.put("ingredient", ingredient);
    map.put("amount", amount);

    return CustomFieldsDraftBuilder.of()
        .type(
            typeResourceIdentifierBuilder ->
                typeResourceIdentifierBuilder.key("custom-type-line-items"))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(map))
        .build();
  }

  @Nonnull
  private static List<TextLineItemDraft> buildRecipeTextLineItemDrafts() {

    final TextLineItemDraft item1 =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("step 1"))
            .quantity(1L)
            .description(ofEnglish("Peel carrots and set aside, separate eggs into small balls."))
            .custom(buildUtensilsCustomType("Peeler, 2 small bowls"))
            .build();

    final TextLineItemDraft item2 =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("step 2"))
            .quantity(1L)
            .description(
                ofEnglish(
                    "Mix powder and baking powder in a large bowl set aside, "
                        + "Blend slowly egg yolks and cinnamon until smooth."))
            .custom(buildUtensilsCustomType("2 large bowls, hand mixer"))
            .build();

    final TextLineItemDraft item3 =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("step 3"))
            .quantity(1L)
            .description(ofEnglish("Mix egg whites and sugar until stiff."))
            .custom(buildUtensilsCustomType("1 large bowl"))
            .build();

    final TextLineItemDraft item4 =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("step 4"))
            .quantity(1L)
            .description(
                ofEnglish(
                    "Transfer egg whites into other egg mixture, combine with powder, "
                        + "add peeled carrots, stir with spatula."))
            .custom(buildUtensilsCustomType("Rubber spatula"))
            .build();

    final TextLineItemDraft item5 =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("step 5"))
            .quantity(1L)
            .description(
                ofEnglish("Put cake mixture into cake pan, bake appr 40 min with 180 C degree"))
            .custom(buildUtensilsCustomType("Cake pan, oven"))
            .build();

    final TextLineItemDraft item6 =
        TextLineItemDraftBuilder.of()
            .name(ofEnglish("step 6"))
            .quantity(1L)
            .description(ofEnglish("Decorate as you wish and serve, enjoy!"))
            .custom(buildUtensilsCustomType("Knife, cake plate."))
            .addedAt(ZonedDateTime.parse("2020-11-06T10:00:00.000Z"))
            .build();

    return List.of(item1, item2, item3, item4, item5, item6);
  }

  /**
   * Creates an instance of {@link CustomFieldsDraft} with the type key
   * 'custom-type-text-line-items' and two custom fields 'utensil'.
   *
   * @param utensils the text field.
   * @return an instance of {@link CustomFieldsDraft} with the type key
   *     'custom-type-text-line-items' and two custom fields 'utensils'.
   */
  @Nonnull
  public static CustomFieldsDraft buildUtensilsCustomType(@Nonnull final String utensils) {

    final Map<String, Object> map = new HashMap<>();
    map.put("utensils", utensils);

    return CustomFieldsDraftBuilder.of()
        .type(
            typeResourceIdentifierBuilder ->
                typeResourceIdentifierBuilder.key("custom-type-text-line-items"))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(map))
        .build();
  }

  private ShoppingListITUtils() {}
}
