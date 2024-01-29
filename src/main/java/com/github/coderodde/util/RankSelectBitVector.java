package com.github.coderodde.util;

/**
 * This class defines a packed bit vector that supports {@code rank()} operation
 * in {@code O(1)} time, and {@code select()} in {@code O(log n)} time.
 * 
 * @version 1.0.1
 * @since 1.0.0
 */
public final class RankSelectBitVector {
    
    /**
     * Indicates whether some bits were changed since the previous building of
     * the index data structures.
     */
    private boolean hasDirtyState = true;
    
    /**
     * The actual bit storage array.
     */
    private final long[] wordData;
    
    /**
     * The actual requested number of bits in this bit vector. Will be smaller 
     * than the total capacity.
     */
    private final int numberOfRequestedBits;
    
    /**
     * Denotes index of the most rightmost meaningful bit. Will be set to 
     * {@code numberOfRequestedBits - 1}.
     */
    private final int maximumBitIndex;
    
    /**
     * Caches the number of bits set to one (1).
     */
    private int numberOfSetBits;
    
    /**
     * The block size in the {@code first} table.
     */
    private int ell;
    
    /**
     * The block size in the {@code second} table.
     */
    private int k;
    
    // The following three tables hold the index necessary for efficient rank 
    // operation. According to internet, has space 
    // O(sgrt(n) * log log n * log n.
    private int[] first;
    private int[] second;
    private int[][] third;
    
    /**
     * Constructs a new bit vector.
     * 
     * @param numberOfRequestedBits the actual number of bits to support.
     */
    public RankSelectBitVector(int numberOfRequestedBits) {
        checkNumberOfRequestedBits(numberOfRequestedBits);
        
        this.numberOfRequestedBits = numberOfRequestedBits;
        
        // Calculate the actual number of storage bytes:
        int numberOfLongs = numberOfRequestedBits / Long.SIZE + 
                           (numberOfRequestedBits % Long.SIZE != 0 ? 1 : 0);
        
        numberOfLongs++; // Padding tail long in order to simplify the last 
                         // rank/select.
        
        wordData = new long[numberOfLongs];
        
        // Set the rightmost, valid index:
        this.maximumBitIndex = this.wordData.length * Long.SIZE - 1;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("[Bit vector, size = ");
        
        sb.append(getNumberOfSupportedBits())
          .append(" bits, data = ");
        
        int bitNumber = 0;
        
        for (int i = 0; i < getNumberOfSupportedBits(); i++) {
            sb.append(readBitImpl(i) ? "1" : "0");
            
            bitNumber++;
            
            if (bitNumber % 8 == 0) {
                sb.append(" ");
            }
        }
        
        return sb.append("]").toString();
    }
    
    /**
     * Preprocesses the internal data structures in {@code O(n)}.
     */
    public void buildIndices() {
        if (hasDirtyState == false) {
            // Nothing to do.
            return;
        }
        
        //// Deal with the 'first'.
        // n - total number of bit slots:
        int n = wordData.length * Long.SIZE;
        
        // elll - the l value:
        this.ell = (int) Math.pow(Math.ceil(log2(n) / 2.0), 2.0);
        this.first = new int[n / ell + 1];
        
        for (int i = ell; i < n; i++) {
            if (i % ell == 0) {
                int firstArraySlotIndex = i / ell;
                int startIndex = i - ell;
                int endIndex   = i - 1;
                
                first[firstArraySlotIndex]     =
                first[firstArraySlotIndex - 1] + 
                bruteForceRank(startIndex,
                               endIndex);
            }
        }
        
        //// Deal with the 'second'.
        this.k = (int) Math.ceil(log2(n) / 2.0);
        this.second = new int[n / k + 1];
        
        for (int i = k; i < n; i++) {
            if (i % k == 0) {
                second[i/k] = bruteForceRank(ell * (i / ell), i - 1);
            }
        }
        
        //// Deal with the 'third': four Russians' technique:
        this.third = new int[(int) Math.pow(2.0, k - 1)][];
        
        for (int selectorIndex = 0;
                 selectorIndex < third.length;
                 selectorIndex++) {
            
            third[selectorIndex] = new int[k - 1];
            
            // third[selectorIndex][0] is always zero (0).
            third[selectorIndex][0] = (bitIsSet(selectorIndex, k - 2) ? 1 : 0);
            
            for (int j = 1; j < k - 1; j++) {
                third[selectorIndex][j] = 
                third[selectorIndex][j - 1] + 
                        (bitIsSet(selectorIndex, k - j - 2) ? 1 : 0);
            }
        }
        
        hasDirtyState = false;
    }
    
