package com.commercetools.sync.integration.services;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

public class ProductFetchTestIT {
    @Test
    public void fetchAllProducts() {
        final String projectKey = CTP_TARGET_CLIENT.getConfig().getProjectKey();

        final CompletionStage<List<List<Product>>> listCompletionStage = CtpQueryUtils
            .queryAll(CTP_TARGET_CLIENT, ProductQuery.of(), products -> products);

        final List<List<Product>> allProducts = listCompletionStage.toCompletableFuture().join();
        final List<Product> productList = allProducts.stream()
                                                     .flatMap(Collection::stream).collect(Collectors.toList());


        final int numberOfProducts = productList.size();
        System.out.println("Number of products:" +  numberOfProducts);
    }

}
