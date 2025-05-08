package com.sfchain.examples.demo;

import com.sfchain.examples.common.ConsoleUI;
import com.sfchain.examples.common.DemoRunner;
import com.sfchain.operations.common.sample2.NewsAnalysisCoordinator;
import com.sfchain.operations.common.sample2.domain.NewsAnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.sfchain.examples.common.ConsoleUI.*;


/**
 * 新闻分析演示模块
 * 展示AIOperationCoordinator的并发调用能力
 */
@Component
public class NewsAnalysisDemo implements DemoRunner {

    @Autowired
    private NewsAnalysisCoordinator newsAnalysisCoordinator;
    
    // 示例新闻列表
    private final List<Map<String, String>> exampleNewsList = new ArrayList<>();
    
    // 在构造函数中初始化示例新闻
    public NewsAnalysisDemo() {
        initializeExampleNews();
    }

    @Override
    public String getName() {
        return "新闻分析系统";
    }

    @Override
    public String getDescription() {
        return "展示AIOperationCoordinator的并发调用能力，同时执行多个AI任务";
    }

    @Override
    public String getIcon() {
        return "📰";
    }
    
    @Override
    public String getCategory() {
        return "并发处理";
    }
    
    @Override
    public int getOrder() {
        return 20;
    }
    
    @Override
    public String[] getTags() {
        return new String[]{"并发", "新闻", "分析"};
    }

    @Override
    public void run(Scanner scanner, ConsoleUI ui) {
        ui.clearScreen();
        
        // 显示模块介绍
        ui.showTitle("新闻分析系统");
        System.out.println(ANSI_CYAN + "本演示展示了AIOperationCoordinator的并行处理能力，可同时执行多个AI任务" + ANSI_RESET);
        System.out.println("系统将对新闻进行：" + 
                ANSI_GREEN + "📝 摘要生成" + ANSI_RESET + "、" + 
                ANSI_YELLOW + "😊 情感分析" + ANSI_RESET + "、" + 
                ANSI_BLUE + "🔑 关键词提取" + ANSI_RESET + "和" + 
                ANSI_PURPLE + "🏷️ 分类" + ANSI_RESET);
        System.out.println("每个任务由不同的AI模型" + ANSI_BOLD + "并行处理" + ANSI_RESET + "，大幅提高处理效率\n");
        
        boolean running = true;
        while (running) {
            System.out.println("\n" + ANSI_BG_BLUE + ANSI_WHITE + ANSI_BOLD + " 操作选项 " + ANSI_RESET);
            System.out.println(ANSI_BRIGHT_YELLOW + "1. " + ANSI_RESET + "分析示例新闻");
            System.out.println(ANSI_BRIGHT_YELLOW + "2. " + ANSI_RESET + "分析自定义新闻");
            System.out.println(ANSI_BRIGHT_YELLOW + "0. " + ANSI_RESET + "返回主菜单");
            System.out.print("\n请选择操作 > ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    analyzeExampleNews(scanner, ui);
                    break;
                case "2":
                    analyzeUserInputNews(scanner, ui);
                    break;
                case "0":
                case "exit":
                case "quit":
                    running = false;
                    break;
                default:
                    ui.showError("无效的选择，请重试");
            }
        }
        
        ui.clearScreen();
    }
    
    @Override
    public void cleanup() {
        // 关闭协调器
        newsAnalysisCoordinator.shutdown();
    }
    