    /**
     * Returns the number of bits that are set (have value of one (1)).
     * 
     * @return the number of set bits.
     */
    public int getNumberOfSetBits() {
        return numberOfSetBits;
    }
    
    /**
     * Returns the number of bits this bit vector supports.
     * 
     * @return the number of bits supported.
     */
    public int getNumberOfSupportedBits() {
        return numberOfRequestedBits;
    }
    
    /**
     * Sets the {@code index}th bit to one (1).
     * 
     * @param index the index of the target bit.
     */
    public void writeBitOn(int index) {
        writeBit(index, true);
    }
    
    /**
     * Sets the {@code index}th bit to zero (0).
     * 
     * @param index the index of the target bit.
     */
    public void writeBitOff(int index) {
        writeBit(index, false);
    }
    
    /**
     * Writes the {@code index}th bit to {@code on}.
     * 
     * @param index the index of the target bit.
     * @param on    the selector of the bit: if {@code true}, the bit will be 
     *              set to one, otherwise set zero.
     */
    public void writeBit(int index, boolean on) {
        checkBitAccessIndex(index);
        writeBitImpl(index, on);
    }
    
    /**
     * Reads the {@code index}th bit where indexation starts from zero (0).
     * 
     * @param index the bit index.
     * @return {@code true} if and only if the {@code index}th bit is set.
     */
    public boolean readBit(int index) {
        checkBitAccessIndex(index);
        return readBitImpl(index);
    }
    
    /**
     * Returns the rank of {@code index}, i.e., the number of set bits in the 
     * subvector {@code vector[1..index]}. Runs in {@code O((log n)^2)} time.
     * 
     * @param index the target index.
     * @return the rank for the input target.
     */
    public int rankFirst(int index) {
        checkBitIndexForRank(index);
        makeSureStateIsCompiled();
        
        int startIndex = ell * (index / ell);
        int endIndex = index - 1;
        
        return first[index / ell] + bruteForceRank(startIndex, endIndex);
    }
    
    /**
     * Returns the {@code index}th rank. Runs in {@code O(log n)} time.
     * 
     * @param index the target index.
     * @return the rank of the input index.
     */
    public int rankSecond(int index) {
        checkBitIndexForRank(index);
        makeSureStateIsCompiled();
        
        int startIndex = k * (index / k);
        int endIndex = index - 1;
        
        return first[index / ell] +
               second[index / k] + 
               bruteForceRank(startIndex, 
                              endIndex);
    }
    
    /**
     * Returns the {@code index}th rank. Runs in {@code O(1)} time.
     * 
     * @param index the target index.
     * @return the rank of the input index.
     */
    public int rankThird(int index) {
        checkBitIndexForRank(index);
        makeSureStateIsCompiled();
        
        int f = first[index / ell];
        int s = second[index / k];
        
        int thirdEntryIndex = index % k - 1;
        
        if (thirdEntryIndex == -1) {
            return f + s;
        }
        
        int selectorIndexF = computeSelectorIndex(index);
        
        return f + s + third[selectorIndexF][thirdEntryIndex];
    }
    
    /**
     * Returns the index of the {@code index}th 1-bit. Relies on 
     * {@link #rankFirst(int)}, which runs in {@code O((log n)^2)}, which yields
     * {@code O((log n)^3)} running time for the {@code selectFirst}.
     * 
     * @param bitIndex the target index.
     * @return the index of the {@code index}th 1-bit.
     */
    public int selectFirst(int bitIndex) {
        checkBitIndexForSelect(bitIndex);
        return selectImplFirst(bitIndex, 0, getNumberOfSupportedBits());
    }
    
    /**
     * Returns the index of the {@code index}th 1-bit. Relies on 
     * {@link #rankSecond(int)}, which runs in {@code O(log n)}, which yields
     * {@code O((log n)^2)} running time for the {@code selectSecond}.
     * 
     * @param bitIndex the target index.
     * @return the index of the {@code index}th 1-bit.
     */
    public int selectSecond(int bitIndex) {
        checkBitIndexForSelect(bitIndex);
        return selectImplSecond(bitIndex, 0, getNumberOfSupportedBits());
    }
    
