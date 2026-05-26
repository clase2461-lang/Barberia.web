package com.barberia.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseConfig implements ApplicationContextAware {

    private static ApplicationContext context;
    private static DatabaseConfig instance = new DatabaseConfig();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static DatabaseConfig getInstance() {
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext no inicializado");
        }
        return context.getBean(DataSource.class).getConnection();
    }
}
