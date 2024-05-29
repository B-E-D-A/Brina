package org.hse.brina;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.client.Client;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Config предоставляет значения ширины и высоты по умолчанию для приложения, сохраняет путь до базы данных у пользователя
 */
public class Config {
    private static final Properties properties = new Properties();
    private static final Logger logger = LogManager.getLogger();

    public static Client client = new Client("localhost", 8080);
    private static final String projectPath = "projectPath";
    private static final String pathToDB = "pathToDB";
    private static final String defaultHeight = "defaultHeight";
    private static final String defaultWidth = "defaultWidth";
    private static final String pathToViews = "/org/hse/brina/views/";
    private static final String pathToAssets = "/org/hse/brina/assets/";
    private static final String pathToCss = "/org/hse/brina/css/";
    private static final int appNameLength = 5;
    public static StringBuilder oldScene = new StringBuilder();


    static {
        try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(inputStream);
            properties.setProperty(projectPath, System.getProperty("user.dir"));
            FileOutputStream fileOutputStream = new FileOutputStream("src/main/resources/config.properties");
            properties.store(fileOutputStream, null);
            fileOutputStream.close();
        } catch (Exception e) {
            logger.error("Error loading the settings file: {}", e.getMessage());
        }
    }

    public static double getDefaultWidth() {
        return Double.parseDouble(properties.getProperty(defaultWidth));

    }

    public static double getDefaultHeight() {
        return Double.parseDouble(properties.getProperty(defaultHeight));
    }

    public static String getPathToDB() {
        return getProjectPath() + properties.getProperty(pathToDB);
    }

    public static String getProjectPath() {
        return properties.getProperty(projectPath);
    }

    public static String getPathToViews() {
        return pathToViews;
    }

    public static String getPathToAssets() {
        return pathToAssets;
    }

    public static String getPathToCss() {
        return pathToCss;
    }

    public static int getAppNameLength() {
        return appNameLength;
    }
}