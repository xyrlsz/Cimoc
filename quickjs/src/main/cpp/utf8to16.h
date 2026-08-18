//
// Created by xyrlsz on 2026/8/2.
//

#ifndef XCIMOC_UTF8TO16_H
#define XCIMOC_UTF8TO16_H

#include <jni.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>

size_t utf8_to_utf16(const char *src,
                     size_t src_len,
                     jchar *dst);

#endif //XCIMOC_UTF8TO16_H
