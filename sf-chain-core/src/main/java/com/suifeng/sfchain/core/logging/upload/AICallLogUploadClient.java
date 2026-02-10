package com.suifeng.sfchain.core.logging.upload;

import java.util.List;

/**
 * AI调用日志上报客户端
 */
public interface AICallLogUploadClient {

    boolean upload(List<AICallLogUploadItem> items);
}
