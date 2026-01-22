package com.bank.persistence;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Provides a shared H2 DataSource as an OSGi service for all bundles.
 */
@Component(service = DataSource.class, immediate = true)
public class H2DataSourceProvider implements DataSource {

    private static final String JDBC_URL = "jdbc:h2:./bankdb;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    private JdbcDataSource delegate;
    private Server webServer;

    @Activate
    void activate() {

        try {
            webServer = Server.createWebServer("-webPort", "8082", "-tcpAllowOthers").start();
            System.out.println("H2 Web Console started at: " + webServer.getURL());
        } catch (SQLException e) {
            System.err.println("Failed to start H2 Web Console: " + e.getMessage());
        }

        delegate = new JdbcDataSource();
        delegate.setURL(JDBC_URL);
        delegate.setUser(JDBC_USER);
        delegate.setPassword(JDBC_PASSWORD);
        System.out.println("H2 DataSource started at " + JDBC_URL);
    }

    @Deactivate
    void deactivate() {
        delegate = null;
        if (webServer != null) {
            webServer.stop();
            System.out.println("H2 Web Console stopped.");
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
