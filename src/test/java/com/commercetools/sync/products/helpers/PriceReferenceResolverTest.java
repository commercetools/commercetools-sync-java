package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PriceReferenceResolverTest {
    private TypeService typeService;
    private ChannelService channelService;
    private ProductSyncOptions syncOptions;

    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String CHANNEL_ID = "1";

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        channelService = getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
        syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveCustomTypeReference_WithKeysAsUuidSetAndAllowed_ShouldResolveReferences() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson(UUID.randomUUID().toString(), new HashMap<>());

        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .custom(customFieldsDraft);

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(productSyncOptions, typeService, channelService);


        final PriceDraftBuilder resolvedDraft = priceReferenceResolver.resolveCustomTypeReference(priceBuilder)
                                                                             .toCompletableFuture().join();

        assertThat(resolvedDraft.getCustom()).isNotNull();
        assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveCustomTypeReference_WithKeyAsUuidSetAndNotAllowed_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft
            .ofTypeIdAndJson(UUID.randomUUID().toString(), new HashMap<>());

        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(customFieldsDraft);

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on PriceDraft"
                + " with country:'DE' and value: 'EUR 10'. Reason: Found a UUID"
                + " in the id field. Expecting a key without a UUID value. If you want to"
                + " allow UUID values for reference keys, please use the "
                + "allowUuidKeys(true) option in the sync options.");
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
            new PriceReferenceResolver(syncOptions, typeService, channelService);

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
            new PriceReferenceResolver(syncOptions, typeService, channelService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on PriceDraft"
                + " with country:'DE' and value: 'EUR 10'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
    }

    @Test
    public void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final CustomFieldsDraft customFieldsDraft = CustomFieldsDraft.ofTypeIdAndJson("", new HashMap<>());
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .custom(customFieldsDraft);

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference on PriceDraft"
                + " with country:'DE' and value: 'EUR 10'. Reason: Reference 'id' field"
                + " value is blank (null/empty).");
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
            new PriceReferenceResolver(syncOptions, typeService, channelService);

        assertThat(priceReferenceResolver.resolveCustomTypeReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(SphereException.class)
            .hasMessageContaining("CTP error on fetch");
    }

    @Test
    public void resolveChannelReference_WithChannelKeyAsUuidSetAndAllowed_ShouldResolveChannelReference() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .allowUuidKeys(true)
                                                                               .build();
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(Channel.referenceOfId(UUID.randomUUID().toString()));

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(productSyncOptions, typeService, channelService);

        final PriceDraftBuilder resolvedBuilder = priceReferenceResolver.resolveChannelReference(priceBuilder)
                                                               .toCompletableFuture().join();
        assertThat(resolvedBuilder.getChannel()).isNotNull();
        assertThat(resolvedBuilder.getChannel().getId()).isEqualTo(CHANNEL_ID);
    }

    @Test
    public void resolveChannelReference_WithChannelKeyAsUuidSetAndNotAllowed_ShouldNotResolveChannelReference() {
        final PriceDraftBuilder priceBuilder = PriceDraftBuilder
            .of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
            .country(CountryCode.DE)
            .channel(Channel.referenceOfId(UUID.randomUUID().toString()));

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService);

        assertThat(priceReferenceResolver.resolveChannelReference(priceBuilder).toCompletableFuture())
            .hasFailed()
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve the channel reference on PriceDraft"
                + " with country:'DE' and value: 'EUR 10'. Reason: Found a UUID in the id field. Expecting a key"
                + " without a UUID value. If you want to allow UUID values for reference keys, please"
                + " use the allowUuidKeys(true) option in the sync options.");
    }

    @Test
    public void resolveReferences_WithNoCustomTypeReferenceAndNoChannelReference_ShouldNotResolveReferences() {
        final PriceDraft priceDraft = PriceDraftBuilder.of(MoneyImpl.of(BigDecimal.TEN, DefaultCurrencyUnits.EUR))
                                                       .country(CountryCode.DE)
                                                       .build();

        final PriceReferenceResolver priceReferenceResolver =
            new PriceReferenceResolver(syncOptions, typeService, channelService);

        final PriceDraft referencesResolvedDraft = priceReferenceResolver.resolveReferences(priceDraft)
                                                                         .toCompletableFuture().join();

        assertThat(referencesResolvedDraft.getCustom()).isNull();
        assertThat(referencesResolvedDraft.getChannel()).isNull();
    }
}
