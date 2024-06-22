#include <time.h>
#include "photon.h"
#include "photon.c"

#if defined(_PHOTON160_)
#define DigestByteSize 20
#define BlockSize 5

#elif defined(_PHOTON224_)
#define DigestByteSize 28
#define BlockSize 4

#elif defined(_PHOTON256_)
#define DigestByteSize 32
#define BlockSize 4
#endif

void hmacPhoton(int *key, int keylenbytes, int *msg, int msglenbytes, int *hmacDigest)
{
    int BLOCK_SIZE_BYTES = BlockSize;
    int HASH_SIZE_BYTES = DigestByteSize;
    int i_key_pad[BLOCK_SIZE_BYTES];
    int o_key_pad[BLOCK_SIZE_BYTES];
    int i_pad_msg[BLOCK_SIZE_BYTES + msglenbytes];
    int inner_hash[HASH_SIZE_BYTES];
    int o_pad_inner[BLOCK_SIZE_BYTES + HASH_SIZE_BYTES];
    int key_hash[HASH_SIZE_BYTES];
    int ipad = 0x36;
    int opad = 0x5c;
    if (keylenbytes > BLOCK_SIZE_BYTES)
    {
        hash(key, keylenbytes, key_hash, HASH_SIZE_BYTES);
        keylenbytes = HASH_SIZE_BYTES;
    }
    else
    {
        memcpy(key_hash, key, keylenbytes*sizeof(int));
    }
    for (int i = 0; i < BLOCK_SIZE_BYTES; ++i)
    {
        i_key_pad[i] = (i < keylenbytes) ? key_hash[i] ^ ipad : ipad;
        o_key_pad[i] = (i < keylenbytes) ? key_hash[i] ^ opad : opad;
    }

    memcpy(i_pad_msg, i_key_pad, BLOCK_SIZE_BYTES * sizeof(int));
    memcpy(i_pad_msg + BLOCK_SIZE_BYTES, msg, msglenbytes* sizeof(int));
    hash(i_pad_msg, (BLOCK_SIZE_BYTES + msglenbytes), inner_hash, HASH_SIZE_BYTES);


    memcpy(o_pad_inner, o_key_pad, BLOCK_SIZE_BYTES * sizeof(int));
    memcpy(o_pad_inner + BLOCK_SIZE_BYTES, inner_hash, HASH_SIZE_BYTES * sizeof(int));
    hash(o_pad_inner, (BLOCK_SIZE_BYTES + HASH_SIZE_BYTES), hmacDigest, HASH_SIZE_BYTES);

}

int getTOTP(char* keystring)
{
    int hmacDigest[DigestByteSize];

    clock_t begin = clock();

    int keylenbytes = strlen(keystring);
    int key[keylenbytes];
    for (int i=0;i<keylenbytes;i++) key[i]=keystring[i];
    int timestep = 30;
    unsigned long T = (unsigned long)time(NULL);

    T /= timestep;
    int msg[8];
    msg[0] = (T >> 56) & 0xFF;
    msg[1] = (T >> 48) & 0xFF;
    msg[2] = (T >> 40) & 0xFF;
    msg[3] = (T >> 32) & 0xFF;
    msg[4] = (T >> 24) & 0xFF;
    msg[5] = (T >> 16) & 0xFF;
    msg[6] = (T >> 8) & 0xFF;
    msg[7] = T & 0xFF;
    int msglenbytes = 8;
    hmacPhoton(key, keylenbytes, msg, msglenbytes, hmacDigest);
    int offset = hmacDigest[DigestByteSize - 1] & 0xf;
    int binary =
            ((hmacDigest[offset] & 0x7f) << 24) |
            ((hmacDigest[offset + 1] & 0xff) << 16) |
            ((hmacDigest[offset + 2] & 0xff) << 8) |
            (hmacDigest[offset + 3] & 0xff);
    int totp = binary % 1000000;
    return totp;


}

int main(){
    char key[] = "abc";
    int totp = getTOTP(key);
    printf("%d", totp);
}