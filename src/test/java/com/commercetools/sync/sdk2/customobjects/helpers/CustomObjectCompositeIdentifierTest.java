package com.commercetools.sync.sdk2.customobjects.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

public class CustomObjectCompositeIdentifierTest {

  private static final String CONTAINER = "container";
  private static final String KEY = "key";

  @Test
  void of_WithCustomObjectDraft_ShouldCreateCustomObjectCompositeIdentifier() {
    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(CONTAINER)
            .key(KEY)
            .value(StringUtils.EMPTY)
            .build();

    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(customObjectDraft);

    assertThat(customObjectCompositeIdentifier).isNotNull();
    assertThat(customObjectCompositeIdentifier.getContainer()).isEqualTo(CONTAINER);
    assertThat(customObjectCompositeIdentifier.getKey()).isEqualTo(KEY);
  }

  @Test
  void of_WithContainerAndKeyParams_ShouldCreateCustomObjectCompositeIdentifier() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);

    assertThat(customObjectCompositeIdentifier).isNotNull();
    assertThat(customObjectCompositeIdentifier.getContainer()).isEqualTo(CONTAINER);
    assertThat(customObjectCompositeIdentifier.getKey()).isEqualTo(KEY);
  }

  @Test
  void of_WithCustomObject_ShouldCreateCustomObjectCompositeIdentifier() {
    final CustomObject customObject = mock(CustomObject.class);
    when(customObject.getContainer()).thenReturn(CONTAINER);
    when(customObject.getKey()).thenReturn(KEY);

    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(customObject);

    assertThat(customObjectCompositeIdentifier).isNotNull();
    assertThat(customObjectCompositeIdentifier.getContainer()).isEqualTo(CONTAINER);
    assertThat(customObjectCompositeIdentifier.getKey()).isEqualTo(KEY);
  }

  @Test
  void of_WithInvalidIdentifierAsString_ShouldThrowAnError() {
    assertThatThrownBy(() -> CustomObjectCompositeIdentifier.of("aContainer-andKey"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "The custom object identifier value: \"aContainer-andKey\" does not have the correct format. "
                + "The correct format must have a vertical bar \"|\" character between the container and key.");
  }

  @Test
  void equals_WithSameObj_ShouldReturnTrue() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);

    boolean result = customObjectCompositeIdentifier.equals(customObjectCompositeIdentifier);

    assertTrue(result);
  }

  @Test
  void equals_WithDiffType_ShouldReturnFalse() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);
    final Object other = new Object();

    boolean result = customObjectCompositeIdentifier.equals(other);

    assertFalse(result);
  }

  @Test
  void equals_WithEqualObjects_ShouldReturnTrue() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);
    final CustomObjectCompositeIdentifier other =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);

    boolean result = customObjectCompositeIdentifier.equals(other);

    assertTrue(result);
  }

  @Test
  void equals_WithDifferentKeys_ShouldReturnFalse() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of("key1", CONTAINER);
    final CustomObjectCompositeIdentifier other =
        CustomObjectCompositeIdentifier.of("key2", CONTAINER);

    boolean result = customObjectCompositeIdentifier.equals(other);

    assertFalse(result);
  }

  @Test
  void equals_WithDifferentContainers_ShouldReturnFalse() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, "container1");
    final CustomObjectCompositeIdentifier other =
        CustomObjectCompositeIdentifier.of(KEY, "container2");

    boolean result = customObjectCompositeIdentifier.equals(other);

    assertFalse(result);
  }

  @Test
  void hashCode_withSameInstances_ShouldBeEquals() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);
    final CustomObjectCompositeIdentifier other = customObjectCompositeIdentifier;

    final int hash1 = customObjectCompositeIdentifier.hashCode();
    final int hash2 = other.hashCode();

    assertEquals(hash1, hash2);
  }

  @Test
  void hashCode_withSameKeyAndSameContainer_ShouldBeEquals() {
    // preparation
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);
    final CustomObjectCompositeIdentifier other =
        CustomObjectCompositeIdentifier.of(KEY, CONTAINER);

    // test
    final int hash1 = customObjectCompositeIdentifier.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertEquals(hash1, hash2);
  }

  @Test
  void hashCode_withDifferentKeyAndSameContainer_ShouldNotBeEquals() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of("key1", CONTAINER);

    final CustomObjectCompositeIdentifier other =
        CustomObjectCompositeIdentifier.of("key2", CONTAINER);

    final int hash1 = customObjectCompositeIdentifier.hashCode();
    final int hash2 = other.hashCode();

    assertNotEquals(hash1, hash2);
  }

  @Test
  void hashCode_withSameKeyAndDifferentContainer_ShouldNotBeEquals() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of(KEY, "container1");
    final CustomObjectCompositeIdentifier other =
        CustomObjectCompositeIdentifier.of(KEY, "container2");

    final int hash1 = customObjectCompositeIdentifier.hashCode();
    final int hash2 = other.hashCode();

    assertNotEquals(hash1, hash2);
  }

  @Test
  void hashCode_withCompletelyDifferentValues_ShouldNotBeEquals() {
    final CustomObjectCompositeIdentifier customObjectCompositeIdentifier =
        CustomObjectCompositeIdentifier.of("key1", "container1");
    final CustomObjectCompositeIdentifier other =
        CustomObjectCompositeIdentifier.of("key2", "container2");

    final int hash1 = customObjectCompositeIdentifier.hashCode();
    final int hash2 = other.hashCode();

    assertNotEquals(hash1, hash2);
  }
}
