#!/usr/bin/env bash
#
# XCimoc APK 一键构建脚本
#
# 流程：
#   1. 编译 QuickJS 模块（生成 AAR）
#   2. 将 AAR 复制到 app/libs（该文件在 .gitignore 中，仓库不包含，app 通过
#      implementation fileTree("libs") 引用，必须先复制才能编译 APK）
#   3. 编译 APK
#
# 用法：
#   ./build.sh            # 默认 release
#   ./build.sh debug      # 编译 debug
#
set -euo pipefail

cd "$(dirname "$0")"

BUILD_TYPE="${1:-release}"

case "$BUILD_TYPE" in
    release|debug) ;;
    *) echo "用法: ./build.sh [release|debug]" >&2; exit 1 ;;
esac

# 任务名首字母大写：release -> assembleRelease / debug -> assembleDebug
TASK="assemble$(echo "${BUILD_TYPE:0:1}" | tr '[:lower:]' '[:upper:]')${BUILD_TYPE:1}"

echo ""
echo "=============================================="
echo " 步骤 1/3：编译 QuickJS 模块 (:quickjs:$TASK)"
echo "=============================================="
./gradlew ":quickjs:$TASK"

echo ""
echo "=============================================="
echo " 步骤 2/3：复制 AAR 到 app/libs"
echo "=============================================="
mkdir -p app/libs
cp "quickjs/build/outputs/aar/quickjs-${BUILD_TYPE}.aar" app/libs/
ls -lh "app/libs/quickjs-${BUILD_TYPE}.aar"

echo ""
echo "=============================================="
echo " 步骤 3/3：编译 APK (:app:$TASK)"
echo "=============================================="
./gradlew ":app:$TASK"

echo ""
echo "=============================================="
echo " 构建完成 ✅  APK 输出目录："
echo "   app/build/outputs/apk/${BUILD_TYPE}/"
echo "=============================================="
