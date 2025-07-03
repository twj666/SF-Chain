package com.sfchain.examples.demo;

import com.sfchain.core.AIService;
import com.sfchain.examples.common.ConsoleUI;
import com.sfchain.examples.common.DemoRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sfchain.core.constant.AIConstant.SILI_DEEP_SEEK_V3;
import static com.sfchain.examples.common.ConsoleUI.*;

/**
 * 项目上下文构建器
 * 扫描Java项目，生成AI友好的项目上下文文档
 */
@Component
public class JavaCodeAnalyzerDemo implements DemoRunner {

    @Autowired
    private AIService aiService;

    private static final int DEFAULT_THREADS = 3;
    private static final Set<String> EXCLUDED_DIRS = Set.of(
            "target", "build", ".git", ".idea", "node_modules"
    );

    @Override
    public String getName() { return "项目上下文构建器"; }

    @Override
    public String getDescription() { return "生成项目的AI上下文文档，极致压缩核心信息"; }

    @Override
    public String getIcon() { return "📋"; }

    @Override
    public String getCategory() { return "项目分析"; }

    @Override
    public int getOrder() { return 30; }

    @Override
    public String[] getTags() { return new String[]{"上下文", "项目分析", "文档生成"}; }

    @Override
    public void run(Scanner scanner, ConsoleUI ui) {
        ui.clearScreen();
        ui.showTitle("项目上下文构建器");

        System.out.println(ANSI_CYAN + "构建项目的AI上下文文档，为AI理解项目提供核心信息" + ANSI_RESET);
        System.out.println("输出格式专为AI优化，信息密度最大化\n");

        boolean running = true;
        while (running) {
            System.out.println(ANSI_BG_BLUE + ANSI_WHITE + ANSI_BOLD + " 操作选项 " + ANSI_RESET);
            System.out.println(ANSI_BRIGHT_YELLOW + "1. " + ANSI_RESET + "构建项目上下文");
            System.out.println(ANSI_BRIGHT_YELLOW + "2. " + ANSI_RESET + "分析单个类");
            System.out.println(ANSI_BRIGHT_YELLOW + "0. " + ANSI_RESET + "返回主菜单");
            System.out.print("\n请选择操作 > ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    buildProjectContext(scanner, ui);
                    break;
                case "2":
                    analyzeSingleClass(scanner, ui);
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    ui.showError("无效选择");
            }
        }

        ui.clearScreen();
    }

    /**
     * 构建项目上下文
     */
    private void buildProjectContext(Scanner scanner, ConsoleUI ui) {
        ui.clearScreen();
        ui.showTitle("项目上下文构建");

        // 获取项目路径
        System.out.println(ANSI_BOLD + "📁 项目根目录:" + ANSI_RESET);
        String projectPath = scanner.nextLine().trim();

        if (projectPath.isEmpty()) {
            ui.showError("路径不能为空");
            return;
        }

        Path rootPath = Paths.get(projectPath);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            ui.showError("路径不存在或不是目录");
            return;
        }

        // 扫描Java文件
        ui.showLoading("扫描Java文件");
        List<Path> javaFiles = scanJavaFiles(rootPath);

        if (javaFiles.isEmpty()) {
            ui.showError("未找到Java文件");
            return;
        }

        ui.clearScreen();
        System.out.println(ANSI_GREEN + "✅ 找到 " + javaFiles.size() + " 个Java文件" + ANSI_RESET);

