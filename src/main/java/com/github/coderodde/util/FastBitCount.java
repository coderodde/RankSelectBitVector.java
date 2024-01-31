package com.github.coderodde.util;

/**
 *
 * @version 1.0.3
 * @since 1.0.0
 */
public final class FastBitCount {
    
    /**
     * Computes efficiently the number of set bits in the {@code value}.
     * 
     * @param value the value in which to count the number of bits.
     * 
     * @return the number of set bits.
     */
    public static native int popcnt(long value);
}
