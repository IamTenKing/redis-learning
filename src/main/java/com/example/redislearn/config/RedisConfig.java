package com.example.redislearn.config;

import javax.annotation.PostConstruct;

import com.example.redislearn.utils.PropertiesLoaderUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
/**
 * @author jt
 * @date 2020-5-14
 */


@Configuration
@Data
public class RedisConfig {
    @Autowired
    private Environment env;
    private JedisPool pool;
    private String maxActive;
    private String maxIdle;
    private String maxWait;
    private String host;
    private String password;
    private String timeout;
    private String database;
    private String port;
    private String enable;
    private String sysName;

    public RedisConfig() {
    }

    @PostConstruct
    public void init() {
        PropertiesLoaderUtils prop = new PropertiesLoaderUtils(new String[]{"application.yml"});
        this.host = prop.getProperty("redis.host");
        if (StringUtils.isBlank(this.host)) {
            this.host = this.env.getProperty("redis.host");
            this.maxActive = this.env.getProperty("redis.pool.maxActive");
            this.maxIdle = this.env.getProperty("redis.pool.maxIdle");
            this.maxWait = this.env.getProperty("redis.pool.maxWait");
            this.password = this.env.getProperty("redis.password");
            this.timeout = this.env.getProperty("redis.timeout");
            this.database = this.env.getProperty("redis.database");
            this.port = this.env.getProperty("redis.port");
            this.sysName = this.env.getProperty("redis.sysName");
            this.enable = this.env.getProperty("redis.enable");
        } else {
            this.maxActive = prop.getProperty("redis.pool.maxActive");
            this.maxIdle = prop.getProperty("redis.pool.maxIdle");
            this.maxWait = prop.getProperty("redis.pool.maxWait");
            this.password = prop.getProperty("redis.password");
            this.timeout = prop.getProperty("redis.timeout");
            this.database = prop.getProperty("redis.database");
            this.port = prop.getProperty("redis.port");
            this.sysName = prop.getProperty("redis.sysName");
            this.enable = prop.getProperty("redis.enable");
        }

    }

    @Bean
    public JedisPoolConfig constructJedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(Integer.parseInt(this.maxActive));
        config.setMaxIdle(Integer.parseInt(this.maxIdle));
        config.setMaxWaitMillis((long)Integer.parseInt(this.maxWait));
        config.setTestOnBorrow(true);
        return config;
    }

    @Bean(
            name = {"pool"}
    )
    public JedisPool constructJedisPool() {
        String ip = this.host;
        int port = Integer.parseInt(this.port);
        String password = this.password;
        int timeout = Integer.parseInt(this.timeout);
        int database = Integer.parseInt(this.database);
        if (null == this.pool) {
            if (StringUtils.isBlank(password)) {
                this.pool = new JedisPool(this.constructJedisPoolConfig(), ip, port, timeout);
            } else {
                this.pool = new JedisPool(this.constructJedisPoolConfig(), ip, port, timeout, password, database);
            }
        }

        return this.pool;
    }


}
