//
// Created by xyrlsz on 2026/8/2.
//

#include "utf8to16.h"

#include <stdint.h>
#include <stddef.h>

#define UNI_REPLACEMENT_CHAR 0xFFFDu

size_t utf8_to_utf16(const char *src,
                     size_t src_len,
                     jchar *dst) {
    if (src == NULL || src_len == 0) {
        return 0;
    }

    const unsigned char *s = (const unsigned char *) src;
    const unsigned char *end = s + src_len;
    size_t out = 0;

    /*
     * ASCII fast path
     *
     * ASCII occupies one UTF-8 byte and one UTF-16 code unit.
     * This is a very common case for HTML / JSON / JavaScript text.
     */
    while (s < end) {
        unsigned char c = *s;

        if (c >= 0x80) {
            break;
        }

        if (dst != NULL) {
            dst[out] = (jchar) c;
        }

        ++out;
        ++s;
    }

    while (s < end) {
        uint32_t cp;
        unsigned char c = *s++;

        /*
         * ASCII
         */
        if (c < 0x80) {
            cp = c;
        }

            /*
             * 2-byte UTF-8
             */
        else if ((c & 0xE0u) == 0xC0u) {
            if (s < end && (s[0] & 0xC0u) == 0x80u) {
                cp = ((uint32_t) (c & 0x1Fu) << 6) |
                     (uint32_t) (s[0] & 0x3Fu);

                /*
                 * Reject overlong encodings:
                 *
                 * C0 80 .. C1 BF
                 */
                if (cp < 0x80u) {
                    cp = UNI_REPLACEMENT_CHAR;
                } else {
                    ++s;
                }
            } else {
                cp = UNI_REPLACEMENT_CHAR;
            }
        }

            /*
             * 3-byte UTF-8
             */
        else if ((c & 0xF0u) == 0xE0u) {
            if ((size_t) (end - s) >= 2 &&
                (s[0] & 0xC0u) == 0x80u &&
                (s[1] & 0xC0u) == 0x80u) {

                cp = ((uint32_t) (c & 0x0Fu) << 12) |
                     ((uint32_t) (s[0] & 0x3Fu) << 6) |
                     (uint32_t) (s[1] & 0x3Fu);

                /*
                 * Reject:
                 *   overlong sequences
                 *   UTF-16 surrogate code points
                 */
                if (cp < 0x800u ||
                    (cp >= 0xD800u && cp <= 0xDFFFu)) {

                    cp = UNI_REPLACEMENT_CHAR;
                } else {
                    s += 2;
                }
            } else {
                cp = UNI_REPLACEMENT_CHAR;
            }
        }

            /*
             * 4-byte UTF-8
             */
        else if ((c & 0xF8u) == 0xF0u) {
            if ((size_t) (end - s) >= 3 &&
                (s[0] & 0xC0u) == 0x80u &&
                (s[1] & 0xC0u) == 0x80u &&
                (s[2] & 0xC0u) == 0x80u) {

                cp = ((uint32_t) (c & 0x07u) << 18) |
                     ((uint32_t) (s[0] & 0x3Fu) << 12) |
                     ((uint32_t) (s[1] & 0x3Fu) << 6) |
                     (uint32_t) (s[2] & 0x3Fu);

                /*
                 * Reject:
                 *   overlong sequences
                 *   code points > U+10FFFF
                 */
                if (cp < 0x10000u ||
                    cp > 0x10FFFFu) {

                    cp = UNI_REPLACEMENT_CHAR;
                } else {
                    s += 3;
                }
            } else {
                cp = UNI_REPLACEMENT_CHAR;
            }
        }

            /*
             * Invalid UTF-8 leading byte
             *
             * 80-BF : stray continuation byte
             * F8-FF : invalid UTF-8
             */
        else {
            cp = UNI_REPLACEMENT_CHAR;
        }

        /*
         * Convert Unicode code point -> UTF-16
         */
        if (cp <= 0xFFFFu) {

            if (dst != NULL) {
                dst[out] = (jchar) cp;
            }

            ++out;

        } else {

            /*
             * U+10000 .. U+10FFFF
             *
             * Convert to UTF-16 surrogate pair.
             */
            cp -= 0x10000u;

            if (dst != NULL) {
                dst[out] =
                        (jchar) (0xD800u + (cp >> 10));

                dst[out + 1] =
                        (jchar) (0xDC00u + (cp & 0x3FFu));
            }

            out += 2;
        }
    }

    return out;
}