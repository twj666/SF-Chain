package com.suifeng.sfchain.configcenter.logging;

/**
 * 配置中心AI日志路由上下文（线程级）
 */
public final class AICallLogRouteContext {

    private static final ThreadLocal<RouteKey> HOLDER = new ThreadLocal<>();

    private AICallLogRouteContext() {
    }

    public static Scope use(String tenantId, String appId) {
        HOLDER.set(new RouteKey(tenantId, appId));
        return () -> HOLDER.remove();
    }

    public static RouteKey current() {
        return HOLDER.get();
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    public static final class RouteKey {
        private final String tenantId;
        private final String appId;

        public RouteKey(String tenantId, String appId) {
            this.tenantId = tenantId;
            this.appId = appId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getAppId() {
            return appId;
        }
    }
}
