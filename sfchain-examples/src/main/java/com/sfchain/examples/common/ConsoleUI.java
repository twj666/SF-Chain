package com.sfchain.examples.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 控制台UI组件
 * 提供统一的UI渲染功能
 */
@Component
public class ConsoleUI {

    // ANSI颜色和样式常量
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_ITALIC = "\u001B[3m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BG_BLUE = "\u001B[44m";
    public static final String ANSI_BG_GREEN = "\u001B[42m";
    public static final String ANSI_BG_BLACK = "\u001B[40m";
    public static final String ANSI_BRIGHT_BLACK = "\u001B[90m";
    public static final String ANSI_BRIGHT_GREEN = "\u001B[92m";
    public static final String ANSI_BRIGHT_YELLOW = "\u001B[93m";
    public static final String ANSI_BRIGHT_BLUE = "\u001B[94m";
    public static final String ANSI_BRIGHT_CYAN = "\u001B[96m";
    
    // 应用信息
    @Value("${sfchain.app.name:SFChain Demo}")
    private String appName;
    
    @Value("${sfchain.app.version:1.0.0}")
    private String appVersion;
    
    /**
     * 清屏
     */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    /**
     * 显示欢迎界面
     */
    public void showWelcomeScreen() {
        String[] logo = {
            "   _____ ______ _____ _           _         _____                      ",
            "  / ____|  ____/ ____| |         (_)       |  __ \\                     ",
            " | (___ | |__ | |    | |__   __ _ _ _ __   | |  | | ___ _ __ ___   ___  ",
            "  \\___ \\|  __|| |    | '_ \\ / _` | | '_ \\  | |  | |/ _ \\ '_ ` _ \\ / _ \\ ",
            "  ____) | |   | |____| | | | (_| | | | | | | |__| |  __/ | | | | | (_) |",
            " |_____/|_|    \\_____|_| |_|\\__,_|_|_| |_| |_____/ \\___|_| |_| |_|\\___/ "
        };

        System.out.println("\n" + ANSI_BRIGHT_CYAN);
        Arrays.stream(logo).forEach(System.out::println);
        System.out.println(ANSI_RESET);

        System.out.println("\n" + ANSI_BOLD + ANSI_YELLOW + "欢迎使用 " + appName + " v" + appVersion + ANSI_RESET);
        System.out.println(ANSI_CYAN + "这是SFChain AI框架的功能演示系统，展示了框架的各种能力和使用场景" + ANSI_RESET);
        System.out.println(ANSI_BRIGHT_BLACK + "输入'help'查看帮助，输入'exit'退出系统" + ANSI_RESET + "\n");
    }

    private Map<Integer, DemoRunner> displayIndexToDemo = new HashMap<>();

    /**
     * 显示主菜单并记录显示索引映射
     */
    public void showMainMenu(List<DemoRunner> demos) {
        // 清除旧映射
        displayIndexToDemo.clear();

        // 按分类组织演示模块
        Map<String, List<DemoRunner>> demosByCategory = demos.stream()
                .collect(Collectors.groupingBy(DemoRunner::getCategory));

        System.out.println("\n" + ANSI_BG_BLUE + ANSI_WHITE + ANSI_BOLD + " 可用演示模块 " + ANSI_RESET);

        int displayIndex = 1;
        for (Map.Entry<String, List<DemoRunner>> entry : demosByCategory.entrySet()) {
            System.out.println("\n" + ANSI_BOLD + ANSI_YELLOW + "【" + entry.getKey() + "】" + ANSI_RESET);

            // 对每个类别内的演示模块按order排序
            List<DemoRunner> categoryDemos = new ArrayList<>(entry.getValue());
            categoryDemos.sort(Comparator.comparingInt(DemoRunner::getOrder));

            for (DemoRunner demo : categoryDemos) {
                System.out.println(ANSI_BRIGHT_YELLOW + displayIndex + ". " + ANSI_RESET +
                        demo.getIcon() + " " + ANSI_BOLD + demo.getName() + ANSI_RESET +
                        " - " + demo.getDescription());

                // 显示标签
                if (demo.getTags().length > 0) {
                    System.out.print("   ");
                    for (String tag : demo.getTags()) {
                        System.out.print(ANSI_BG_BLACK + ANSI_CYAN + " " + tag + " " + ANSI_RESET + " ");
                    }
                    System.out.println();
                }

                // 记录映射关系
                displayIndexToDemo.put(displayIndex, demo);
                displayIndex++;
            }
        }

        System.out.println("\n" + ANSI_BRIGHT_YELLOW + "0. " + ANSI_RESET + "退出系统");
        System.out.print("\n请选择要运行的演示 > ");
    }

    /**
     * 根据显示索引获取对应的演示模块
     */
    public DemoRunner getDemoByDisplayIndex(int index) {
        return displayIndexToDemo.get(index);
    }
    
    /**
     * 显示帮助界面
     */
    public void showHelpScreen() {
        System.out.println("\n" + ANSI_BG_BLUE + ANSI_WHITE + ANSI_BOLD + " 帮助信息 " + ANSI_RESET + "\n");
        
        System.out.println(ANSI_BOLD + "基本命令:" + ANSI_RESET);
        System.out.println("  help    - 显示此帮助信息");
        System.out.println("  clear   - 清屏");
        System.out.println("  exit    - 退出系统");
        
        System.out.println("\n" + ANSI_BOLD + "演示模块:" + ANSI_RESET);
        System.out.println("  输入对应的数字选择要运行的演示模块");
        System.out.println("  每个模块展示SFChain框架的不同功能和使用场景");
        
        System.out.println("\n" + ANSI_BOLD + "关于SFChain:" + ANSI_RESET);
        System.out.println("  SFChain是一个强大的AI应用开发框架，支持多模型调用、并行处理和复杂工作流");
        System.out.println("  详细文档请访问: https://github.com/suifeng/sfchain");
        
        System.out.println("\n按回车键返回主菜单...");
        try {
            System.in.read();
        } catch (Exception e) {
            // 忽略异常
        }
    }
    
    /**
     * 显示错误信息
     */
    public void showError(String message) {
        System.out.println("\n" + ANSI_RED + "错误: " + message + ANSI_RESET);
        sleep(1500);
    }
    
    /**
     * 显示成功信息
     */
    public void showSuccess(String message) {
        System.out.println("\n" + ANSI_GREEN + "✓ " + message + ANSI_RESET);
    }
    
    /**
     * 显示信息
     */
    public void showInfo(String message) {
        System.out.println("\n" + ANSI_CYAN + "ℹ " + message + ANSI_RESET);
    }
    
    /**
     * 显示警告信息
     */
    public void showWarning(String message) {
        System.out.println("\n" + ANSI_YELLOW + "⚠ " + message + ANSI_RESET);
    }
    
    /**
     * 显示标题
     */
    public void showTitle(String title) {
        System.out.println("\n" + ANSI_BG_BLUE + ANSI_WHITE + ANSI_BOLD + " " + title + " " + ANSI_RESET + "\n");
    }
    
    /**
     * 显示子标题
     */
    public void showSubTitle(String title) {
        System.out.println("\n" + ANSI_BOLD + ANSI_YELLOW + "【" + title + "】" + ANSI_RESET);
    }
    
    /**
     * 显示分隔线
     */
    public void showDivider() {
        System.out.println("\n" + ANSI_BRIGHT_BLACK + "─".repeat(80) + ANSI_RESET + "\n");
    }
    
    /**
     * 显示加载中
     */
    public void showLoading(String message) {
        System.out.println("\n" + ANSI_CYAN + "⏳ " + message + "..." + ANSI_RESET);
    }
    
    /**
     * 显示退出信息
     */
    public void showExitMessage() {
        System.out.println("\n" + ANSI_GREEN + "感谢使用" + appName + "！再见！" + ANSI_RESET);
    }
    
    /**
     * 显示标签
     */
    public void showTags(String[] tags, String color) {
        if (tags.length == 0) return;
        
        System.out.print("  ");
        for (String tag : tags) {
            System.out.print(color + "「" + tag + "」" + ANSI_RESET + " ");
        }
        System.out.println();
    }
    
    /**
     * 等待用户按回车继续
     */
    public void waitForEnter(Scanner scanner, String message) {
        System.out.print("\n" + (message != null ? message : "按回车键继续..."));
        scanner.nextLine();
    }
    
    /**
     * 休眠指定毫秒数
     */
    public void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 格式化持续时间
     */
    public String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "毫秒";
        } else if (durationMs < 60000) {
            return String.format("%.2f秒", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%d分%d秒", minutes, seconds);
        }
    }
}