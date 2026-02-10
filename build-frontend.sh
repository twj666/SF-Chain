#!/bin/bash

# SF-Chain前端构建脚本
set -e

echo "🚀 开始构建SF-Chain前端..."

# 定义路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "$SCRIPT_DIR/sf-chain-config-center-frontend" && pwd)"
STATIC_DIR="$SCRIPT_DIR/sf-chain-config-center-server/src/main/resources/static"
DIST_DIR="$FRONTEND_DIR/dist"

# 检查前端目录是否存在
if [ ! -d "$FRONTEND_DIR" ]; then
    echo "❌ 前端目录不存在: $FRONTEND_DIR"
    echo "请确保前端工程目录存在: ./sf-chain-config-center-frontend"
    exit 1
fi

echo "📁 前端目录: $FRONTEND_DIR"
echo "📁 静态资源目录: $STATIC_DIR"

# 进入前端目录
cd "$FRONTEND_DIR"

# 检查是否有package.json
if [ ! -f "package.json" ]; then
    echo "❌ 未找到package.json文件"
    exit 1
fi

# 安装依赖
echo "📦 安装前端依赖..."
if command -v npm >/dev/null 2>&1; then
    npm install
elif command -v yarn >/dev/null 2>&1; then
    yarn install
else
    echo "❌ 未找到npm或yarn，请先安装Node.js"
    exit 1
fi

# 构建前端项目（跳过类型检查）
echo "🔨 构建前端项目..."
if command -v npm >/dev/null 2>&1; then
    # 只运行vite build，跳过类型检查
    npx vite build
elif command -v yarn >/dev/null 2>&1; then
    yarn vite build
fi

# 检查构建结果
if [ ! -d "$DIST_DIR" ]; then
    echo "❌ 构建失败，未找到dist目录"
    exit 1
fi

# 创建静态资源目录
echo "📂 创建静态资源目录..."
mkdir -p "$STATIC_DIR"

# 清理旧的静态资源
echo "🧹 清理旧的静态资源..."
rm -rf "$STATIC_DIR"/*

# 复制构建产物到静态资源目录
echo "📋 复制构建产物到静态资源目录..."
cp -r "$DIST_DIR"/* "$STATIC_DIR"/

# 验证复制结果
if [ -f "$STATIC_DIR/index.html" ]; then
    echo "✅ 前端构建完成！"
    echo "📊 构建统计:"
    echo "   - 静态文件数量: $(find "$STATIC_DIR" -type f | wc -l)"
    echo "   - 总大小: $(du -sh "$STATIC_DIR" | cut -f1)"
else
    echo "❌ 复制失败，未找到index.html"
    exit 1
fi

echo "🎉 SF-Chain前端构建成功！"
echo "💡 提示: 现在可以启动Spring Boot应用来访问前端界面"
