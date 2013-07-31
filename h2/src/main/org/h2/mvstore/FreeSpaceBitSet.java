/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore;

import java.util.BitSet;

import org.h2.util.MathUtils;

/**
 * A free space bit set.
 */
public class FreeSpaceBitSet {

    /**
     * The first usable block.
     */
    private final int firstFreeBlock;

    /**
     * The block size in bytes.
     */
    private final int blockSize;

    /**
     * The bit set.
     */
    private final BitSet set = new BitSet();

    /**
     * Create a new free space map.
     *
     * @param firstFreeBlock the first free block
     * @param blockSize the block size
     */
    public FreeSpaceBitSet(int firstFreeBlock, int blockSize) {
        this.firstFreeBlock = firstFreeBlock;
        this.blockSize = blockSize;
        clear();
    }

    /**
     * Reset the list.
     */
    public synchronized void clear() {
        set.clear();
        set.set(0, firstFreeBlock);
    }

    /**
     * Check whether one of the blocks is in use.
     *
     * @param pos the position in bytes
     * @param length the number of bytes
     * @return true if a block is in use
     */
    public synchronized boolean isUsed(long pos, int length) {
        int start = getBlock(pos);
        int blocks = getBlockCount(length);
        for (int i = start; i < start + blocks; i++) {
            if (!set.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether one of the blocks is free.
     *
     * @param pos the position in bytes
     * @param length the number of bytes
     * @return true if a block is free
     */
    public synchronized boolean isFree(long pos, int length) {
        int start = getBlock(pos);
        int blocks = getBlockCount(length);
        for (int i = start; i < start + blocks; i++) {
            if (set.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Allocate a number of blocks and mark them as used.
     *
     * @param length the number of bytes to allocate
     * @return the start position in bytes
     */
    public synchronized long allocate(int length) {
        int blocks = getBlockCount(length);
        for (int i = 0;;) {
            int start = set.nextClearBit(i);
            int end = set.nextSetBit(start + 1);
            if (end < 0 || end - start >= blocks) {
                set.set(start, start + blocks);
                return getPos(start);
            }
            i = end;
        }
    }

    /**
     * Mark the space as in use.
     *
     * @param pos the position in bytes
     * @param length the number of bytes
     */
    public synchronized void markUsed(long pos, int length) {
        int start = getBlock(pos);
        int blocks = getBlockCount(length);
        set.set(start, start + blocks);
    }

    /**
     * Mark the space as free.
     *
     * @param pos the position in bytes
     * @param length the number of bytes
     */
    public synchronized void free(long pos, int length) {
        int start = getBlock(pos);
        int blocks = getBlockCount(length);
        set.clear(start, start + blocks);
    }

    private long getPos(int block) {
        return (long) block * (long) blockSize;
    }

    private int getBlock(long pos) {
        return (int) (pos / blockSize);
    }

    private int getBlockCount(int length) {
        return MathUtils.roundUpInt(length, blockSize) / blockSize;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("[");
        for (int i = 0;;) {
            if (i > 0) {
                buff.append(", ");
            }
            int start = set.nextClearBit(i);
            buff.append(start).append('-');
            int end = set.nextSetBit(start + 1);
            if (end < 0) {
                break;
            }
            buff.append(end - 1);
            i = end + 1;
        }
        return buff.append(']').toString();
    }

}