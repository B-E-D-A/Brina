package org.hse.brina.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Connector {

    private static final Logger logger = LogManager.getLogger();

    public static void connect() {
        Connection connector = null;
        try {
            String url = "jdbc:sqlite:" + Config.getPathToDB();
            connector = DriverManager.getConnection(url);
            logger.info("Connection to SQLite has been established.");

            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT NOT NULL," +
                    "password TEXT NOT NULL" +
                    ")";
            Statement statement = connector.createStatement();
            statement.executeUpdate(sql);
            logger.info("Table has been created");
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (SQLException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        //ExecuteCommands.main(args);
        connect();
    }
}