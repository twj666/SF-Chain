package com.tml.mosaic.factory.io.resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 描述: 资源加载接口
 * @author suifeng
 * 日期: 2025/5/29
 */
public interface Resource {

    InputStream getInputStream() throws IOException;

    String getPath();
}