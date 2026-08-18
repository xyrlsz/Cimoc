#ifndef UTF8TO16_H
#define UTF8TO16_H

#include <jni.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Convert UTF-8 to UTF-16.
 *
 * @param src      UTF-8 input buffer
 * @param src_len  input length in bytes
 * @param dst      output UTF-16 buffer, or NULL to calculate length
 *
 * @return number of UTF-16 code units required/written
 *
 * Invalid UTF-8 sequences are replaced with U+FFFD.
 */
size_t utf8_to_utf16(const char *src,
                     size_t src_len,
                     jchar *dst);

#ifdef __cplusplus
}
#endif

#endif /* UTF8TO16_H */