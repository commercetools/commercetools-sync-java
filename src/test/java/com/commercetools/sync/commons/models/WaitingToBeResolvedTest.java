package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WaitingToBeResolvedTest {


    private final ProductDraft productDraft = mock(ProductDraft.class);
    private final Set<String> refSet = new HashSet<>();
    private final WaitingToBeResolved obj1 = new WaitingToBeResolved(productDraft, refSet);

    @Test
    void testEquals() {
        // preparation
        WaitingToBeResolved obj2 = new WaitingToBeResolved(productDraft, refSet);

        // test
        boolean equalsResult = obj1.equals(obj2);

        // assertions
        assertTrue(equalsResult);
    }

    @Test
    void testEquals_withSameRef() {

        // test
        boolean equalsResult = obj1.equals(obj1);

        // assertions
        assertTrue(equalsResult);
    }

    @Test
    void testEquals_withSimilarObjects() {
        // preparation
        when(productDraft.getKey()).thenReturn("test-product-key");
        refSet.add("test-reference-key");
        WaitingToBeResolved obj2 = new WaitingToBeResolved(productDraft, refSet);

        // test
        boolean equalsResult = obj1.equals(obj2);

        // assertions
        assertTrue(equalsResult);
    }

    @Test
    void testHashCode_withSameProductKeyAndSameRefSet() {
        // preparation
        WaitingToBeResolved obj2 = new WaitingToBeResolved(productDraft, refSet);

        // test
        int hash1 = obj1.hashCode();
        int hash2 = obj2.hashCode();

        // assertions
        assertEquals(hash1, hash2);
    }
}