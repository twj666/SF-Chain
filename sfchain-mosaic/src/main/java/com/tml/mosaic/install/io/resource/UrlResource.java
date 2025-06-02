package com.tml.mosaic.install.io.resource;

import com.tml.mosaic.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * 描述: 网络加载资源
 * @author suifeng
 * 日期: 2025/5/29
 */
public class UrlResource implements Resource {

    private final URL url;

    public UrlResource(URL url) {
        Assert.notNull(url,"URL 不得为空");
        this.url = url;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        URLConnection con = this.url.openConnection();
        try {
            return con.getInputStream();
        }
        catch (IOException ex){
            if (con instanceof HttpURLConnection){
                // 断开连接
                ((HttpURLConnection) con).disconnect();
            }
            throw ex;
        }
    }

}
