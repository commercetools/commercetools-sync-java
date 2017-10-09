package com.commercetools.sync.products;

import org.junit.Test;

import static com.commercetools.sync.products.ActionGroup.ATTRIBUTES;
import static com.commercetools.sync.products.ActionGroup.CATEGORIES;
import static com.commercetools.sync.products.ActionGroup.DESCRIPTION;
import static com.commercetools.sync.products.ActionGroup.IMAGES;
import static com.commercetools.sync.products.ActionGroup.METADESCRIPTION;
import static com.commercetools.sync.products.ActionGroup.NAME;
import static com.commercetools.sync.products.ActionGroup.PRICES;
import static com.commercetools.sync.products.SyncFilter.ofBlackList;
import static com.commercetools.sync.products.SyncFilter.ofWhiteList;
import static org.assertj.core.api.Assertions.assertThat;

public class SyncFilterTest {

    @Test
    public void filterActionGroup_WithWhiteListingSingleOption_ShouldFilterInOnlyThisOption() {
        final SyncFilter syncFilter = ofWhiteList(IMAGES);

        assertThat(syncFilter.filterActionGroup(IMAGES)).isTrue();
        assertThat(syncFilter.filterActionGroup(ATTRIBUTES)).isFalse();
        assertThat(syncFilter.filterActionGroup(CATEGORIES)).isFalse();
        assertThat(syncFilter.filterActionGroup(DESCRIPTION)).isFalse();
        assertThat(syncFilter.filterActionGroup(METADESCRIPTION)).isFalse();
        assertThat(syncFilter.filterActionGroup(NAME)).isFalse();
    }

    @Test
    public void filterActionGroup_WithWhiteListingMultipleOptions_ShouldFilterInOnlyTheseOptions() {
        final SyncFilter syncFilter = ofWhiteList(IMAGES, ATTRIBUTES, CATEGORIES);

        assertThat(syncFilter.filterActionGroup(IMAGES)).isTrue();
        assertThat(syncFilter.filterActionGroup(ATTRIBUTES)).isTrue();
        assertThat(syncFilter.filterActionGroup(CATEGORIES)).isTrue();
        assertThat(syncFilter.filterActionGroup(DESCRIPTION)).isFalse();
        assertThat(syncFilter.filterActionGroup(METADESCRIPTION)).isFalse();
        assertThat(syncFilter.filterActionGroup(NAME)).isFalse();
    }

    @Test
    public void filterActionGroup_WithBlackListingSingleOption_ShouldFilterOutOnlyThisOption() {
        final SyncFilter syncFilter = ofBlackList(PRICES);

        assertThat(syncFilter.filterActionGroup(PRICES)).isFalse();
        assertThat(syncFilter.filterActionGroup(ATTRIBUTES)).isTrue();
        assertThat(syncFilter.filterActionGroup(CATEGORIES)).isTrue();
        assertThat(syncFilter.filterActionGroup(DESCRIPTION)).isTrue();
        assertThat(syncFilter.filterActionGroup(METADESCRIPTION)).isTrue();
        assertThat(syncFilter.filterActionGroup(NAME)).isTrue();
    }

    @Test
    public void filterActionGroup_WithBlackListingMultipleOptions_ShouldFilterOutOnlyTheseOptions() {
        final SyncFilter syncFilter = ofBlackList(PRICES, IMAGES, ATTRIBUTES, NAME);

        assertThat(syncFilter.filterActionGroup(PRICES)).isFalse();
        assertThat(syncFilter.filterActionGroup(IMAGES)).isFalse();
        assertThat(syncFilter.filterActionGroup(ATTRIBUTES)).isFalse();
        assertThat(syncFilter.filterActionGroup(NAME)).isFalse();
        assertThat(syncFilter.filterActionGroup(CATEGORIES)).isTrue();
        assertThat(syncFilter.filterActionGroup(DESCRIPTION)).isTrue();
        assertThat(syncFilter.filterActionGroup(METADESCRIPTION)).isTrue();
    }
}
