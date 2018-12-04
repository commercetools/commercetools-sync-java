package com.commercetools.sync.services;

import com.commercetools.sync.products.AttributeMetaData;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface ProductTypeService {
    /**
     * Given a {@code key}, this method first checks if a cached map of ProductType keys -&gt; ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all ProductType keys to ids
     * in the CTP project, by querying the CTP project for all ProductTypes.
     *
     * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link ProductType} was
     * found in the CTP project with this key.
     *
     * @param key the key by which a {@link ProductType} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link ProductType} was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull final String key);

    /**
     * TODO FIX JAVADOC AND TEST METHOD
     * Given a {@code productType}, this method first checks if a cached map of ProductType ids -&gt; map of
     * {@link AttributeMetaData} is not empty. If not, it returns a completed future that contains an optional that
     * contains what this productType id maps to in the cache. If the cache is empty,
     * the method populates the cache with the mapping of all ProductType ids to maps of each product type's attributes'
     * {@link AttributeMetaData} in the CTP project, by querying the CTP project for all ProductTypes.
     *
     * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with a map of the attributes names -&gt; {@link AttributeMetaData} inside of it or an empty
     * {@link Optional} if no {@link ProductType} was found in the CTP project with this id.
     *
     * @param productTypeId the id by which a a map of the attributes names -&gt; {@link AttributeMetaData}
     *                      corresponding to the product type should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link ProductType} was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<Map<String, AttributeMetaData>>> fetchCachedProductAttributeMetaDataMap(
            @Nonnull final String productTypeId);

    /**
     * Given a {@link Set} of ProductType keys, this method fetches a set of all the ProductTypes, matching this given
     * set of keys in the CTP project, defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}. A
     * mapping of the key to the id of the fetched ProductType is persisted in an in-memory map.
     *
     * @param keys set of ProductType keys to fetch matching ProductTypes by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all matching ProductType.
     */
    @Nonnull
    CompletionStage<Set<ProductType>> fetchMatchingProductTypesByKeys(@Nonnull final Set<String> keys);

    /**
     * Creates new product type from {@code productTypeDraft}.
     *
     * @param productTypeDraft draft with data for new product type
     * @return {@link CompletionStage} with created {@link ProductType}.
     */
    @Nonnull
    CompletionStage<ProductType> createProductType(@Nonnull final ProductTypeDraft productTypeDraft);

    /**
     * Updates existing product type with {@code updateActions}.
     *
     * @param productType   product type that should be updated
     * @param updateActions {@link List} of actions that should be applied to {@code productType}
     * @return {@link CompletionStage} with updated {@link ProductType}.
     */
    @Nonnull
    CompletionStage<ProductType> updateProductType(@Nonnull final ProductType productType,
                                                   @Nonnull final List<UpdateAction<ProductType>> updateActions);
}
