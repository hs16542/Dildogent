#!/bin/bash

echo "🔧 修复LLMUtil依赖问题..."

# 备份当前配置
echo "📦 备份当前配置..."
cp build.gradle.kts build.gradle.kts.backup.$(date +%Y%m%d_%H%M%S)

# 使用最小化配置
echo "⚙️ 应用最小化配置..."
cp build.gradle.kts.minimal build.gradle.kts

echo "🧹 清理项目..."
./gradlew clean

echo "🔄 刷新依赖..."
./gradlew --refresh-dependencies

echo "✅ 修复完成！"
echo ""
echo "📋 下一步："
echo "1. 运行: ./gradlew build"
echo "2. 如果成功，可以尝试逐步添加其他依赖"
echo "3. 如果失败，查看错误日志"
echo ""
echo "📚 更多信息请查看 TROUBLESHOOTING.md" 