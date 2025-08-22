package com.suifeng.sfchain.persistence;

import com.suifeng.sfchain.persistence.config.DatabaseType;
import com.suifeng.sfchain.persistence.config.PersistenceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 持久化服务工厂
 */
@Component
public class PersistenceServiceFactory {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private PersistenceConfig persistenceConfig;
    
    public PersistenceService createPersistenceService() {
        DatabaseType dbType = persistenceConfig.getDatabaseTypeEnum();
        
        switch (dbType) {
            case MYSQL:
                return applicationContext.getBean(MySQLPersistenceService.class);
            case POSTGRESQL:
                return applicationContext.getBean(PostgreSQLPersistenceService.class);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
}