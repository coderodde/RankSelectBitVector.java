package com.github.coderodde.util.benchmark;

import com.github.coderodde.util.RankSelectBitVector;
import java.util.Random;

public final class RankSelectBitVectorBenchmark {
    
    /**
     * The number of bits in the benchmark bit vector.
     */
    private static final int BIT_VECTOR_LENGTH = 1_000;
    
    public static void main(String[] args) {
        long seed = parseSeed(args);
        
        System.out.printf("Seed = %d\n", seed);
        Random random = new Random(seed);
        
        long st = System.currentTimeMillis();
        
        RankSelectBitVector rankSelectBitVector = createRandomBitVector(random);
        
        System.out.printf("Built the bit vector in %d milliseconds.\n",
                          System.currentTimeMillis() - st);
        
        st = System.currentTimeMillis(); // st - start time.
        
        rankSelectBitVector.buildIndices();
        
        System.out.printf("Preprocessed the bit vector in %d milliseconds.\n",
                          System.currentTimeMillis() - st);
        
        System.out.println("--- Benchmarking rank operation ---");
        
        benchmarkRanks(rankSelectBitVector);
        
        System.out.println("--- Benchmarking select operation ---");
        
        benchmarkSelects(rankSelectBitVector);
    }
    
    private static RankSelectBitVector createRandomBitVector(Random random) {
        RankSelectBitVector rankSelectBitVector =
                new RankSelectBitVector(BIT_VECTOR_LENGTH);
        
        for (int bitIndex = 0;
                bitIndex != rankSelectBitVector.getNumberOfSupportedBits(); 
                bitIndex++) {
            
            if (random.nextBoolean()) {
                rankSelectBitVector.writeBitOn(bitIndex);
            }
        }
        
        return rankSelectBitVector;
    }
    
    private static boolean rankArraysEqual(int[] rankArray1, int[] rankArray2) {
        if (rankArray1.length != rankArray2.length) {
            throw new IllegalArgumentException("Rank array length mismatch.");
        }
        
        int n = Math.max(rankArray1.length, rankArray2.length);
        
        for (int i = 0; i != n; i++) {
            int rank1 = rankArray1[i];
            int rank2 = rankArray2[i];
            
            if (rank1 != rank2) {
                System.err.printf(
                        "ERROR: Mismatch at index = %d, " + 
                        "rank1 = %d, rank2 = %d.\n",
                        i,
                        rank1,
                        rank2);
                
                return false;
            }
        }
        
        return true;
    }
    
    private static void
         benchmarkRanks(RankSelectBitVector rankSelectBitVector) {
        
        int numberOfBits = rankSelectBitVector.getNumberOfSupportedBits();
        
        int[] answers1 = new int[numberOfBits];
        int[] answers2 = new int[numberOfBits];
        int[] answers3 = new int[numberOfBits];
        
        long st = System.currentTimeMillis(); // st - start time.
        
        for (int i = 0; i != numberOfBits; i++) {
            answers1[i] = rankSelectBitVector.rankFirst(i);
        }
        
        long answersDuration1 = System.currentTimeMillis() - st;
        
        System.out.printf(
                "rankFirst() ran for %d milliseconds.\n", 
                answersDuration1);
        
        st = System.currentTimeMillis();
        
        for (int i = 0; i != numberOfBits; i++) {
            answers2[i] = rankSelectBitVector.rankSecond(i);
        }
        
        long answersDuration2 = System.currentTimeMillis() - st;
        
        System.out.printf(
                "rankSecond() ran for %d milliseconds.\n",
                answersDuration2);
        
        st = System.currentTimeMillis();
        
        for (int i = 0; i != numberOfBits; i++) {
            answers3[i] = rankSelectBitVector.rankThird(i);
        }
        
        long answersDuration3 = System.currentTimeMillis() - st;
        
        System.out.printf(
                "rankThird() ran for %d milliseconds.\n",
                answersDuration3);
        
        if (!rankArraysEqual(answers1, answers2)) {
            System.err.println("Failed on rankFirst vs. rankSecond.");
            return;
        }
        
        if (!rankArraysEqual(answers1, answers3)) {
            System.err.println("Failed on rankFirst vs. rankThird.");
        }
    }
         
    private static void
         benchmarkSelects(RankSelectBitVector rankSelectBitVector) {
        int numberOfSetBits = rankSelectBitVector.getNumberOfSetBits();
        
        int[] answers1 = new int[numberOfSetBits + 1];
        int[] answers2 = new int[numberOfSetBits + 1];
        int[] answers3 = new int[numberOfSetBits + 1];
        
        long st = System.currentTimeMillis();
        
        for (int i = 1; i <= numberOfSetBits; i++) {
            answers1[i] = rankSelectBitVector.selectFirst(i);
        }
        
        long answersDuration1 = System.currentTimeMillis() - st;
        
        System.out.printf(
                "selectFirst() ran for %d milliseconds.\n", 
                answersDuration1);
        
        st = System.currentTimeMillis();
        
        for (int i = 1; i <= numberOfSetBits; i++) {
            answers2[i] = rankSelectBitVector.selectSecond(i);
        }
        
        long answersDuration2 = System.currentTimeMillis() - st;
        
        System.out.printf(
                "selectSecond() ran for %d milliseconds.\n",
                answersDuration2);
        
        st = System.currentTimeMillis();
        
        for (int i = 1; i <= numberOfSetBits; i++) {
            answers3[i] = rankSelectBitVector.selectThird(i);
        }
        
        long answersDuration3 = System.currentTimeMillis() - st;
        
        System.out.printf(
                "selectThird() ran for %d milliseconds.\n",
                answersDuration3);
        
        if (!rankArraysEqual(answers1, answers2)) {
            System.err.println("Failed on selectFirst vs. selectSecond.");
            return;
        }
        
        if (!rankArraysEqual(answers1, answers3)) {
            System.err.println("Failed on selectFirst vs. selectThird.");
        }
    }
         
    private static long parseSeed(String[] args) {
        if (args.length == 0) {
            return System.currentTimeMillis();
        }
        
        try {
            return Long.parseLong(args[0]);
        } catch (NumberFormatException ex) {
            System.err.printf(
                    "WARNING: Could not parse '%s' as an long value.", args[0]);
            
            return System.currentTimeMillis();
        }
    }
}
