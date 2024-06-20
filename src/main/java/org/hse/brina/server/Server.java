package org.hse.brina.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс Server - сервер, который прослушивает подключения клиентов и обрабатывает их запросы,
 * поддерживает подключение к базе данных для хранения и извлечения пользовательской информации.
 */

public class Server {
    private static final Logger logger = LogManager.getLogger();
    public ServerSocket serverSocket;
    public Connection connection;
    public boolean isRunning;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Server started. Waiting for clients...");
            String url = "jdbc:sqlite:" + Config.getPathToDB();
            connection = DriverManager.getConnection(url);
        } catch (IOException | SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server(8080);
        server.start();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public Connection getConnection() {
        return connection;
    }

    public void start() {
        try {
            isRunning = true;
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected: " + clientSocket.getInetAddress().getHostName());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public class ClientHandler implements Runnable {
        private final Socket clientSocket;
        public PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("signInUser")) {
                        performSignIn(inputLine);
                    } else if (inputLine.startsWith("addUser")) {
                        performAddUser(inputLine);
                    } else if (inputLine.startsWith("signUpUser")) {
                        performSignUp(inputLine);
                    } else if (inputLine.startsWith("getDocuments")) {
                        performGetDocuments(inputLine);
                    } else if (inputLine.startsWith("saveDocument")) {
                        performSaveDocument(inputLine);
                    } else if (inputLine.startsWith("openDocumentById")) {
                        performOpenDocumentById(inputLine);
                    } else if (inputLine.startsWith("addDocumentById")) {
                        performAddDocumentById(inputLine);
                    } else if (inputLine.startsWith("unlockDocument")) {
                        performUnlockDocument(inputLine);
                    } else if (inputLine.startsWith("lockDocument")) {
                        performLockDocument(inputLine);
                    } else if (inputLine.startsWith("getLock")) {
                        performGetLock(inputLine);
                    } else if (inputLine.startsWith("addFriend")) {
                        performAddFriend(inputLine);
                    } else if (inputLine.startsWith("getFriendsList")) {
                        performGetFriendsList(inputLine);
                    } else {
                        out.println("Unknown command");
                    }
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        private void performSignIn(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 3) {
                String username = userData[1];
                String password = userData[2];
                boolean isUserExist = checkIsUserRegistered(username);
                if (!isUserExist) {
                    out.println("User with this name not found");
                } else {
                    boolean isPasswordCorrect = checkPassword(username, password);
                    if (isPasswordCorrect) {
                        out.println("User logged in");
                    } else {
                        out.println("Wrong password");
                    }
                }
            } else {
                out.println("Invalid command format");
            }
        }

        private void performAddUser(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 3) {
                String username = userData[1];
                String password = userData[2];
                addUser(username, password, Integer.toString(password.hashCode()));
                out.println("New user added");
            } else {
                out.println("Invalid command format");
            }
        }

        private void performSignUp(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 4) {
                String username = userData[1];
                String password = userData[2];
                String passwordSalt = userData[3];
                boolean isUserExist = checkIsUserRegistered(username);
                if (isUserExist) {
                    out.println("User with the same name already exists");
                } else {
                    addUser(username, password, passwordSalt);
                    out.println("User is registered");
                }
            } else {
                out.println("Invalid command format");
            }
        }

        private void performGetDocuments(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 2) {
                String username = userData[1];
                Map<String, String> userDocuments = getUserDocuments(username);
                StringBuilder stringBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : userDocuments.entrySet()) {
                    stringBuilder.append(entry.getKey()).append(" ").append(entry.getValue()).append(" ");
                }
                out.println(stringBuilder.toString().trim());
            } else {
                out.println("Invalid command format");
            }
        }

        private void performSaveDocument(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 4) {
                String filename = userData[1];
                String username = userData[2];
                String name = userData[3];
                addDocument(filename, username, name, 0);
                out.println("Document saved");
            } else {
                out.println("Invalid command format");
            }
        }

        public void performOpenDocumentById(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 3) {
                String username = userData[1];
                int fileId = Integer.parseInt(userData[2]);
                String response = checkAndUpdateFileLock(username, fileId);
                out.println(response);
            } else {
                out.println("Invalid command format");
            }
        }

        public void performAddDocumentById(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 4) {
                String username = userData[1];
                String filename = userData[2];
                String access = userData[3];
                saveDocumentById(username, filename, access);
                out.println("Document saved");
            } else {
                out.println("Invalid command format");
            }
        }

        private void performUnlockDocument(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 2) {
                int documentId = userData[1].hashCode();
                setLockStatus(documentId, 0);
                out.println("Document unlocked");
            } else {
                out.println("Invalid command format");
            }
        }

        private void performLockDocument(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 2) {
                String documentId = userData[1];
                if (getLockStatus(Integer.parseInt(documentId)) == 1) {
                    out.println("Document closed");
                } else {
                    out.println("Document locked");
                }
                setLockStatus(Integer.parseInt(documentId), 1);
            } else {
                out.println("Invalid command format");
            }
        }

        private void performGetLock(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 2) {
                String documentName = userData[1];
                String[] filename = documentName.split("\\.");
                int lockStatus = getLockStatus(filename[0].hashCode());
                out.println(lockStatus);
            } else {
                out.println("Invalid command format");
            }
        }

        private void performAddFriend(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 3) {
                String username = userData[1];
                String friendName = userData[2];

                if (checkIsUserRegistered(friendName)) {
                    addFriend(username, friendName);
                    addFriend(friendName, username);
                    out.println("Friend added successfully");
                } else {
                    out.println("User " + friendName + " not found");
                }
            } else {
                out.println("Invalid command format");
            }
        }

        private void performGetFriendsList(String inputLine) {
            String[] userData = inputLine.split(" ");
            if (userData.length == 2) {
                String username = userData[1];
                String friendsList = getFriendsList(username);

                if (friendsList.isEmpty()) {
                    out.println("No friends found");
                } else {
                    out.println(friendsList);
                }
            } else {
                out.println("Invalid command format");
            }
        }

        public String getFriendsList(String username) {
            StringBuilder friends = new StringBuilder();
            String sql = "SELECT friend FROM friends WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    if (!friends.isEmpty()) {
                        friends.append(" ");
                    }
                    friends.append(resultSet.getString("friend"));
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }

            return friends.toString();
        }

        public void addFriend(String username, String friendName) {
            String sql = "INSERT INTO friends (username, friend) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                statement.setString(2, friendName);
                statement.executeUpdate();
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }

        public void addUser(String username, String password, String passwordSalt) {
            String sql = "INSERT INTO users (username, password, password_salt) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                statement.setString(2, password);
                statement.setString(3, passwordSalt);
                statement.executeUpdate();
                logger.info("User added into db");
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }

        public void addDocument(String filename, String username, String name, int m_lock) {
            String sql = "INSERT INTO user_documents (username, filename, file_path, file_id, access, lock) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                statement.setString(2, filename);
                statement.setString(3, Config.getProjectPath().substring(0, Config.getProjectPath().length() - Config.getAppNameLength()) + "documents/" + filename);
                statement.setInt(4, Math.abs(name.hashCode()));
                statement.setString(5, "w");
                statement.setInt(6, m_lock);
                statement.executeUpdate();
                logger.info("Document added into db");
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }

        public void saveDocumentById(String username, String filename, String access) {
            String sql = "INSERT INTO user_documents (username, filename, file_path, file_id, access, lock) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                String documentName = filename + ".rtfx";
                statement.setString(2, documentName);
                statement.setString(3, Config.getProjectPath().substring(0, Config.getProjectPath().length() - Config.getAppNameLength()) + "documents/" + documentName);
                statement.setInt(4, Math.abs(filename.hashCode()));
                statement.setString(5, access);
                statement.setInt(6, 1);
                statement.executeUpdate();
                logger.info(documentName + " added into db for " + username);
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }

        public String checkAndUpdateFileLock(String username, int fileId) {
            String selectQuery = "SELECT file_path, access FROM user_documents WHERE username = ? AND file_id = ?";
            String updateQuery = "UPDATE user_documents SET lock = 1 WHERE username = ? AND file_id = ?";

            try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery); PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                selectStatement.setString(1, username);
                selectStatement.setInt(2, fileId);
                ResultSet resultSet = selectStatement.executeQuery();

                if (resultSet.next()) {
                    String filePath = resultSet.getString("file_path");
                    String access = resultSet.getString("access");
                    updateStatement.setString(1, username);
                    updateStatement.setInt(2, fileId);
                    updateStatement.executeUpdate();

                    return filePath + " " + access;
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
            return "No such file";
        }

        public boolean checkPassword(String username, String password) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                statement.setString(2, password);
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next();
            } catch (SQLException e) {
                logger.error(e.getMessage());
                return false;
            }
        }

        public boolean checkIsUserRegistered(String username) {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return false;
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
            return true;
        }

        public Map<String, String> getUserDocuments(String username) {
            Map<String, String> documentsMap = new HashMap<>();
            String sql = "SELECT filename, file_path, access FROM user_documents WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String filename = resultSet.getString("filename");
                    String filePath = resultSet.getString("file_path");
                    String access = resultSet.getString("access");
                    documentsMap.put(access + filename, filePath);
                    logger.info(filePath);
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
            return documentsMap;
        }

        public void setLockStatus(Integer id, Integer value) {
            String sql = "UPDATE user_documents SET lock = 1 WHERE file_id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(sql)) {
                updateStatement.setInt(1, id);
                ResultSet resultSet = updateStatement.executeQuery();

                if (resultSet.next()) {
                    updateStatement.setInt(1, value);
                    updateStatement.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }

        public int getLockStatus(Integer id) {
            String sql = "SELECT lock FROM user_documents WHERE file_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getInt("lock");
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
            return -1;
        }

        public void stop() {
            try {
                in.close();
                out.close();
                clientSocket.close();
                serverSocket.close();
                logger.info("Server stopped");
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }
}