    /**
     * Returns the index of the {@code index}th 1-bit. Relies on 
     * {@link #rankThird(int)}, which runs in {@code O(1)}, which yields
     * {@code O(log n)} running time for the {@code selectThird}.
     * 
     * @param bitIndex the target index.
     * @return the index of the {@code index}th 1-bit.
     */
    public int selectThird(int bitIndex) {
        checkBitIndexForSelect(bitIndex);
        return selectImplThird(bitIndex, 0, getNumberOfSupportedBits());
    }
    
    private int selectImplFirst(int bitIndex,
                                int rangeStartIndex,
                                int rangeLength) {
        
        if (rangeLength == 1) {
            return rangeStartIndex;
        }
        
        int halfRangeLength = rangeLength / 2;
        int r = rankFirst(halfRangeLength + rangeStartIndex);
        
        if (r >= bitIndex) {
            return selectImplFirst(bitIndex, 
                                   rangeStartIndex,
                                   halfRangeLength);
        } else {
            return selectImplFirst(bitIndex, 
                                   rangeStartIndex + halfRangeLength,
                                   rangeLength - halfRangeLength);
        }
    }
    
    private int selectImplSecond(int bitIndex, 
                                 int rangeStartIndex,
                                 int rangeLength) {
        
        if (rangeLength == 1) {
            return rangeStartIndex;
        }
        
        int halfRangeLength = rangeLength / 2;
        int r = rankSecond(halfRangeLength + rangeStartIndex);
        
        if (r >= bitIndex) {
            return selectImplSecond(bitIndex, 
                                    rangeStartIndex,
                                    halfRangeLength);
        } else {
            return selectImplSecond(bitIndex, 
                                    rangeStartIndex + halfRangeLength,
                                    rangeLength - halfRangeLength);
        }
    }
    
    private int selectImplThird(int bitIndex,
                                int rangeStartIndex, 
                                int rangeLength) {
        
        if (rangeLength == 1) {
            return rangeStartIndex;
        }
        
        int halfRangeLength = rangeLength / 2;
        int r = rankThird(halfRangeLength + rangeStartIndex);
        
        if (r >= bitIndex) {
            return selectImplThird(bitIndex, 
                                   rangeStartIndex,
                                   halfRangeLength);
        } else {
            return selectImplThird(bitIndex, 
                                   rangeStartIndex + halfRangeLength,
                                   rangeLength - halfRangeLength);
        }
    }
    
    /**
     * The delegate for manipulating bits.
     * 
     * @param index the index of the target bit.
     * @param on    the flag deciding the value of the bit in question.
     */
    private void writeBitImpl(int index, boolean on) {
        boolean previousBitValue = readBit(index);
        
        if (on) {
            if (previousBitValue == false) {
                hasDirtyState = true;
                numberOfSetBits++;
            }
            
            turnBitOn(index);
        } else {
            if (previousBitValue == true) {
                hasDirtyState = true;
                numberOfSetBits--;
            }
            
            turnBitOff(index);
        }
    }
    
    /**
     * Implements the actual reading of a bit.
     * 
     * @param index the index of the target bit to read.
     * @return the value of the target bit.
     */
    boolean readBitImpl(int index) {
        int targetLongIndex = index / Long.SIZE;
        int targetLongBitIndex = index % Long.SIZE;
        long targetLong = wordData[targetLongIndex];
        return (targetLong & (1L << targetLongBitIndex)) != 0;
    }
    
    /**
     * Makes sure that the state of the internal data structures is up to date.
     */
    private void makeSureStateIsCompiled() {
        if (hasDirtyState) {
            buildIndices();
            hasDirtyState = false;
        }
    }
    
    /**
     * Turns the {@code index}th bit on. Indexation is zero-based.
     * 
     * @param index the target bit index.
     */
    private void turnBitOn(int index) {
        int targetLongIndex = index / Long.SIZE;
        int targetLongBitIndex = index % Long.SIZE;
        long mask = 1L;
        mask <<= targetLongBitIndex;
        wordData[targetLongIndex] |= mask;
    }
    
