//
// Created by xyrlsz on 2026/8/2.
//
#include "utf8to16.h"

#define UNI_REPLACEMENT_CHAR 0xFFFD

size_t utf8_to_utf16(const char *src,
                     size_t src_len,
                     jchar *dst) {
    const unsigned char *s = (const unsigned char *) src;
    const unsigned char *end = s + src_len;
    size_t out = 0;

    while (s < end) {

        uint32_t cp;
        unsigned char c = *s++;

        if (c < 0x80) {
            cp = c;
        } else if ((c & 0xE0) == 0xC0) {

            if (s >= end ||
                (s[0] & 0xC0) != 0x80) {

                cp = UNI_REPLACEMENT_CHAR;
            } else {

                cp = ((c & 0x1F) << 6) |
                     (s[0] & 0x3F);

                if (cp < 0x80)
                    cp = UNI_REPLACEMENT_CHAR;
                else
                    s++;
            }
        } else if ((c & 0xF0) == 0xE0) {

            if (end - s < 2 ||
                (s[0] & 0xC0) != 0x80 ||
                (s[1] & 0xC0) != 0x80) {

                cp = UNI_REPLACEMENT_CHAR;
            } else {

                cp =
                        ((c & 0x0F) << 12) |
                        ((s[0] & 0x3F) << 6) |
                        (s[1] & 0x3F);

                if (cp < 0x800 ||
                    (cp >= 0xD800 && cp <= 0xDFFF))
                    cp = UNI_REPLACEMENT_CHAR;
                else
                    s += 2;
            }
        } else if ((c & 0xF8) == 0xF0) {

            if (end - s < 3 ||
                (s[0] & 0xC0) != 0x80 ||
                (s[1] & 0xC0) != 0x80 ||
                (s[2] & 0xC0) != 0x80) {

                cp = UNI_REPLACEMENT_CHAR;
            } else {

                cp =
                        ((c & 0x07) << 18) |
                        ((s[0] & 0x3F) << 12) |
                        ((s[1] & 0x3F) << 6) |
                        (s[2] & 0x3F);

                if (cp < 0x10000 ||
                    cp > 0x10FFFF)
                    cp = UNI_REPLACEMENT_CHAR;
                else
                    s += 3;
            }
        } else {

            cp = UNI_REPLACEMENT_CHAR;
        }

        if (dst) {

            if (cp <= 0xFFFF) {

                dst[out++] = (jchar) cp;

            } else {

                cp -= 0x10000;

                dst[out++] =
                        (jchar) (0xD800 + (cp >> 10));

                dst[out++] =
                        (jchar) (0xDC00 + (cp & 0x3FF));
            }

        } else {

            out += (cp <= 0xFFFF) ? 1 : 2;
        }
    }

    return out;
}