package com.sms.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * druid 配置屬性
 *
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource.druid")
public class DruidProperties
{
    private int initialSize;

    private int minIdle;

    private int maxActive;

    private int maxWait;

    private int connectTimeout;

    private int socketTimeout;

    private int timeBetweenEvictionRunsMillis;

    private int minEvictableIdleTimeMillis;

    private int maxEvictableIdleTimeMillis;

    private String validationQuery;

    private boolean testWhileIdle;

    private boolean testOnBorrow;

    private boolean testOnReturn;

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public int getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(int maxWait) {
        this.maxWait = maxWait;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public int getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public int getMaxEvictableIdleTimeMillis() {
        return maxEvictableIdleTimeMillis;
    }

    public void setMaxEvictableIdleTimeMillis(int maxEvictableIdleTimeMillis) {
        this.maxEvictableIdleTimeMillis = maxEvictableIdleTimeMillis;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public com.alibaba.druid.pool.DruidDataSource dataSource(com.alibaba.druid.pool.DruidDataSource datasource)
    {
        /** 配置初始化大小、最小、最大 */
        datasource.setInitialSize(this.initialSize);
        datasource.setMaxActive(this.maxActive);
        datasource.setMinIdle(this.minIdle);

        /** 配置獲取連接等待超時的時間 */
        datasource.setMaxWait(this.maxWait);
        
        /** 配置驅動連接超時時間，檢測數據庫建立連接的超時時間，單位是毫秒 */
        datasource.setConnectTimeout(this.connectTimeout);
        
        /** 配置網絡超時時間，等待數據庫操作完成的網絡超時時間，單位是毫秒 */
        datasource.setSocketTimeout(this.socketTimeout);

        /** 配置間隔多久才進行一次檢測，檢測需要關閉的空閒連接，單位是毫秒 */
        datasource.setTimeBetweenEvictionRunsMillis(this.timeBetweenEvictionRunsMillis);

        /** 配置一個連接在池中最小、最大生存的時間，單位是毫秒 */
        datasource.setMinEvictableIdleTimeMillis(this.minEvictableIdleTimeMillis);
        datasource.setMaxEvictableIdleTimeMillis(this.maxEvictableIdleTimeMillis);

        /**
         * 用來檢測連接是否有效的sql，要求是一個查詢語句，常用select 'x'。如果validationQuery爲null，testOnBorrow、testOnReturn、testWhileIdle都不會起作用。
         */
        datasource.setValidationQuery(this.validationQuery);
        /** 建議配置爲true，不影響性能，並且保證安全性。申請連接的時候檢測，如果空閒時間大於timeBetweenEvictionRunsMillis，執行validationQuery檢測連接是否有效。 */
        datasource.setTestWhileIdle(this.testWhileIdle);
        /** 申請連接時執行validationQuery檢測連接是否有效，做了這個配置會降低性能。 */
        datasource.setTestOnBorrow(this.testOnBorrow);
        /** 歸還連接時執行validationQuery檢測連接是否有效，做了這個配置會降低性能。 */
        datasource.setTestOnReturn(this.testOnReturn);
        return datasource;
    }
    
    public com.alibaba.druid.pool.DruidDataSource dataSource(com.alibaba.druid.pool.DruidDataSource datasource, String prefix)
    {
        // 對於帶 master 或其他前綴的情況，使用默認配置
        return dataSource(datasource);
    }
}