    /**
     * Turns the {@code index}th bit off. Indexation is zero-based.
     * 
     * @param index the target bit index.
     */
    private void turnBitOff(int index) {
        int targetLongIndex = index / Long.SIZE;
        int targetLongBitIndex = index % Long.SIZE;
        long mask = 1L;
        mask <<= targetLongBitIndex;
        wordData[targetLongIndex] &= ~mask;
    }
    
    private void checkBitIndexForSelect(int selectionIndex) {
        if (selectionIndex < 0) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "The input selection index is negative: " + 
                            "(%d). Must be within range [1..%d].\n",
                            selectionIndex,
                            numberOfSetBits));
        }
        
        if (selectionIndex == 0) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "The input selection index is zero (0). " + 
                            "Must be within range [1..%d].\n",
                            numberOfSetBits));
        }
        
        if (selectionIndex > numberOfSetBits) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "The input selection index is too large (%d). " + 
                            "Must be within range [1..%d].\n", 
                            selectionIndex, 
                            numberOfSetBits));
        }
    }
    
    private void checkBitIndexForRank(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(
                    String.format("Negative bit index: %d.", index));
        } 
        
        if (index > numberOfRequestedBits) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "Too large bit index (%d), number of bits " + 
                            "supported is %d.",
                            index, 
                            numberOfRequestedBits));
        }
    }
    
    private void checkBitAccessIndex(int accessIndex) {
        if (accessIndex < 0) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "Negative bit access index: %d.",
                            accessIndex));
        } 
        
        if (accessIndex >= getNumberOfSupportedBits()) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "Too large bit access index (%d), number of bits " + 
                            "supported is %d.",
                            accessIndex, 
                            getNumberOfSupportedBits()));
        }
    }
    
    /**
     * Returns {@code true} if and only if the {@code bitIndex}th bit in 
     * {@code value} is set.
     * 
     * @param value    the value of which to inspect the bit.
     * @param bitIndex the bit index.
     * @return {@code true} if and only if the specified bit is set.
     */
    private boolean bitIsSet(int value, int bitIndex) {
        return (value & (1 << bitIndex)) != 0;
    }
    
    int toInteger(int numberOfBitsToRead) {
        int integer = 0;
        
        for (int i = 0; i < numberOfBitsToRead; i++) {
            
            boolean bit = readBitImpl(i);
            
            if (bit == true) {
                integer |= 1 << i;
            }
        }
        
        return integer;
    }
    
    int computeSelectorIndex(int i) {
        int startBitIndex = k * (i / k);
        int endBitIndex = Math.min(k * (i / k + 1) - 2, maximumBitIndex);
        
        int startLongIndex = startBitIndex / Long.SIZE;
        int endLongIndex = endBitIndex / Long.SIZE;
        
        if (startLongIndex == endLongIndex) {
            int bitRangeLength = endBitIndex - startBitIndex + 1;
            int omitBitCountRight = startBitIndex - Long.SIZE * startLongIndex;
            int omitBitCountLeft = 
                    Long.SIZE - omitBitCountRight - bitRangeLength;
                    
            long word = wordData[startLongIndex];
            word = Long.reverse(word);
            
            word  <<= omitBitCountRight;
            word >>>= omitBitCountLeft + omitBitCountRight;
            
            return (int) word;
        } else {
            // Here, 'startLongIndex + 1 == endLongIndex':
            // i = 65: 9 (1001) vs. 18 (10010)
            // i = 129: 36 (100100) vs. 41 (101001)
            // i = 192: 36 (100100) vs. 44 (101100)
            int lengthWordLo = Long.SIZE - startBitIndex
                                        + Long.SIZE * startLongIndex;
            
            int lengthWordHi = endBitIndex - Long.SIZE * endLongIndex + 1;
            
            long wordLo = wordData[startLongIndex];
            long wordHi = wordData[endLongIndex];
            
            wordLo = preprocessLowWord(wordLo, lengthWordLo, lengthWordHi);
            wordHi = preprocessHighWord(wordHi, lengthWordHi);
//            
//            int actualResult = extractBitVector(i).toInteger(k - 1);
//            int result1 = stitchCase1(wordLo, 
//                                      wordHi, 
//                                      lengthWordLo, 
//                                      lengthWordHi);
//            
//            int result2 = stitchCase2(wordLo,
//                                      wordHi,
//                                      lengthWordLo,
//                                      lengthWordHi);
//            
//            int result3 = stitchCase3(wordLo,
//                                      wordHi,
//                                      lengthWordLo,
//                                      lengthWordHi);
//            
//            int result4 = stitchCase4(wordLo, 
//                                      wordHi,
//                                      lengthWordLo, 
//                                      lengthWordHi);
//            
//            int result5 = stitchCase5(wordLo, 
//                                      wordHi,
//                                      lengthWordLo, 
//                                      lengthWordHi);
//            
//            int result6 = stitchCase6(wordLo, 
//                                      wordHi,
//                                      lengthWordLo, 
//                                      lengthWordHi);
//            
//            int result7 = stitchCase7(wordLo, 
//                                      wordHi,
//                                      lengthWordLo, 
//                                      lengthWordHi);
//            
//            int result8 = stitchCase8(wordLo, 
//                                      wordHi,
//                                      lengthWordLo, 
//                                      lengthWordHi);
//            
//            if (actualResult != result1 &&
//                actualResult != result2 &&
//                actualResult != result3 &&
//                actualResult != result4 &&
//                actualResult != result5 &&
//                actualResult != result6 &&
//                actualResult != result7 &&
//                actualResult != result8) {
//                
//                throw new IllegalStateException("MISMATCH!");
//            }
//            
//            System.out.println("Actual result: " + actualResult + 
//                    ", result1: " + result1 + ", result2: " + result2 + 
//                    ", result3: " + result3);
            
            
            
            int result = (int)(wordHi | wordLo);
            return result;
        }
    }
    
    private static long preprocessLowWord(long wordLo, 
                                          int lengthWordLo, 
                                          int lengthWordHi) {
        
        wordLo <<= Long.SIZE - lengthWordLo;
        wordLo >>>= Long.SIZE - lengthWordLo;
        wordLo <<= lengthWordHi;
        return wordLo;
    }
    
    private static long preprocessHighWord(long wordHi, int lengthWordHi) {
        wordHi = Long.reverse(wordHi);
        wordHi >>>= Long.SIZE - lengthWordHi;
        return wordHi;
    }
    
    int stitchCase1(long wordLo, 
                    long wordHi, 
                    int lengthWordLo, 
                    int lengthWordHi) {
        
        wordLo <<= Long.SIZE - lengthWordLo;
        wordLo = Long.reverse(wordLo);
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        wordHi <<= lengthWordLo;
        
        return (int)(wordHi | wordLo);
    }
    
    int stitchCase2(long wordLo, 
                    long wordHi, 
                    int lengthWordLo, 
                    int lengthWordHi) {
        
        wordLo <<= Long.SIZE - lengthWordLo;
        wordLo = Long.reverse(wordLo);
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        wordHi = Long.reverse(wordHi);
        wordHi >>>= Long.SIZE - lengthWordHi - lengthWordLo;
        
        return (int)(wordHi | wordLo);
    }
    
    int stitchCase3(long wordLo,
                    long wordHi,
                    int lengthWordLo,
                    int lengthWordHi) {
        
        wordLo <<= Long.SIZE - lengthWordLo;
        wordLo >>>= Long.SIZE - lengthWordLo;
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        wordHi <<= lengthWordLo;
        
        return (int)(wordHi | wordLo);
    }
    
    int stitchCase4(long wordLo,
                    long wordHi,
                    int lengthWordLo,
                    int lengthWordHi) {
        
        wordLo <<= Long.SIZE - lengthWordLo;
        wordLo >>>= Long.SIZE - lengthWordLo;
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        wordHi = Long.reverse(wordHi);
        wordHi >>>= Long.SIZE - lengthWordHi - lengthWordLo;
        
        return (int)(wordHi | wordLo);
    }
    
    int stitchCase5(long wordLo,
                    long wordHi,
                    int lengthWordLo,
                    int lengthWordHi) {
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        wordHi = Long.reverse(wordHi);
        wordHi >>>= Long.SIZE - lengthWordHi;
        
        wordLo <<= Long.SIZE - lengthWordLo;
        wordLo = Long.reverse(wordLo);
        wordLo <<= lengthWordHi;
        
        return (int)(wordHi | wordLo);
    }
    
    int stitchCase6(long wordLo,
                    long wordHi,
                    int lengthWordLo,
                    int lengthWordHi) {
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        wordHi = Long.reverse(wordHi);
        wordHi >>>= Long.SIZE - lengthWordHi;
        
        wordLo = Long.reverse(wordLo); // 1000|xxxx
        wordLo >>>= Long.SIZE - lengthWordLo; // 0000|1000
        wordLo >>>= lengthWordLo - lengthWordHi;
        
        return (int)(wordHi | wordLo);
    }
   
    int stitchCase7(long wordLo,
                    long wordHi,
                    int lengthWordLo,
                    int lengthWordHi) {
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        
        wordLo <<= Long.SIZE - lengthWordLo;
        wordLo = Long.reverse(wordLo);
        wordLo <<= lengthWordHi;
        
        return (int)(wordHi | wordLo);
    }
   
    int stitchCase8(long wordLo,
                    long wordHi,
                    int lengthWordLo,
                    int lengthWordHi) {
        
        wordHi >>>= Long.SIZE - lengthWordHi;
        
        wordLo = Long.reverse(wordLo); // 1000|xxxx
        wordLo >>>= Long.SIZE - lengthWordLo; // 0000|1000
        wordLo >>>= lengthWordLo - lengthWordHi;
        
        return (int)(wordHi | wordLo);
    }
    
    RankSelectBitVector extractBitVector(int i) {
        int startIndex = k * (i / k);
        int endIndex = Math.min(k * (i / k + 1) - 2, maximumBitIndex);
        
        int extractedBitVectorLength = endIndex - startIndex + 1;
        
        RankSelectBitVector extractedBitVector = 
                new RankSelectBitVector(extractedBitVectorLength);
        
        for (int index = extractedBitVectorLength - 1,
                j = startIndex; 
                j <= endIndex;
                j++, index--) {
            
            extractedBitVector.writeBitImpl(index, this.readBitImpl(j));
        }
        
        return extractedBitVector;
    }
    
    // Relies on Long.bitCount (possibly compiled to the POPCOUNT CPU 
    // instruction). Computes the rank of bit vector [startIndex..endIndex].
    int bruteForceRank(int startIndex, int endIndex) {
        if (startIndex > endIndex) {
            return 0;
        }
        
        int startLongIndex = startIndex / Long.SIZE;
        int endLongIndex = endIndex / Long.SIZE;
        int rank = 0; 
        
        for (int longIndex = startLongIndex + 1;
                 longIndex < endLongIndex; 
                 longIndex++) {
            
            rank += Long.bitCount(wordData[longIndex]);
        }
        
        if (startLongIndex != endLongIndex) {
            // Deal with leading bits:
            // 0001....1 | 100....1, endIndex == 64, shift to >>> 63 times
            int numberOfLeadingBits  = startIndex - startLongIndex * Long.SIZE;
            int numberOfTrailingBits = Long.SIZE - (endIndex - endLongIndex * Long.SIZE + 1);

            long word1 = wordData[startLongIndex];
            long word2 = wordData[endLongIndex];
            
            // Clear word1:
            word1 >>>= numberOfLeadingBits;
            word2 <<= numberOfTrailingBits;
            
            rank += Long.bitCount(word1) +
                    Long.bitCount(word2);
        } else {
            // Here, 'startLongIndex == endLongIndex':
            int rangeLength = endIndex - startIndex + 1;
            int numberOfLeadingBits = startIndex - startLongIndex * Long.SIZE;
            int numberOfTrailingBits = Long.SIZE - numberOfLeadingBits 
                                                 - rangeLength;
            
            if (numberOfLeadingBits + numberOfTrailingBits == Long.SIZE) {
                return rank;
            }
            
            // Grab the word:
            long word = wordData[startLongIndex];
            
            word >>>= numberOfLeadingBits;
            word  <<= numberOfLeadingBits + numberOfTrailingBits;
            
            rank += Long.bitCount(word);
        }
        
        return rank;
    }
    
    private void checkNumberOfRequestedBits(int numberOfRequestedBits) {
        if (numberOfRequestedBits == 0) {
            throw new IllegalArgumentException("Requested zero (0) bits.");
        }
        
        if (numberOfRequestedBits < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Requested negative number of bits (%d).", 
                            numberOfRequestedBits));
        }
    }
    
    private static double log2(double v) {
        return Math.log(v) / Math.log(2.0);
    }
}
