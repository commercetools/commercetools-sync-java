package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.DefaultCurrencyUnits;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroup;
import static com.commercetools.sync.products.ProductSyncMockUtils.getMockCustomerGroupService;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PriceReferenceResolverTest {
    private TypeService typeService;
    private ChannelService channelService;
    private CustomerGroupService customerGroupService;
    private ProductSyncOptions syncOptions;

    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";

    private static final String CUSTOMER_GROUP_KEY = "customer-group-key_1";
    private static final String CUSTOMER_GROUP_ID = "1";

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        channelService = getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
        customerGroupService = getMockCustomerGroupService(getMockCustomerGroup(CUSTOMER_GROUP_ID, CUSTOMER_GROUP_KEY));
        syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveCustomTypeReference_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson(customTypeKey, new HashMap<>());
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(customFieldsDraft);

        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasNotFailed()
            .isCompletedWithValueMatching(resolvedDraft ->
                Objects.nonNull(resolvedDraft.getCustom())
                    && Objects.nonNull(resolvedDraft.getCustom().getType())
                    && Objects.equals(resolvedDraft.getCustom().getType().getId(), customTypeKey));
    }

    @Test
    public void resolveCustomTypeReference_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> typeReference = ResourceIdentifier.ofId(null);
        when(customFieldsDraft.getType()).thenReturn(typeReference);

        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(customFieldsDraft);

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on PriceDraft"
                + " with country:'DE' and value: 'EUR 10'. Reason: %s", BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson("", new HashMap<>());
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(customFieldsDraft);

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage(format("Failed to resolve custom type reference on PriceDraft"
                + " with country:'DE' and value: 'EUR 10'. Reason: %s", BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    }

    @Test
    public void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        final String customTypeKey = "customTypeKey";
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson(customTypeKey, new HashMap<>());
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(customFieldsDraft);

        final CompletableFuture<Optional<String>> futureThrowingSphereException = new CompletableFuture<>();
        futureThrowingSphereException.completeExceptionally(new SphereException("CTP error on fetch"));
        when(typeService.fetchCachedTypeId(anyString())).thenReturn(futureThrowingSphereException);

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    public void resolveChannelReference_WithNonExistingChannelKey_ShouldResolveChannelReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .build();
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(Channel.referenceOfId("channelKey"));

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(productSyncOptions, typeService, channelService, customerGroupService);

        final PriceDraftBuilder resolvedBuilder = priceReferenceResolver.resolveChannelReference(priceBuilder)
                                                                        .toCompletableFuture().join();
        assertThat(resolvedBuilder.getChannel()).isNotNull();
        assertThat(resolvedBuilder.getChannel().getId()).isEqualTo(CHANNEL_ID);
    }

    @Test
    public void
        resolveSupplyChannelReference_WithNonExistingChannelAndNotEnsureChannel_ShouldNotResolveChannelReference() {
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(Channel.referenceOfId("channelKey"));

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);


        priceReferenceResolver.resolveChannelReference(priceBuilder)
                              .exceptionally(exception -> {
                                  assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                  assertThat(exception.getCause())
                                      .isExactlyInstanceOf(ReferenceResolutionException.class);
                                  assertThat(exception.getCause().getCause())
                                      .isExactlyInstanceOf(ReferenceResolutionException.class);
                                  assertThat(exception.getCause().getCause().getMessage())
                                      .isEqualTo("Channel with key 'channelKey' does not exist.");
                                  return null;
                              })
                              .toCompletableFuture()
                              .join();
    }

    @Test
    public void
        resolveSupplyChannelReference_WithNonExistingChannelAndEnsureChannel_ShouldResolveSupplyChannelReference() {
        final ProductSyncOptions optionsWithEnsureChannels = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                      .ensurePriceChannels(true)
                                                                                      .build();
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(Channel.referenceOfId("channelKey"));

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(optionsWithEnsureChannels, typeService, channelService, customerGroupService);

        priceReferenceResolver.resolveChannelReference(priceBuilder)
                              .thenApply(PriceDraftBuilder::build)
                              .thenAccept(resolvedDraft -> {
                                  assertThat(resolvedDraft.getChannel()).isNotNull();
                                  assertThat(resolvedDraft.getChannel().getId()).isEqualTo(CHANNEL_ID);
                              })
                              .toCompletableFuture()
                              .join();
    }

    @Test
    public void resolveReferences_WithNoReferences_ShouldNotResolveReferences() {
        final PriceDraft priceDraft = PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
                                                       .country(CountryCode.DE)
                                                       .build();

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService, customerGroupService);

        final PriceDraft referencesResolvedDraft = priceReferenceResolver.resolveReferences(priceDraft)
                                                                         .toCompletableFuture().join();

        assertThat(referencesResolvedDraft.getCustom()).isNull();
        assertThat(referencesResolvedDraft.getChannel()).isNull();
        assertThat(referencesResolvedDraft.getCustomerGroup()).isNull();
    }
}
