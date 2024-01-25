package com.github.coderodde.util;

final class BruteForceBitVector {
    
    private final byte[] bytes;
    
    public BruteForceBitVector(int capacity) {
        capacity++;
        
        bytes = new byte[
                 capacity / Byte.SIZE + 
                (capacity % Byte.SIZE != 0 
                ? 1 
                : 0)
        ];
    }
    
    public int getNumberOfBits() {
        return bytes.length * Byte.SIZE;
    }
    
    public void writeBitOn(int index) {
        writeBit(index, true);
    }
    
    public void writeBitOff(int index) {
        writeBit(index, false);
    }
    
    public void writeBit(int index, boolean on) {
        writeBitImpl(index, on);
    }
    
    private void writeBitImpl(int index, boolean on) {
        if (on) {
            turnBitOn(index);
        } else {
            turnBitOff(index);
        }
    }
    
    public boolean readBit(int index) {
        return readBitImpl(index);
    }
    
    public boolean readBitImpl(int index) {
        int byteIndex = index / Byte.SIZE;
        int targetByteBitIndex = index % Byte.SIZE;
        byte targetByte = bytes[byteIndex];
        return (targetByte & (1 << targetByteBitIndex)) != 0;
    }
    
    public int rank(int index) {
        int rank = 0;
        
        for (int i = 0; i < index; i++) {
            if (readBit(i)) {
                rank++;
            }
        }
        
        return rank;
    }
    
    public int select(int index) {
        int counted = 0;
        
        for (int i = 0; i < getNumberOfBits(); i++) {
            if (readBitImpl(i)) {
                counted++;
                
                if (counted == index) {
                    return i;
                }
            }
        }
        
        return getNumberOfBits();
    }
    
    private void turnBitOn(int index) {
        int byteIndex = index / Byte.SIZE;
        int bitIndex = index % Byte.SIZE;
        byte mask = 1;
        mask <<= bitIndex;
        bytes[byteIndex] |= mask;
    }
    
    /**
     * Turns the {@code index}th bit off. Indexation is zero-based.
     * 
     * @param index the target bit index.
     */
    private void turnBitOff(int index) {
        int byteIndex = index / Byte.SIZE;
        int bitIndex = index % Byte.SIZE;
        byte mask = 1;
        mask <<= bitIndex;
        bytes[byteIndex] &= ~mask;
    }
}
