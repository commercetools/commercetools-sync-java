package com.commercetools.sync.producttypes;

import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductTypeWithNestedAttributeSyncTest {

    public static final String PRODUCT_TYPE_KEY_1 = "key_1";
    public static final String PRODUCT_TYPE_KEY_4 = "key_4";

    public static final String PRODUCT_TYPE_NAME_1 = "name_1";
    public static final String PRODUCT_TYPE_NAME_4 = "name_4";

    public static final String PRODUCT_TYPE_DESCRIPTION_1 = "description_1";
    public static final String PRODUCT_TYPE_DESCRIPTION_4 = "description_4";

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_1 =
            AttributeDefinitionDraftBuilder
                    .of(StringAttributeType.of(), "attr_name_1",
                            LocalizedString.ofEnglish("attr_label_1"), true)
                    .inputTip(LocalizedString.ofEnglish("inputTip1"))
                    .build();

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_2 =
            AttributeDefinitionDraftBuilder
                    .of(StringAttributeType.of(), "attr_name_2",
                            LocalizedString.ofEnglish("attr_label_2"), true)
                    .inputTip(LocalizedString.ofEnglish("inputTip2"))
                    .build();

    public static final AttributeDefinitionDraft ATTRIBUTE_DEFINITION_DRAFT_3 =
            AttributeDefinitionDraftBuilder
                    .of(StringAttributeType.of(), "attr_name_3",
                            LocalizedString.ofEnglish("attr_label_3"), true)
                    .inputTip(LocalizedString.ofEnglish("inputTip3"))
                    .build();

    @Test
    void sync_WithNewProductTypeWithFailedFetchOnReferenceResolution_ShouldFail() {
        // preparation
        final ProductTypeSyncOptions productTypeSyncOptions;
        final List<UpdateAction<ProductType>> builtUpdateActions = new ArrayList<>();
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final AttributeDefinitionDraft nestedTypeAttr = AttributeDefinitionDraftBuilder
                .of(AttributeDefinitionBuilder
                        .of("nestedattr", ofEnglish("nestedattr"),
                                NestedAttributeType.of(ProductType.referenceOfId(PRODUCT_TYPE_KEY_4)))
                        .build())
                // isSearchable=true is not supported for attribute type 'nested' and AttributeDefinitionBuilder sets
                // it to true by default
                .searchable(false)
                .build();

        final ProductTypeDraft withMissingNestedTypeRef = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_1,
                PRODUCT_TYPE_NAME_1,
                PRODUCT_TYPE_DESCRIPTION_1,
                asList(ATTRIBUTE_DEFINITION_DRAFT_1, ATTRIBUTE_DEFINITION_DRAFT_2, nestedTypeAttr));

        final ProductTypeDraft productTypeDraft4 = ProductTypeDraft.ofAttributeDefinitionDrafts(
                PRODUCT_TYPE_KEY_4,
                PRODUCT_TYPE_NAME_4,
                PRODUCT_TYPE_DESCRIPTION_4,
                singletonList(ATTRIBUTE_DEFINITION_DRAFT_3));

        ProductTypeService service = buildMockProductTypeServiceWithFailedFetchOnReferenceResolution();

        productTypeSyncOptions = ProductTypeSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .batchSize(1) // this ensures the drafts are in separate batches.
                .beforeUpdateCallback((actions, draft, oldProductType) -> {
                    builtUpdateActions.addAll(actions);
                    return actions;
                })
                .errorCallback((exception, oldResource, newResource, actions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception);
                })
                .build();


        final ProductTypeSync productTypeSync = new ProductTypeSync(productTypeSyncOptions, service);

        // tests
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
                .sync(asList(withMissingNestedTypeRef, productTypeDraft4))
                .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages).containsExactly("Failed to fetch existing product types with keys: '[key_1]'.");
        assertThat(exceptions).singleElement().satisfies(exception ->
                assertThat(exception.getCause()).hasCauseExactlyInstanceOf(BadGatewayException.class));
        assertThat(builtUpdateActions).isEmpty();
        assertThat(productTypeSyncStatistics).hasValues(2, 1, 0, 0, 1);
        assertThat(productTypeSyncStatistics
                .getReportMessage())
                .isEqualTo("Summary: 2 product types were processed in total"
                        + " (1 created, 0 updated, 0 failed to sync and 1 product types with at least one NestedType or"
                        + " a Set of NestedType attribute definition(s) referencing a missing product type).");

        assertThat(productTypeSyncStatistics.getProductTypeKeysWithMissingParents()).hasSize(1);
        final ConcurrentHashMap<String, ConcurrentHashMap.KeySetView<AttributeDefinitionDraft, Boolean>>
                children = productTypeSyncStatistics
                .getProductTypeKeysWithMissingParents().get(PRODUCT_TYPE_KEY_4);

        assertThat(children).hasSize(1);
        assertThat(children.get(PRODUCT_TYPE_KEY_1)).containsExactly(nestedTypeAttr);
    }

    @Nonnull
    private AttributeDefinition buildMockAttributeDefinition(
            final String name,
            final LocalizedString label,
            final LocalizedString inputTip) {
        final AttributeDefinition mockAttributeDefinition = mock(AttributeDefinition.class);
        when(mockAttributeDefinition.getAttributeType()).thenReturn(StringAttributeType.of());
        when(mockAttributeDefinition.getName()).thenReturn(name);
        when(mockAttributeDefinition.getLabel()).thenReturn(label);
        when(mockAttributeDefinition.isRequired()).thenReturn(true);
        when(mockAttributeDefinition.getInputTip()).thenReturn(inputTip);
        when(mockAttributeDefinition.getInputHint()).thenReturn(TextInputHint.SINGLE_LINE);
        when(mockAttributeDefinition.getAttributeConstraint()).thenReturn(AttributeConstraint.NONE);
        when(mockAttributeDefinition.isSearchable()).thenReturn(true);

        return mockAttributeDefinition;
    }

    @Nonnull
    private ProductTypeService buildMockProductTypeServiceWithFailedFetchOnReferenceResolution() {
        final ProductTypeService service = mock(ProductTypeService.class);
        final ProductType mockProductType1 = mock(ProductType.class);
        final ProductType mockProductType4 = mock(ProductType.class);
        final AttributeDefinition attributeDefinition1 =
                buildMockAttributeDefinition("attr_name_1",
                        LocalizedString.ofEnglish("attr_label_1"),
                        LocalizedString.ofEnglish("inputTip1"));

        final AttributeDefinition attributeDefinition2 =
                buildMockAttributeDefinition("attr_name_2",
                        LocalizedString.ofEnglish("attr_label_2"),
                        LocalizedString.ofEnglish("inputTip2"));

        final AttributeDefinition attributeDefinition3 =
                buildMockAttributeDefinition("attr_name_3",
                        LocalizedString.ofEnglish("attr_label_3"),
                        LocalizedString.ofEnglish("inputTip3"));

        final List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(attributeDefinition1);
        attributeDefinitions.add(attributeDefinition2);

        when(mockProductType1.getKey()).thenReturn(PRODUCT_TYPE_KEY_1);
        when(mockProductType1.getId()).thenReturn(UUID.randomUUID().toString());
        when(mockProductType1.getName()).thenReturn(PRODUCT_TYPE_NAME_1);
        when(mockProductType1.getDescription()).thenReturn(PRODUCT_TYPE_DESCRIPTION_1);
        when(mockProductType1.getAttributes()).thenReturn(attributeDefinitions);

        when(mockProductType4.getKey()).thenReturn(PRODUCT_TYPE_KEY_4);
        when(mockProductType4.getName()).thenReturn(PRODUCT_TYPE_NAME_4);
        when(mockProductType4.getDescription()).thenReturn(PRODUCT_TYPE_DESCRIPTION_4);
        when(mockProductType4.getAttributes()).thenReturn(singletonList(attributeDefinition3));

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(mockProductType1.getKey(), UUID.randomUUID().toString());
        when(service.cacheKeysToIds(anySet()))
                .thenReturn(completedFuture(emptyMap()))
                .thenReturn(completedFuture(keyToIds));

        when(service.fetchMatchingProductTypesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockProductType1)))
                .thenReturn(completedFuture(emptySet()))
                .thenReturn(completedFuture(emptySet()))
                .thenReturn(exceptionallyCompletedFuture(new CompletionException(new BadGatewayException())));

        when(service.createProductType(any())).thenReturn(completedFuture(Optional.of(mockProductType4)));

        return service;
    }

}
