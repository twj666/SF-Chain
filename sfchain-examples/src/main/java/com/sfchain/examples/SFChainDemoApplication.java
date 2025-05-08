package com.sfchain.examples;


import com.sfchain.examples.common.ConsoleUI;
import com.sfchain.examples.common.DemoRegistry;
import com.sfchain.examples.common.DemoRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

/**
 * SFChain示例应用主入口
 * 提供统一的演示入口，支持多种演示场景
 * @author suifeng
 * 日期: 2025/4/30
 */
@SpringBootApplication
public class SFChainDemoApplication {

    @Autowired
    private DemoRegistry demoRegistry;

    @Autowired
    private ConsoleUI ui;

    public static void main(String[] args) {
        SpringApplication.run(SFChainDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            // 显示欢迎界面
            ui.clearScreen();
            ui.showWelcomeScreen();

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                // 显示主菜单
                ui.showMainMenu(demoRegistry.getAvailableDemos());

                String choice = scanner.nextLine().trim();

                if (choice.equalsIgnoreCase("exit") || choice.equalsIgnoreCase("quit") || choice.equals("0")) {
                    running = false;
                    ui.showExitMessage();
                    continue;
                }

                try {
                    int selectedIndex = Integer.parseInt(choice);
                    DemoRunner selectedDemo = ui.getDemoByDisplayIndex(selectedIndex);

                    if (selectedDemo != null) {
                        // 运行演示
                        ui.clearScreen();
                        selectedDemo.run(scanner, ui);
                    } else {
                        ui.showError("无效的选择，请输入有效的数字");
                    }
                } catch (NumberFormatException e) {
                    // 处理特殊命令
                    if (choice.equalsIgnoreCase("clear")) {
                        ui.clearScreen();
                        ui.showWelcomeScreen();
                    } else if (choice.equalsIgnoreCase("help")) {
                        ui.showHelpScreen();
                    } else {
                        ui.showError("无效的输入，请输入数字选择演示或输入'exit'退出");
                    }
                }
            }
        };
    }
}