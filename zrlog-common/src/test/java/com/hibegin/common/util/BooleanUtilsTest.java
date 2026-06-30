package com.hibegin.common.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BooleanUtilsTest {

    @Test
    public void shouldTreatOnlyLowercaseTrueAsTrue() {
        assertTrue(BooleanUtils.isTrue("true"));
        assertFalse(BooleanUtils.isTrue("TRUE"));
        assertFalse(BooleanUtils.isTrue("false"));
        assertFalse(BooleanUtils.isTrue(null));
    }

    @Test
    public void shouldTreatNullAndLowercaseFalseAsFalse() {
        assertTrue(BooleanUtils.isFalse(null));
        assertTrue(BooleanUtils.isFalse("false"));
        assertFalse(BooleanUtils.isFalse("FALSE"));
        assertFalse(BooleanUtils.isFalse("true"));
    }
}
