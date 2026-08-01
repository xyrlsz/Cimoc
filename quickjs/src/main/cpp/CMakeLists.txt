cmake_minimum_required(VERSION 3.22.1)

project("quickjs_jni")

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# ============================================================
# QuickJS JavaScript 引擎（官方源码 https://github.com/bellard/quickjs）
#
# 仅编译运行 JS 所需的核心源文件。刻意【不】编译 quickjs-libc.c，
# 因此脚本无法使用 std/os 模块（无文件系统、进程等宿主能力），
# 相比 Rhino 的 ClassShutter 屏蔽方案，这里从根上杜绝了越权访问。
#
# 说明：2026-06-04 版本的 BigInt 已集成进 quickjs.c，
# 不再需要 libbf.c；dtoa.c 为浮点打印/解析库。
# ============================================================
add_library(quickjs STATIC
        quickjs/quickjs.c
        quickjs/dtoa.c
        quickjs/libregexp.c
        quickjs/libunicode.c
        quickjs/cutils.c
)

target_include_directories(quickjs PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/quickjs)

target_compile_definitions(quickjs PRIVATE
        _GNU_SOURCE
        CONFIG_VERSION="2026-06-04"
)

target_compile_options(quickjs PRIVATE
        -fwrapv
        -funsigned-char
        -Wno-sign-compare
        -Wno-unused-parameter
        -Wno-missing-field-initializers
)

# ============================================================
# JNI 封装层
# ============================================================
add_library(quickjs_jni SHARED
        quickjs_jni.c
)

target_include_directories(quickjs_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/quickjs)

target_link_libraries(quickjs_jni
        quickjs
        log
        m
        atomic
)
