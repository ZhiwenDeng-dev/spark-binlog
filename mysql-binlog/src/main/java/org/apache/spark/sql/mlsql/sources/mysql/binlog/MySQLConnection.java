package org.apache.spark.sql.mlsql.sources.mysql.binlog;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;

/**
 * Representation of a MySQL connection.
 */
public final class MySQLConnection implements Closeable {

    private final String hostname;
    private final int port;
    private final String username;
    private final String password;
    private Connection connection;

    public MySQLConnection(String hostname, int port, String username, String password)
            throws ClassNotFoundException, SQLException {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        Class.forName("com.mysql.jdbc.Driver");
        connect();
    }

    private void connect() throws SQLException {
        String connectURL = "jdbc:mysql://" + hostname + ":" + port +
                "?useUnicode=true&zeroDateTimeBehavior=convertToNull&characterEncoding=UTF-8&tinyInt1isBit=false";
        System.out.println(connectURL);
        this.connection = DriverManager.getConnection(connectURL, username, password);
    }

    public String hostname() {
        return hostname;
    }

    public int port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public void execute(Callback<Statement> callback, boolean autocommit) throws SQLException {
        connection.setAutoCommit(autocommit);
        Statement statement = connection.createStatement();
        try {
            callback.execute(statement);
            if (!autocommit) {
                connection.commit();
            }
        } finally {
            statement.close();
        }
    }

    public void executePrepare(String sql, Callback<PreparedStatement> callback, boolean autocommit) throws SQLException {
        connection.setAutoCommit(autocommit);
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            callback.execute(statement);
            if (!autocommit) {
                connection.commit();
            }
        } finally {
            statement.close();
        }
    }

    public void execute(Callback<Statement> callback) throws SQLException {
        execute(callback, false);
    }

    public void execute(final String... statements) throws SQLException {
        execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                for (String command : statements) {
                    statement.execute(command);
                }
            }
        });
    }

    public void query(String sql, Callback<ResultSet> callback) throws SQLException {
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        try {
            ResultSet rs = statement.executeQuery(sql);
            try {
                callback.execute(rs);
                connection.commit();
            } finally {
                rs.close();
            }
        } finally {
            statement.close();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public void reconnect() throws IOException, SQLException {
        close();
        connect();
    }

    public interface Callback<T> {

        void execute(T obj) throws SQLException;
    }
}