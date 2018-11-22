package com.commercetools.sync.producttypes;

import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductTypeSyncTest {

    private ProductTypeService productTypeService;

    private ProductType item;
    private List<UpdateAction<ProductType>> updateActions;

    private Set<ProductType> matching;

    @Before
    public void setup() {
        productTypeService = mock(ProductTypeService.class);

        matching = new HashSet<>();

        when(productTypeService.fetchMatchingProductTypesByKeys(any()))
            .thenReturn(completedFuture(matching));

        item = null;
        updateActions = new ArrayList<>();
    }

    @Test
    public void sync_WithNotExistingProductType_ShouldCallAfterCreateCallback() {
        ProductType mocked = mock(ProductType.class);
        ProductTypeSyncOptions options = ProductTypeSyncOptionsBuilder.of(mock(SphereClient.class))
            .afterCreateCallback(created -> item = created)
            .build();

        when(productTypeService.createProductType(any())).thenReturn(completedFuture(mocked));

        ProductTypeSync productTypeSync = new ProductTypeSync(options, productTypeService);

        productTypeSync.sync(singletonList(
            getProductTypeDraft("testKey", "createName", "createDescription",
                getAttributeDefinitionDraft(StringAttributeType.of(), "createAttrName",
                    LocalizedString.of(Locale.ENGLISH, "createAttrLabel"), false))
        ));

        assertThat(item).isNotNull();
        assertThat(updateActions).isEmpty();
    }

    @Test
    public void sync_WithExistingProductType_ShouldCallAfterUpdateCallback() {
        String key = "testKey";

        ProductType existing = mock(ProductType.class);
        when(existing.getKey()).thenReturn(key);
        when(existing.getName()).thenReturn("name");
        when(existing.getDescription()).thenReturn("description");
        when(existing.getAttributes()).thenReturn(new ArrayList<>());

        ProductTypeSyncOptions options = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .afterUpdateCallback((updated, actions) -> {
                item = updated;
                updateActions = actions;
            })
            .build();
        matching.add(existing);

        when(productTypeService.updateProductType(any(), any()))
            .thenReturn(completedFuture(existing));

        ProductTypeSync productTypeSync = new ProductTypeSync(options, productTypeService);

        productTypeSync.sync(singletonList(
            getProductTypeDraft(key, "updateName", "updateDescription",
                getAttributeDefinitionDraft(StringAttributeType.of(), "updateAttrName",
                    LocalizedString.of(Locale.ENGLISH, "updateAttrLabel"), true))
        ));

        assertThat(item).isNotNull();
        assertThat(updateActions).isNotEmpty();
    }

    private ProductTypeDraft getProductTypeDraft(final String key, final String name, final String description,
        final AttributeDefinitionDraft attribute) {
        return ProductTypeDraftBuilder.of(key, name, description, singletonList(attribute)).build();
    }

    private AttributeDefinitionDraft getAttributeDefinitionDraft(final AttributeType type,
        final String name, final LocalizedString label, final Boolean required) {
        return AttributeDefinitionDraftBuilder.of(type, name, label, required)
            .searchable(false)
            .build();
    }

}
