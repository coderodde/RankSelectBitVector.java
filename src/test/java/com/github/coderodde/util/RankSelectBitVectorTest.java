package com.github.coderodde.util;

import java.util.Random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public final class RankSelectBitVectorTest {
    
    private static final long SEED = 1706524892441L; // System.currentTimeMillis();
    
    static {
        System.out.printf("Seed = %d.\n", SEED);
    }
    
    @Test
    public void smallSelects() {
        RankSelectBitVector bv = new RankSelectBitVector(100);
        
        bv.writeBitOn(10);
        bv.writeBitOn(20);
        bv.writeBitOn(30);
        bv.writeBitOn(40);
        
        bv.buildIndices();
        
        assertEquals(10, bv.selectFirst(1));
        assertEquals(20, bv.selectFirst(2));
        assertEquals(30, bv.selectFirst(3));
        assertEquals(40, bv.selectFirst(4));
        
        assertEquals(10, bv.selectSecond(1));
        assertEquals(20, bv.selectSecond(2));
        assertEquals(30, bv.selectSecond(3));
        assertEquals(40, bv.selectSecond(4));
        
        assertEquals(10, bv.selectThird(1));
        assertEquals(20, bv.selectThird(2));
        assertEquals(30, bv.selectThird(3));
        assertEquals(40, bv.selectThird(4));
        
        bv.writeBitOn(35);
        
        assertEquals(10, bv.selectThird(1));
        assertEquals(20, bv.selectThird(2));
        assertEquals(30, bv.selectThird(3));
        assertEquals(35, bv.selectThird(4));
        assertEquals(40, bv.selectThird(5));
        
        bv.writeBitOn(63);
        bv.writeBitOn(64);
        bv.writeBitOn(65);
        
        assertEquals(10, bv.selectThird(1));
        assertEquals(20, bv.selectThird(2));
        assertEquals(30, bv.selectThird(3));
        assertEquals(35, bv.selectThird(4));
        assertEquals(40, bv.selectThird(5));
        assertEquals(63, bv.selectThird(6));
        assertEquals(64, bv.selectThird(7));
        assertEquals(65, bv.selectThird(8));
    }
    
    @Test
    public void largeRankFirst() {
        RankSelectBitVector bv = new RankSelectBitVector(2001);
        
        bv.writeBitOn(1000);
        bv.writeBitOn(1001);
        bv.writeBitOn(1003);
        
        assertEquals(3, bv.rankFirst(1010));
        assertEquals(3, bv.rankSecond(1010));
        assertEquals(3, bv.rankThird(1010));
    }
    
    @Test
    public void lastBitRank() {
        RankSelectBitVector bv = new RankSelectBitVector(8);
        
        bv.writeBitOn(2);
        bv.writeBitOn(6);
        bv.writeBitOn(7);
        
        assertEquals(3, bv.rankFirst(8));
        assertEquals(3, bv.rankSecond(8));
        assertEquals(3, bv.rankThird(8));
    }
    
    @Test
    public void smallSelect() {
        RankSelectBitVector bv = new RankSelectBitVector(8);
        
        bv.writeBitOn(2);
        bv.writeBitOn(4);
        bv.writeBitOn(5);
        bv.writeBitOn(7);
        
        // 00101101
        // select(1) = 2
        // select(2) = 4
        // select(3) = 5
        // select(4) = 7
        
        assertEquals(2, bv.selectFirst(1));
        assertEquals(4, bv.selectFirst(2));
        assertEquals(5, bv.selectFirst(3));
        assertEquals(7, bv.selectFirst(4));
    }
    
    @Test
    public void debugTest1RankFirst() {
        // 00101101
        RankSelectBitVector bv = new RankSelectBitVector(8);
        
        bv.writeBitOn(2);
        bv.writeBitOn(4);
        bv.writeBitOn(5);
        bv.writeBitOn(7);
        
        assertEquals(0, bv.rankFirst(0));
        assertEquals(0, bv.rankFirst(1));
        assertEquals(0, bv.rankFirst(2));
        assertEquals(1, bv.rankFirst(3));
        assertEquals(1, bv.rankFirst(4));
        assertEquals(2, bv.rankFirst(5));
        assertEquals(3, bv.rankFirst(6));
        assertEquals(3, bv.rankFirst(7));
        assertEquals(4, bv.rankFirst(8));
        
        assertEquals(2, bv.selectFirst(1));
        assertEquals(4, bv.selectFirst(2));
        assertEquals(5, bv.selectFirst(3));
        assertEquals(7, bv.selectFirst(4));
    }
    
    @Test
    public void debugTest2RankFirst() {
        // 00101101 10101101
        RankSelectBitVector bv = new RankSelectBitVector(16);
        
        bv.writeBitOn(2);
        bv.writeBitOn(4);
        bv.writeBitOn(5);
        bv.writeBitOn(7);
        
        bv.writeBitOn(8);
        bv.writeBitOn(10);
        bv.writeBitOn(12);
        bv.writeBitOn(13);
        bv.writeBitOn(15);
        
        assertEquals(0, bv.rankFirst(0));
        assertEquals(0, bv.rankFirst(1));
        assertEquals(0, bv.rankFirst(2));
        assertEquals(1, bv.rankFirst(3));
        assertEquals(1, bv.rankFirst(4));
        assertEquals(2, bv.rankFirst(5));
        assertEquals(3, bv.rankFirst(6));
        assertEquals(3, bv.rankFirst(7));
        assertEquals(4, bv.rankFirst(8));
        
        assertEquals(5, bv.rankFirst(9));
        assertEquals(5, bv.rankFirst(10));
        assertEquals(6, bv.rankFirst(11));
        assertEquals(6, bv.rankFirst(12));
        assertEquals(7, bv.rankFirst(13));
        assertEquals(8, bv.rankFirst(14));
        assertEquals(8, bv.rankFirst(15));
        assertEquals(9, bv.rankFirst(16));
        
        assertEquals(2, bv.selectFirst(1));
        assertEquals(4, bv.selectFirst(2));
        assertEquals(5, bv.selectFirst(3));
        assertEquals(7, bv.selectFirst(4));
        
        assertEquals(8, bv.selectFirst(5));
        assertEquals(10, bv.selectFirst(6));
        assertEquals(12, bv.selectFirst(7));
        assertEquals(13, bv.selectFirst(8));
        assertEquals(15, bv.selectFirst(9));
    }
    
    @Test
    public void debugTest1() {
        // 00101101
        RankSelectBitVector bv = new RankSelectBitVector(8);
        
        bv.writeBitOn(2);
        bv.writeBitOn(4);
        bv.writeBitOn(5);
        bv.writeBitOn(7);
        
        assertEquals(0, bv.rankThird(0));
        assertEquals(0, bv.rankThird(1));
        assertEquals(0, bv.rankThird(2));
        assertEquals(1, bv.rankThird(3));
        assertEquals(1, bv.rankThird(4));
        assertEquals(2, bv.rankThird(5));
        assertEquals(3, bv.rankThird(6));
        assertEquals(3, bv.rankThird(7));
        assertEquals(4, bv.rankThird(8));
        
        assertEquals(2, bv.selectFirst(1));
        assertEquals(4, bv.selectFirst(2));
        assertEquals(5, bv.selectFirst(3));
        assertEquals(7, bv.selectFirst(4));
    }
    
    @Test
    public void debugTest2() {
        // 00101101 10101101
        RankSelectBitVector bv = new RankSelectBitVector(16);
        
        bv.writeBitOn(2);
        bv.writeBitOn(4);
        bv.writeBitOn(5);
        bv.writeBitOn(7);
        
        bv.writeBitOn(8);
        bv.writeBitOn(10);
        bv.writeBitOn(12);
        bv.writeBitOn(13);
        bv.writeBitOn(15);
        
        assertEquals(0, bv.rankThird(0));
        assertEquals(0, bv.rankThird(1));
        assertEquals(0, bv.rankThird(2));
        assertEquals(1, bv.rankThird(3));
        assertEquals(1, bv.rankThird(4));
        assertEquals(2, bv.rankThird(5));
        assertEquals(3, bv.rankThird(6));
        assertEquals(3, bv.rankThird(7));
        assertEquals(4, bv.rankThird(8));
        
        assertEquals(5, bv.rankThird(9));
        assertEquals(5, bv.rankThird(10));
        assertEquals(6, bv.rankThird(11));
        assertEquals(6, bv.rankThird(12));
        assertEquals(7, bv.rankThird(13));
        assertEquals(8, bv.rankThird(14));
        assertEquals(8, bv.rankThird(15));
        assertEquals(9, bv.rankThird(16));
        
        assertEquals(2, bv.selectFirst(1));
        assertEquals(4, bv.selectFirst(2));
        assertEquals(5, bv.selectFirst(3));
        assertEquals(7, bv.selectFirst(4));
        
        assertEquals(8, bv.selectFirst(5));
        assertEquals(10, bv.selectFirst(6));
        assertEquals(12, bv.selectFirst(7));
        assertEquals(13, bv.selectFirst(8));
        assertEquals(15, bv.selectFirst(9));
    }
    
    @Test
    public void debugTest3() {
        // 00101101 10101101 00010010
        RankSelectBitVector bv = new RankSelectBitVector(24);
        
        bv.writeBitOn(2);
        bv.writeBitOn(4);
        bv.writeBitOn(5);
        bv.writeBitOn(7);
        
        bv.writeBitOn(8);
        bv.writeBitOn(10);
        bv.writeBitOn(12);
        bv.writeBitOn(13);
        bv.writeBitOn(15);
        
        bv.writeBitOn(19);
        bv.writeBitOn(22);
        
        assertEquals(0, bv.rankThird(0));
        assertEquals(0, bv.rankThird(1));
        assertEquals(0, bv.rankThird(2));
        assertEquals(1, bv.rankThird(3));
        assertEquals(1, bv.rankThird(4));
        assertEquals(2, bv.rankThird(5));
        assertEquals(3, bv.rankThird(6));
        assertEquals(3, bv.rankThird(7));
        assertEquals(4, bv.rankThird(8));
        
        assertEquals(5, bv.rankThird(9));
        assertEquals(5, bv.rankThird(10));
        assertEquals(6, bv.rankThird(11));
        assertEquals(6, bv.rankThird(12));
        assertEquals(7, bv.rankThird(13));
        assertEquals(8, bv.rankThird(14));
        assertEquals(8, bv.rankThird(15));
        assertEquals(9, bv.rankThird(16));
        
        // 00010010
        assertEquals(9, bv.rankThird(17));
        assertEquals(9, bv.rankThird(18));
        assertEquals(9, bv.rankThird(19));
        assertEquals(10, bv.rankThird(20));
        assertEquals(10, bv.rankThird(21));
        assertEquals(10, bv.rankThird(22));
        assertEquals(11, bv.rankThird(23));
        assertEquals(11, bv.rankThird(24));
        
        // select():
        assertEquals(2, bv.selectFirst(1));
        assertEquals(4, bv.selectFirst(2));
        assertEquals(5, bv.selectFirst(3));
        assertEquals(7, bv.selectFirst(4));
        
        assertEquals(8, bv.selectFirst(5));
        assertEquals(10, bv.selectFirst(6));
        assertEquals(12, bv.selectFirst(7));
        assertEquals(13, bv.selectFirst(8));
        assertEquals(15, bv.selectFirst(9));
        
        assertEquals(19, bv.selectFirst(10));
        assertEquals(22, bv.selectFirst(11));
    }
    
    @Test
    public void bruteForceTest() {
        Random random = new Random(SEED);
        RankSelectBitVector bv = getRandomBitVector(random);
        
        // Preprocess the data:
        bv.buildIndices();
       
        int numberOfOneBits = bv.rankThird(bv.getNumberOfSupportedBits());
        
        for (int i = 0; i <= bv.getNumberOfSupportedBits(); i++) {
            int rank1 = bv.rankFirst(i);
            int rank2 = bv.rankSecond(i);
            int rank3 = bv.rankThird(i);
            
            int selectIndex = random.nextInt(numberOfOneBits) + 1;
            int select1 = bv.selectFirst(selectIndex);
            int select2 = bv.selectSecond(selectIndex);
            int select3 = bv.selectThird(selectIndex);

            if (rank3 != rank2) {
                System.out.printf(
                        "ERROR: i = %d, rank1 = %d, rank2 = %d, rank3 = %d.\n",
                        i,
                        rank1,
                        rank2,
                        rank3);
                
                System.exit(-1);
            }
            
            assertEquals(rank1, rank2);
            assertEquals(rank2, rank3);
//            assertEquals(select2, select1);
//            assertEquals(select2, select3);
        }
    }
    
    private static RankSelectBitVector getRandomBitVector(Random random) {
        RankSelectBitVector bv = new RankSelectBitVector(537_113);
        
        for (int i = 0; i < bv.getNumberOfSupportedBits(); i++) {
            if (random.nextDouble() < 0.3) {
                bv.writeBitOn(i);
            }
        }
        
        return bv;
    }
    
    private static BruteForceBitVector copy(RankSelectBitVector bv) {
        BruteForceBitVector referenceBv = 
                new BruteForceBitVector(bv.getNumberOfSupportedBits());
        
        for (int i = 0; i < bv.getNumberOfSupportedBits(); i++) {
            if (bv.readBit(i)) {
                referenceBv.writeBitOn(i);
            }
        }
        
        return referenceBv;
    }
    
    @Test
    public void toInteger() {
        RankSelectBitVector bitVector = new RankSelectBitVector(31);
        assertEquals(0, bitVector.toInteger(20));
        
        bitVector.writeBit(1, true);
        assertEquals(2, bitVector.toInteger(20));
        
        bitVector.writeBit(2, true);
        assertEquals(6, bitVector.toInteger(20));
        
        bitVector.writeBit(4, true);
        assertEquals(22, bitVector.toInteger(20));
    }
    
    @Test
    public void readWriteBit() {
        RankSelectBitVector bitVector = new RankSelectBitVector(30);
        bitVector.writeBit(12, true);
        assertTrue(bitVector.readBit(12));
        bitVector.writeBit(12, false);
        assertFalse(bitVector.readBit(12));
        assertFalse(bitVector.readBit(13));
    }
    
    @Test
    public void bruteForceBitVectorSelect() {
        BruteForceBitVector bv = new BruteForceBitVector(8);
        
        bv.writeBitOn(2);
        bv.writeBitOn(4);
        bv.writeBitOn(6);
        bv.writeBitOn(7);
        
        assertEquals(2, bv.select(1));
        assertEquals(4, bv.select(2));
        assertEquals(6, bv.select(3));
        assertEquals(7, bv.select(4));
    }
    
    @Test
    public void countSetBits() {
        RankSelectBitVector bv = new RankSelectBitVector(11);
        
        assertEquals(0, bv.getNumberOfSetBits());
        
        bv.writeBitOn(10);
        
        assertEquals(1, bv.getNumberOfSetBits());
        
        bv.writeBitOn(5);
        
        assertEquals(2, bv.getNumberOfSetBits());
        
        bv.writeBitOff(10);
        
        assertEquals(1, bv.getNumberOfSetBits());
        
        bv.writeBitOff(5);
        
        assertEquals(0, bv.getNumberOfSetBits());
    }
    
    @Rule
    public TestRule watchman = new TestWatcher() {
      
        @Override
        protected void failed(Throwable e, Description desc) {
            System.err.printf("Failed on SEED = %d", SEED);
        }
    };
}
