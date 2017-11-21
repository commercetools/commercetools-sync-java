package com.commercetools.sync.commons.utils;

import org.junit.Test;

import static com.commercetools.sync.commons.utils.SyncSolutionInfo.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;

public class SyncSolutionInfoTest {

    @Test
    public void isAttributeUnspecified_WithNullAttribute_ShouldReturnTrue() {
        assertThat(SyncSolutionInfo.isAttributeUnspecified(null)).isTrue();
    }

    @Test
    public void isAttributeUnspecified_WithEmptyAttribute_ShouldReturnTrue() {
        assertThat(SyncSolutionInfo.isAttributeUnspecified("")).isTrue();
    }

    @Test
    public void isAttributeUnspecified_WithUnspecifiedAttribute_ShouldReturnTrue() {
        assertThat(SyncSolutionInfo.isAttributeUnspecified(UNSPECIFIED)).isTrue();
    }

    @Test
    public void isAttributeUnspecified_WithAttribute_ShouldReturnFalse() {
        assertThat(SyncSolutionInfo.isAttributeUnspecified("v1.0.0-M6")).isFalse();
    }
}
