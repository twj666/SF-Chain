package com.suifeng.sfchain.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {
    
    /**
     * 处理根路径访问，返回白名单页面
     */
    @GetMapping("/")
    public ResponseEntity<String> handleRoot() {
        String whitelistPage = "<!DOCTYPE html>\n" +
                "<html lang='zh-CN'>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <title>访问受限</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; text-align: center; margin-top: 100px; background-color: #f5f5f5; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 40px; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
                "        h1 { color: #333; margin-bottom: 20px; }\n" +
                "        p { color: #666; line-height: 1.6; margin-bottom: 15px; }\n" +
                "        .notice { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 4px; margin: 20px 0; }\n" +
                "        .access-link { color: #007bff; text-decoration: none; font-weight: bold; }\n" +
                "        .access-link:hover { text-decoration: underline; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n" +
                "        <h1>🔒 访问受限</h1>\n" +
                "        <div class='notice'>\n" +
                "            <p><strong>此页面仅对授权用户开放</strong></p>\n" +
                "            <p>如果您是授权用户，请通过指定入口访问应用程序。</p>\n" +
                "        </div>\n" +
                "        <p>如需访问应用，请联系系统管理员获取访问权限。</p>\n" +
                "        <p><small>如果您已获得授权，请使用正确的访问路径。</small></p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(whitelistPage);
    }
}