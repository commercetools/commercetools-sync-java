package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroup;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroupService;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PriceCustomerGroupReferenceResolverTest {
    private static final String CUSTOMER_GROUP_KEY = "customer-group-key_1";
    private static final String CUSTOMER_GROUP_ID = "1";

    private CustomerGroupService customerGroupService;
    private PriceReferenceResolver referenceResolver;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        customerGroupService = getMockCustomerGroupService(getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));
        ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
        referenceResolver = new PriceReferenceResolver(syncOptions, mock(TypeService.class), mock(ChannelService.class),
            customerGroupService);
    }

    @Test
    public void resolveCustomerGroupReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();

        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId(UUID.randomUUID().toString()));

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(productSyncOptions, mock(TypeService.class), mock(ChannelService.class),
                customerGroupService);


        final PriceDraftBuilder resolvedDraft = priceReferenceResolver.resolveCustomerGroupReference(priceBuilder)
                                                                      .toCompletableFuture().join();

        assertThat(resolvedDraft.getCustomerGroup()).isNotNull();
        assertThat(resolvedDraft.getCustomerGroup().getId()).isEqualTo(CUSTOMER_GROUP_ID);
    }

    @Test
    public void resolveCustomerGroupReference_WithKeys_ShouldResolveReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId("anyKey"));

        final PriceDraftBuilder resolvedDraft = referenceResolver.resolveCustomerGroupReference(priceBuilder)
                                                                 .toCompletableFuture().join();

        assertThat(resolvedDraft.getCustomerGroup()).isNotNull();
        assertThat(resolvedDraft.getCustomerGroup().getId()).isEqualTo(CUSTOMER_GROUP_ID);
    }

    @Test
    public void resolveCustomerGroupReference_WithKeysAsUuidSetAndNotAllowed_ShouldNotResolveReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId(UUID.randomUUID().toString()));

        assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve 'customer-group' reference on PriceDraft"
                    + " with country:'%s' and value: '%s'. Reason: Found a UUID in the id field. Expecting a key"
                    + " without a UUID value. If you want to allow UUID values for reference keys, please use the"
                    + " allowUuidKeys(true) option in the sync options.",
                priceBuilder.getCountry(), priceBuilder.getValue()));
    }

    @Test
    public void resolveCustomerGroupReference_WithNullCustomerGroup_ShouldNotResolveReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR));

        assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> isNull(resolvedDraft.getCustomerGroup()));
    }

    @Test
    public void resolveCustomerGroupReference_WithNonExistentCustomerGroup_ShouldNotResolveReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId("nonExistentKey"));

        when(customerGroupService.fetchCachedCustomerGroupId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft -> nonNull(resolvedDraft.getCustomerGroup())
                    && Objects.equals(resolvedDraft.getCustomerGroup().getId(), "nonExistentKey"));
    }

    @Test
    public void resolveCustomerGroupReference_WithNullIdOnCustomerGroupReference_ShouldNotResolveReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(Reference.of(CustomerGroup.referenceTypeId(), (String)null));

        assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve 'customer-group' reference on PriceDraft with country:'%s' and"
                + " value: '%s'. Reason: %s", priceBuilder.getCountry(), priceBuilder.getValue(),
                BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCustomerGroupReference_WithEmptyIdOnCustomerGroupReference_ShouldNotResolveReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId(""));

        assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve 'customer-group' reference on PriceDraft with country:'%s' and"
                    + " value: '%s'. Reason: %s", priceBuilder.getCountry(), priceBuilder.getValue(),
                BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCustomerGroupReference_WithExceptionOnFetch_ShouldNotResolveReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .customerGroup(CustomerGroup.referenceOfId("CustomerGroupKey"));

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(customerGroupService.fetchCachedCustomerGroupId(anyString())).thenReturn(futureThrowingSphereException);

        assertThat(referenceResolver.resolveCustomerGroupReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }
}
