package com.commercetools.sync.products;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class SyncFilterTest {

  @Test
  void of_returnsSameInstanceOfDefaultSyncFilter() {
    final SyncFilter syncFilter = SyncFilter.of();
    final SyncFilter otherSyncFilter = SyncFilter.of();
    assertThat(syncFilter).isSameAs(otherSyncFilter);
  }

  @Test
  void filterActionGroup_WithWhiteListingSingleGroup_ShouldFilterInOnlyThisGroup() {
    final SyncFilter syncFilter = SyncFilter.ofWhiteList(ActionGroup.IMAGES);

    assertThat(syncFilter.filterActionGroup(ActionGroup.IMAGES)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.ATTRIBUTES)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.CATEGORIES)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.DESCRIPTION)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.METADESCRIPTION)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.NAME)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.ASSETS)).isFalse();
  }

  @Test
  void filterActionGroup_WithWhiteListingMultipleGroups_ShouldFilterInOnlyTheseGroups() {
    final SyncFilter syncFilter =
        SyncFilter.ofWhiteList(
            ActionGroup.IMAGES, ActionGroup.ATTRIBUTES, ActionGroup.CATEGORIES, ActionGroup.ASSETS);

    assertThat(syncFilter.filterActionGroup(ActionGroup.IMAGES)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.ATTRIBUTES)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.CATEGORIES)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.ASSETS)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.DESCRIPTION)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.METADESCRIPTION)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.NAME)).isFalse();
  }

  @Test
  void filterActionGroup_WithBlackListingSingleGroup_ShouldFilterOutOnlyThisGroup() {
    final SyncFilter syncFilter = SyncFilter.ofBlackList(ActionGroup.PRICES);

    assertThat(syncFilter.filterActionGroup(ActionGroup.PRICES)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.ATTRIBUTES)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.CATEGORIES)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.DESCRIPTION)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.METADESCRIPTION)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.NAME)).isTrue();
  }

  @Test
  void filterActionGroup_WithBlackListingMultiplGroups_ShouldFilterOutOnlyTheseGroups() {
    final SyncFilter syncFilter =
        SyncFilter.ofBlackList(
            ActionGroup.PRICES, ActionGroup.IMAGES, ActionGroup.ATTRIBUTES, ActionGroup.NAME);

    assertThat(syncFilter.filterActionGroup(ActionGroup.PRICES)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.IMAGES)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.ATTRIBUTES)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.NAME)).isFalse();
    assertThat(syncFilter.filterActionGroup(ActionGroup.CATEGORIES)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.DESCRIPTION)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.METADESCRIPTION)).isTrue();
    assertThat(syncFilter.filterActionGroup(ActionGroup.ASSETS)).isTrue();
  }

  @Test
  void filterActionGroup_WithDefaultSyncFilterShouldFilterInAllActionGroups() {
    final SyncFilter syncFilter = SyncFilter.of();
    EnumSet.allOf(ActionGroup.class)
        .forEach(actionGroup -> assertThat(syncFilter.filterActionGroup(actionGroup)).isTrue());
  }
}
