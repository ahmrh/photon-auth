package com.ahmrh.amryauth.common;

import java.time.Instant;


/*
 * Author:
 * Amry Yahya
 * https://github.com/amryyahya
 */

class TOTP {
    private static final int D = 6;
    private static final int S = 8;
    private static final int N = 32;
    private static final int R = 4;
    private static final int DigestByteSize = 32;
    private static final int BlockSize = 4;
    private static final int[] IC = { 0, 1, 3, 7, 6, 4 };
    private static final int[][] M = {
            { 2, 3, 1, 2, 1, 4 },
            { 8, 14, 7, 9, 6, 17 },
            { 34, 59, 31, 37, 24, 66 },
            { 132, 228, 121, 155, 103, 11 },
            { 22, 153, 239, 111, 144, 75 },
            { 150, 203, 210, 121, 36, 167 }
    };
    private static final int ReductionPoly = 0x1b;
    private static final int padVal = 0x80;

    private static final int[] sbox = {
            0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
            0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
            0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
            0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
            0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
            0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
            0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
            0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
            0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
            0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
            0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
            0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
            0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
            0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
            0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
            0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16 };

    private static final int[] RC = { 1, 3, 7, 14, 13, 11, 6, 12, 9, 2, 5, 10 };

    private static int fieldMult(int a, int b) {
        int x = a, ret = 0;
        int i;
        for (i = 0; i < S; i++) {
            if ((b >> i & 1) == 1) {
                ret ^= x;
            }
            if ((x >> (S - 1) & 1) == 1) {
                x <<= 1;
                x ^= ReductionPoly;
            } else {
                x <<= 1;
            }
        }
        return ret & (1 << S) - 1;
    }

    private static void addConstant(int[][] State, int round) {
        for (int i = 0; i < D; i++) {
            State[i][0] = State[i][0] ^ RC[round] ^ IC[i];
        }
    }

    private static void subCells(int[][] State) {
        for (int i = 0; i < D; i++)
            for (int j = 0; j < D; j++)
                State[i][j] = sbox[State[i][j]];
    }

    private static void shiftRows(int[][] State) {
        int i, j;
        int tmp[] = new int[D];
        for (i = 1; i < D; i++) {
            for (j = 0; j < D; j++)
                tmp[j] = State[i][j];
            for (j = 0; j < D; j++)
                State[i][j] = tmp[(j + i) % D];
        }
    }

    private static void mixColumnsSerial(int[][] State) {
        int tmp[] = new int[D];

        for (int j = 0; j < D; j++) {
            for (int i = 0; i < D; i++) {
                int sum = 0;
                for (int k = 0; k < D; k++) {
                    sum ^= fieldMult(M[i][k], State[k][j]);
                }
                tmp[i] = sum;
            }
            for (int i = 0; i < D; i++) {
                State[i][j] = tmp[i];
            }
        }
    }

    private static void permutation(int[][] State) {
        for (int i = 0; i < 12; i++) {
            addConstant(State, i);
            subCells(State);
            shiftRows(State);
            mixColumnsSerial(State);
        }
    }

    private static int[] photon(int[] msg) {
        int[][] State = {
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0x40, 0x20, 0x20 }

        };
        int remains = R - (msg.length % R);
        int paddedLength = msg.length + remains;
        int[] paddedMsg = new int[paddedLength];
        System.arraycopy(msg, 0, paddedMsg, 0, msg.length);
        paddedMsg[msg.length] = padVal;
        int i = 0;
        while (i < paddedLength) {
            for (int j = 0; j < R; j++) {
                State[0][j] ^= paddedMsg[i++];
            }

            permutation(State);
        }
        i = 0;
        int digestLength = 0;
        int[] digest = new int[N];
        while (digestLength < N) {
            for (int j = 0; j < R; j++) {
                digest[i++] = State[0][j];
            }
            permutation(State);
            digestLength += R;
        }
        return digest;
    }

    private static int[] hmacPhoton(int key[], int[] msg) {
        int[] i_key_pad = new int[BlockSize];
        int[] o_key_pad = new int[BlockSize];
        int[] i_pad_msg = new int[BlockSize + msg.length];
        int[] inner_hash = new int[DigestByteSize];
        int[] o_pad_inner = new int[BlockSize + DigestByteSize];
        int[] key_hash = new int[DigestByteSize];
        int ipad = 0x36;
        int opad = 0x5c;
        if (key.length > BlockSize) key_hash = photon(key);
        else System.arraycopy(key, 0, key_hash, 0, key.length);
        for (int i = 0; i < BlockSize; i++) {
            i_key_pad[i] = (i < key.length) ? key_hash[i] ^ ipad : ipad;
            o_key_pad[i] = (i < key.length) ? key_hash[i] ^ opad : opad;
        }
        System.arraycopy(i_key_pad, 0, i_pad_msg, 0, BlockSize);
        System.arraycopy(msg, 0, i_pad_msg, BlockSize, msg.length);
        inner_hash = photon(i_pad_msg);
        System.arraycopy(o_key_pad, 0, o_pad_inner, 0, BlockSize);
        System.arraycopy(inner_hash, 0, o_pad_inner, BlockSize, DigestByteSize);
        return photon(o_pad_inner);
    }

     static int getTOTP(String keystring) {
        int[] key = keystring.chars().toArray();
        int timestep = 30;
        long T = Instant.now().getEpochSecond();
        T /= timestep;
        int[] msg = new int[8];
        msg[0] = (int) (T >> 56) & 0xFF;
        msg[1] = (int) (T >> 48) & 0xFF;
        msg[2] = (int) (T >> 40) & 0xFF;
        msg[3] = (int) (T >> 32) & 0xFF;
        msg[4] = (int) (T >> 24) & 0xFF;
        msg[5] = (int) (T >> 16) & 0xFF;
        msg[6] = (int) (T >> 8) & 0xFF;
        msg[7] = (int) T & 0xFF;
        int[] hmacDigest = hmacPhoton(key, msg);
        int offset = hmacDigest[DigestByteSize - 1] & 0xf;
        int totp = ((hmacDigest[offset] & 0x7f) << 24) |
                ((hmacDigest[offset + 1] & 0xff) << 16) |
                ((hmacDigest[offset + 2] & 0xff) << 8) |
                (hmacDigest[offset + 3] & 0xff);
        return totp % 1000000;
    }
}
