package com.commercetools.sync.inventories.helpers;

import static com.commercetools.sync.inventories.helpers.InventoryBatchValidator.INVENTORY_DRAFT_IS_NULL;
import static com.commercetools.sync.inventories.helpers.InventoryBatchValidator.INVENTORY_DRAFT_SKU_NOT_SET;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryBatchValidatorTest {

  private InventorySyncOptions syncOptions;
  private InventorySyncStatistics syncStatistics;
  private List<String> errorCallBackMessages;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final SphereClient ctpClient = mock(SphereClient.class);

    syncOptions =
        InventorySyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorCallBackMessages.add(exception.getMessage()))
            .build();
    syncStatistics = mock(InventorySyncStatistics.class);
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<InventoryEntryDraft> validDrafts = getValidDrafts(Collections.emptyList());

    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullInventoryDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<InventoryEntryDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(INVENTORY_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithInventoryDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final InventoryEntryDraft inventoryDraft = mock(InventoryEntryDraft.class);
    final Set<InventoryEntryDraft> validDrafts =
        getValidDrafts(Collections.singletonList(inventoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(INVENTORY_DRAFT_SKU_NOT_SET);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithInventoryDraftWithEmptySku_ShouldHaveValidationErrorAndEmptyResult() {
    final InventoryEntryDraft inventoryDraft = mock(InventoryEntryDraft.class);
    when(inventoryDraft.getSku()).thenReturn(EMPTY);
    final Set<InventoryEntryDraft> validDrafts =
        getValidDrafts(Collections.singletonList(inventoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(INVENTORY_DRAFT_SKU_NOT_SET);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
    final InventoryEntryDraft validInventoryDraft = mock(InventoryEntryDraft.class);
    when(validInventoryDraft.getSku()).thenReturn("validDraftSku");
    when(validInventoryDraft.getSupplyChannel())
        .thenReturn(ResourceIdentifier.ofKey("validSupplyChannelKey"));
    when(validInventoryDraft.getCustom())
        .thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", Collections.emptyMap()));

    final InventoryEntryDraft validMainInventoryDraft = mock(InventoryEntryDraft.class);
    when(validMainInventoryDraft.getSku()).thenReturn("validDraftSku1");

    final InventoryEntryDraft invalidInventoryDraft = mock(InventoryEntryDraft.class);
    when(invalidInventoryDraft.getSupplyChannel()).thenReturn(ResourceIdentifier.ofKey("key"));

    final InventoryBatchValidator inventoryBatchValidator =
        new InventoryBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<InventoryEntryDraft>, InventoryBatchValidator.ReferencedKeys> pair =
        inventoryBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validInventoryDraft, invalidInventoryDraft, validMainInventoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(INVENTORY_DRAFT_SKU_NOT_SET);
    assertThat(pair.getLeft())
        .containsExactlyInAnyOrder(validInventoryDraft, validMainInventoryDraft);
    assertThat(pair.getRight().getChannelKeys()).containsExactlyInAnyOrder("validSupplyChannelKey");
    assertThat(pair.getRight().getTypeKeys()).containsExactlyInAnyOrder("typeKey");
  }

  @Nonnull
  private Set<InventoryEntryDraft> getValidDrafts(
      @Nonnull final List<InventoryEntryDraft> inventoryDrafts) {
    final InventoryBatchValidator inventoryBatchValidator =
        new InventoryBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<InventoryEntryDraft>, InventoryBatchValidator.ReferencedKeys> pair =
        inventoryBatchValidator.validateAndCollectReferencedKeys(inventoryDrafts);
    return pair.getLeft();
  }
}
