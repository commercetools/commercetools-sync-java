package com.commercetools.sync.inventories.helpers;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import java.util.*;
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
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);

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
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(InventoryBatchValidator.INVENTORY_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithInventoryDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final InventoryEntryDraft inventoryDraft = mock(InventoryEntryDraft.class);
    final Set<InventoryEntryDraft> validDrafts =
        getValidDrafts(Collections.singletonList(inventoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(InventoryBatchValidator.INVENTORY_DRAFT_SKU_NOT_SET);
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
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(InventoryBatchValidator.INVENTORY_DRAFT_SKU_NOT_SET);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
    final InventoryEntryDraft validInventoryDraft = mock(InventoryEntryDraft.class);
    when(validInventoryDraft.getSku()).thenReturn("validDraftSku");
    when(validInventoryDraft.getSupplyChannel())
        .thenReturn(ChannelResourceIdentifierBuilder.of().key("validSupplyChannelKey").build());
    when(validInventoryDraft.getCustom())
        .thenReturn(
            CustomFieldsDraftBuilder.of()
                .type(TypeResourceIdentifierBuilder.of().key("typeKey").build())
                .build());

    final InventoryEntryDraft validMainInventoryDraft = mock(InventoryEntryDraft.class);
    when(validMainInventoryDraft.getSku()).thenReturn("validDraftSku1");

    final InventoryEntryDraft invalidInventoryDraft = mock(InventoryEntryDraft.class);
    when(invalidInventoryDraft.getSupplyChannel())
        .thenReturn(ChannelResourceIdentifierBuilder.of().key("key").build());

    final InventoryBatchValidator inventoryBatchValidator =
        new InventoryBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<InventoryEntryDraft>, InventoryBatchValidator.ReferencedKeys> pair =
        inventoryBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validInventoryDraft, invalidInventoryDraft, validMainInventoryDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(InventoryBatchValidator.INVENTORY_DRAFT_SKU_NOT_SET);
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