        // 确认构建
        System.out.print("\n开始构建项目上下文？(Y/n) > ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no")) {
            return;
        }

        // 执行分析
        executeContextBuilding(javaFiles, rootPath, ui);
        ui.waitForEnter(scanner, null);
    }

    /**
     * 分析单个类
     */
    private void analyzeSingleClass(Scanner scanner, ConsoleUI ui) {
        ui.clearScreen();
        ui.showTitle("单类分析");

        System.out.println(ANSI_BOLD + "📄 Java文件路径:" + ANSI_RESET);
        String filePath = scanner.nextLine().trim();

        if (filePath.isEmpty()) {
            ui.showError("文件路径不能为空");
            return;
        }

        Path file = Paths.get(filePath);
        if (!Files.exists(file) || !file.toString().endsWith(".java")) {
            ui.showError("文件不存在或不是Java文件");
            return;
        }

        try {
            ui.showLoading("分析中");
            String javaCode = Files.readString(file);
            String packagePath = extractPackage(javaCode);

            String analysis = analyzeJavaCode(javaCode, file.getFileName().toString(), packagePath);

            ui.clearScreen();
            ui.showTitle("分析结果");
            System.out.println(analysis);

        } catch (Exception e) {
            ui.showError("分析失败: " + e.getMessage());
        }

        ui.waitForEnter(scanner, null);
    }

    /**
     * 执行上下文构建
     */
    private void executeContextBuilding(List<Path> javaFiles, Path rootPath, ConsoleUI ui) {
        ui.clearScreen();
        ui.showTitle("构建进行中");

        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREADS);
        List<Future<ContextEntry>> futures = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);

        // 提交分析任务
        for (Path javaFile : javaFiles) {
            Future<ContextEntry> future = executor.submit(() -> {
                try {
                    String javaCode = Files.readString(javaFile);
                    String packagePath = extractPackage(javaCode);
                    String relativePath = rootPath.relativize(javaFile).toString();

                    String analysis = analyzeJavaCode(javaCode, javaFile.getFileName().toString(), packagePath);

                    // 更新进度
                    int current = completed.incrementAndGet();
                    System.out.print("\r" + ANSI_CYAN + "进度: " + current + "/" + javaFiles.size() + ANSI_RESET);

                    return new ContextEntry(relativePath, packagePath, analysis);
                } catch (Exception e) {
                    System.err.println("\n分析 " + javaFile + " 失败: " + e.getMessage());
                    return null;
                }
            });
            futures.add(future);
        }

        // 收集结果
        List<ContextEntry> results = new ArrayList<>();
        for (Future<ContextEntry> future : futures) {
            try {
                ContextEntry entry = future.get(30, TimeUnit.SECONDS);
                if (entry != null) {
                    results.add(entry);
                }
            } catch (Exception e) {
                System.err.println("\n获取结果失败: " + e.getMessage());
            }
        }

        executor.shutdown();

        // 生成上下文文档
        generateContextDocument(results, rootPath, ui);
    }

    /**
     * 生成上下文文档
     */
    private void generateContextDocument(List<ContextEntry> results, Path rootPath, ConsoleUI ui) {
        try {
            String fileName = "PROJECT_CONTEXT_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";

            ui.clearScreen();
            ui.showTitle("生成上下文文档");

            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writeContextHeader(writer, rootPath, results.size());
                writeProjectStructure(writer, results);
                writeDetailedContext(writer, results);
                writeContextFooter(writer);
            }

            System.out.println(ANSI_GREEN + "✅ 上下文构建完成" + ANSI_RESET);
            System.out.println("📊 统计:");
            System.out.println("  分析类数: " + results.size());
            System.out.println("  文档大小: " + formatFileSize(new File(fileName).length()));
            System.out.println("\n📄 上下文文档: " + ANSI_BOLD + fileName + ANSI_RESET);

        } catch (IOException e) {
            ui.showError("生成文档失败: " + e.getMessage());
        }
    }

    /**
     * 分析Java代码
     */
    private String analyzeJavaCode(String javaCode, String fileName, String packagePath) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("javaCode", javaCode);
            params.put("fileName", fileName);
            params.put("packagePath", packagePath);

            return (String) aiService.execute("code-analysis", SILI_DEEP_SEEK_V3, params);
        } catch (Exception e) {
            return "ANALYSIS_FAILED: " + e.getMessage();
        }
    }

    /**
     * 扫描Java文件
     */
    private List<Path> scanJavaFiles(Path rootPath) {
        List<Path> javaFiles = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (EXCLUDED_DIRS.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java") &&
                            !file.getFileName().toString().contains("Test")) {
                        javaFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("扫描文件失败: " + e.getMessage());
        }

        return javaFiles;
    }

    /**
     * 提取包路径
     */
    private String extractPackage(String javaCode) {
        String[] lines = javaCode.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ") && line.endsWith(";")) {
                return line.substring(8, line.length() - 1).trim();
            }
        }
        return "";
    }

    /**
     * 写入上下文头部
     */
    private void writeContextHeader(PrintWriter writer, Path rootPath, int classCount) {
        writer.println("# PROJECT CONTEXT DOCUMENT");
        writer.println("# Project: " + rootPath.getFileName());
        writer.println("# Classes: " + classCount);
        writer.println();
    }

    /**
     * 写入项目结构
     */
    private void writeProjectStructure(PrintWriter writer, List<ContextEntry> results) {
        writer.println("## PROJECT STRUCTURE");
        writer.println();

        // 按包分组
        Map<String, List<ContextEntry>> packageGroups = new HashMap<>();
        for (ContextEntry entry : results) {
            String pkg = entry.packagePath.isEmpty() ? "default" : entry.packagePath;
            packageGroups.computeIfAbsent(pkg, k -> new ArrayList<>()).add(entry);
        }

        // 输出包结构
        packageGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    writer.println("📦 " + entry.getKey() + " (" + entry.getValue().size() + " classes)");
                    entry.getValue().forEach(cls ->
                            writer.println("  └─ " + Paths.get(cls.filePath).getFileName()));
                    writer.println();
                });
    }

    /**
     * 写入详细上下文
     */
    private void writeDetailedContext(PrintWriter writer, List<ContextEntry> results) {
        writer.println("## DETAILED CONTEXT");
        writer.println();

        results.forEach(entry -> {
            writer.println("### " + entry.filePath);
            writer.println(entry.analysis);
            writer.println();
        });
    }

    /**
     * 写入上下文尾部
     */
    private void writeContextFooter(PrintWriter writer) {
        writer.println("## END OF CONTEXT");
        writer.println("# This document provides comprehensive project understanding for AI assistance");
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
    }

    /**
     * 上下文条目类
     */
    private static class ContextEntry {
        final String filePath;
        final String packagePath;
        final String analysis;

        ContextEntry(String filePath, String packagePath, String analysis) {
            this.filePath = filePath;
            this.packagePath = packagePath;
            this.analysis = analysis;
        }
    }
}