    /**
     * 初始化示例新闻列表
     */
    private void initializeExampleNews() {
        // 示例新闻1
        Map<String, String> news1 = new HashMap<>();
        news1.put("title", "人工智能技术在医疗领域取得重大突破");
        news1.put("source", "科技日报");
        news1.put("date", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        news1.put("content", "近日，由清华大学和北京协和医院联合研发的医疗AI系统\"智医助手\"在诊断准确率上取得重大突破，" +
                "在多项医学影像识别测试中超越了资深专家的平均水平。该系统利用深度学习技术，通过分析数百万张医学影像，" +
                "能够快速准确地识别肺部、肝脏等多个器官的异常情况。\n\n" +
                "研究团队负责人王教授表示：\"我们的目标不是取代医生，而是为医生提供强大的辅助工具，减轻他们的工作负担，" +
                "同时提高诊断准确率，特别是在基层医疗资源紧缺的地区。\"\n\n" +
                "卫生部发言人李明在接受采访时表示，政府将加大对医疗AI技术的支持力度，计划在未来三年内在全国二级以上医院" +
                "推广应用类似系统，预计将惠及数亿患者，显著提升医疗服务效率和质量。\n\n" +
                "然而，也有专家提出了对医疗AI的伦理和安全担忧。中国医学伦理学会会长张教授指出：\"我们需要建立完善的监管" +
                "机制，确保AI系统的决策过程透明可解释，并且最终决策权仍应掌握在医生手中。\"\n\n" +
                "据悉，\"智医助手\"系统目前已在北京、上海、广州等地的10家三甲医院进行试点应用，初步数据显示，系统辅助诊断" +
                "后，医生的工作效率提升了约35%，诊断准确率提高了约15%。");
        exampleNewsList.add(news1);

        // 示例新闻2
        Map<String, String> news2 = new HashMap<>();
        news2.put("title", "全球气候变化加剧，多国承诺减排目标");
        news2.put("source", "环球时报");
        news2.put("date", LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        news2.put("content", "联合国气候变化大会昨日在日内瓦闭幕，与会的195个国家一致同意加强减排力度，以应对日益严峻的全球气候变化。" +
                "最新气象数据显示，2024年前5个月全球平均气温较工业化前水平上升了1.5摄氏度，多地极端天气事件频发。\n\n" +
                "中国代表在会上宣布，将进一步提高国家自主贡献力度，承诺到2030年碳排放达峰，2060年前实现碳中和。同时，中国将投入1000亿元" +
                "支持发展中国家应对气候变化。\n\n" +
                "美国总统也表示将重返《巴黎协定》，并计划在2030年前将温室气体排放量减少50-52%（相比2005年水平）。欧盟则提出了更激进的目标，" +
                "计划到2030年减排55%，2050年实现碳中和。\n\n" +
                "然而，环保组织对会议成果表示担忧，认为各国承诺的减排目标与将全球升温控制在1.5摄氏度以内的目标仍有较大差距。" +
                "绿色和平组织发言人表示：\"我们需要更具雄心的目标和更快的行动，否则将面临灾难性的后果。\"");
        exampleNewsList.add(news2);

        // 示例新闻3
        Map<String, String> news3 = new HashMap<>();
        news3.put("title", "新一代量子计算机问世，计算能力提升百倍");
        news3.put("source", "未来科技报");
        news3.put("date", LocalDate.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        news3.put("content", "中国科学院量子信息与量子科技创新研究院今日宣布，成功研制出新一代超导量子计算机\"悟空\"，" +
                "实现了256个量子比特的纠缠操控，计算能力较上一代提升了近百倍。\n\n" +
                "该量子计算机采用了全新的量子纠错技术和低温超导材料，大幅提高了量子比特的相干时间和运算精度。" +
                "项目负责人陈教授表示：\"'悟空'的问世标志着我国在量子计算领域跻身世界前列，为解决材料设计、药物研发、" +
                "密码破解等领域的复杂计算问题提供了强大工具。\"\n\n" +
                "据悉，研究团队已使用\"悟空\"成功模拟了几种新型高温超导材料的量子特性，这在传统超级计算机上需要数百年才能完成。" +
                "专家预计，量子计算将在未来5-10年内实现商业化应用，并可能引发新一轮技术革命。\n\n" +
                "国际量子计算专家、美国麻省理工学院教授约翰逊评价道：\"中国在量子计算领域的进展令人印象深刻，" +
                "'悟空'的技术指标确实代表了当前全球量子计算的最高水平之一。\"");
        exampleNewsList.add(news3);
    }

    /**
     * 分析示例新闻
     */
    private void analyzeExampleNews(Scanner scanner, ConsoleUI ui) {
        ui.clearScreen();

        ui.showTitle("示例新闻列表");

        for (int i = 0; i < exampleNewsList.size(); i++) {
            Map<String, String> news = exampleNewsList.get(i);
            System.out.println(ANSI_BRIGHT_YELLOW + (i + 1) + ". " + ANSI_RESET +
                    ANSI_BOLD + news.get("title") + ANSI_RESET);
            System.out.println("   " + ANSI_BRIGHT_BLACK + "📢 " +
                    news.get("source") + " | 📅 " + news.get("date") + ANSI_RESET);
        }

        System.out.println(ANSI_BRIGHT_YELLOW + "0. " + ANSI_RESET + "返回上级菜单");
        System.out.print("\n请选择要分析的新闻 > ");

        String choice = scanner.nextLine().trim();

        if (choice.equals("0")) {
            return;
        }

        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < exampleNewsList.size()) {
                Map<String, String> selectedNews = exampleNewsList.get(index);
                analyzeNews(
                        selectedNews.get("title"),
                        selectedNews.get("content"),
                        selectedNews.get("source"),
                        selectedNews.get("date"),
                        ui
                );
                ui.waitForEnter(scanner, null);
            } else {
                ui.showError("无效的选择，请重试");
            }
        } catch (NumberFormatException e) {
            ui.showError("请输入有效的数字");
        }
    }

    /**
     * 分析用户输入的新闻
     */
    private void analyzeUserInputNews(Scanner scanner, ConsoleUI ui) {
        ui.clearScreen();

        ui.showTitle("自定义新闻分析");

        // 提示用户输入标题
        System.out.println(ANSI_BOLD + "📋 新闻标题:" + ANSI_RESET);
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            ui.showError("标题不能为空！");
            ui.sleep(1500);
            return;
        }

        // 提示用户输入来源（可选）
        System.out.println("\n" + ANSI_BOLD + "📢 新闻来源:" + ANSI_RESET + ANSI_BRIGHT_BLACK + " (可选，直接回车跳过)" + ANSI_RESET);
        String source = scanner.nextLine().trim();
        if (source.isEmpty()) {
            source = "未知来源";
        }

        // 使用当前日期作为默认日期
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 提示用户粘贴或输入新闻内容
        System.out.println("\n" + ANSI_BOLD + "📰 新闻内容:" + ANSI_RESET);
        System.out.println(ANSI_BRIGHT_BLACK + "请粘贴或输入新闻全文，完成后按回车，再输入一个单独的'END'表示结束" + ANSI_RESET);

        StringBuilder contentBuilder = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).equals("END")) {
            contentBuilder.append(line).append("\n");
        }
        String content = contentBuilder.toString().trim();

        if (content.isEmpty()) {
            ui.showError("新闻内容不能为空！");
            ui.sleep(1500);
            return;
        }

        analyzeNews(title, content, source, date, ui);
        ui.waitForEnter(scanner, null);
    }

    /**
     * 分析新闻
     */
    private void analyzeNews(String title, String content, String source, String date, ConsoleUI ui) {
        ui.clearScreen();

        // 显示分析中的提示
        ui.showTitle("新闻分析进行中");
        ui.showLoading("正在并行处理新闻分析任务");

        // 执行分析
        long startTime = System.currentTimeMillis();
        NewsAnalysisResult result = newsAnalysisCoordinator.analyzeNews(title, content, source, date);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 显示分析结果
        ui.clearScreen();
        printAnalysisResult(result, duration, ui);
    }

    /**
     * 打印分析结果
     */
    private void printAnalysisResult(NewsAnalysisResult result, long duration, ConsoleUI ui) {
        ui.showTitle("新闻分析结果");
        System.out.println(ANSI_BRIGHT_BLACK + "⏱️ 总处理时间: " + ui.formatDuration(duration) + ANSI_RESET + "\n");

        // 标题和元数据
        System.out.println(ANSI_BOLD + "📋 标题: " + ANSI_RESET + result.getTitle());
        System.out.println(ANSI_BRIGHT_BLACK + "📢 来源: " + result.getSource() + ANSI_RESET);
        System.out.println(ANSI_BRIGHT_BLACK + "📅 日期: " + result.getDate() + ANSI_RESET);

        // 分隔线
        ui.showDivider();

        // 摘要
        System.out.println(ANSI_GREEN + ANSI_BOLD + "📝 摘要" + ANSI_RESET);
        System.out.println(result.getSummary().getSummary());
        System.out.printf(ANSI_BRIGHT_BLACK + "压缩比: %.2f%%" + ANSI_RESET + "\n",
                (1 - result.getSummary().getCompressionRatio()) * 100);

        // 分隔线
        ui.showDivider();

        // 情感分析
        System.out.println(ANSI_YELLOW + ANSI_BOLD + "😊 情感分析" + ANSI_RESET);

        // 根据情感得分选择颜色
        String sentimentColor;
        if (result.getSentiment().getScore() > 0.3) {
            sentimentColor = ANSI_GREEN; // 积极
        } else if (result.getSentiment().getScore() < -0.3) {
            sentimentColor = ANSI_RED; // 消极
        } else {
            sentimentColor = ANSI_BRIGHT_BLACK; // 中性
        }

        System.out.printf("情感倾向: " + sentimentColor + "%s" + ANSI_RESET + " (得分: " +
                        sentimentColor + "%.2f" + ANSI_RESET + ")\n",
                result.getSentiment().getSentiment(),
                result.getSentiment().getScore());
        System.out.println("分析: " + result.getSentiment().getAnalysis());

        // 分隔线
        ui.showDivider();

        // 关键词
        System.out.println(ANSI_BLUE + ANSI_BOLD + "🔑 关键词分析" + ANSI_RESET);

        // 关键词以标签形式展示
        System.out.print("主要关键词: ");
        printTags(result.getKeywords().getKeywords(), ANSI_BLUE);

        if (result.getKeywords().getEntities().length > 0) {
            System.out.print("\n实体: ");
            printTags(result.getKeywords().getEntities(), ANSI_PURPLE);
        }

        if (result.getKeywords().getTopics().length > 0) {
            System.out.print("\n主题: ");
            printTags(result.getKeywords().getTopics(), ANSI_CYAN);
        }

        // 分隔线
        ui.showDivider();

        // 分类
        System.out.println(ANSI_PURPLE + ANSI_BOLD + "🏷️ 新闻分类" + ANSI_RESET);
        System.out.printf("主要分类: " + ANSI_BOLD + "%s" + ANSI_RESET + " (置信度: %.2f%%)\n",
                result.getCategory().getPrimaryCategory(),
                result.getCategory().getConfidence() * 100);

        if (result.getCategory().getSubCategories().length > 0) {
            System.out.print("子分类: ");
            printTags(result.getCategory().getSubCategories(), ANSI_YELLOW);
        }

        // 分隔线
        ui.showDivider();

        // 并行处理信息
        System.out.println(ANSI_BRIGHT_BLACK + "⚡ 并行处理信息: 使用了" +
                result.getModelCount() + "个AI模型同时处理，总耗时" + ui.formatDuration(duration) + ANSI_RESET);
    }

    /**
     * 以标签形式打印字符串数组
     */
    private void printTags(String[] tags, String color) {
        for (int i = 0; i < tags.length; i++) {
            System.out.print(color + "「" + tags[i] + "」" + ANSI_RESET);
            if (i < tags.length - 1) {
                System.out.print(" ");
            }
        }
        System.out.println();
    }
}