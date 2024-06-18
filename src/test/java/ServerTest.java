import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.hse.brina.server.Server;

import static org.mockito.ArgumentMatchers.anyString;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerTest {
    private Server server;
    private Socket clientSocket;
    private ByteArrayInputStream inputStream;
    private ByteArrayOutputStream outputStream;
    private PrintWriter out;
    private Connection connection;
    private ServerSocket serverSocket;

    @BeforeEach
    void setUp() throws IOException {
        server = new Server(8080);
        clientSocket = mock(Socket.class);
        connection = mock(Connection.class);
        serverSocket = mock(ServerSocket.class);
        server.isRunning = true;
        server.connection = connection;
        server.serverSocket = serverSocket;

        when(clientSocket.getInputStream()).thenReturn(inputStream);
        when(clientSocket.getOutputStream()).thenReturn(outputStream);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testServerInitialization() throws IOException {
        assertNotNull(server.getServerSocket());
        assertNotNull(server.getConnection());
    }

    @Test
    void testServerStart() {
        Thread serverThread = new Thread(() -> server.start());

        serverThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFalse(server.getServerSocket().isClosed());
        assertTrue(server.isRunning());
        serverThread.interrupt();
    }

    @Test
    void testClientConnection() throws IOException {
        Thread serverThread = new Thread(() -> server.start());

        serverThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFalse(server.getServerSocket().isClosed());

        try (Socket clientSocket = new Socket("localhost", 8080)) {
            assertTrue(clientSocket.isConnected());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to connect to the server");
        }

        server.stop();
    }

    @Test
    void testStop() throws IOException, SQLException {
        when(serverSocket.isClosed()).thenReturn(false);
        when(connection.isClosed()).thenReturn(false);

        server.stop();

        assertFalse(server.isRunning);
        verify(serverSocket, times(1)).close();
        verify(connection, times(1)).close();
        InOrder inOrder = inOrder(serverSocket, connection);
        inOrder.verify(serverSocket).close();
        inOrder.verify(connection).close();
    }


    private Server.ClientHandler createClientHandler(String input) throws IOException {
        inputStream = new ByteArrayInputStream(input.getBytes());
        outputStream = new ByteArrayOutputStream();
        out = new PrintWriter(outputStream, true);

        when(clientSocket.getInputStream()).thenReturn(inputStream);
        when(clientSocket.getOutputStream()).thenReturn(outputStream);

        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);
        clientHandler.out = out;
        return clientHandler;
    }

    private PreparedStatement setupMockDatabase(boolean userExists, String password) throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(userExists);
        when(mockResultSet.getString("password")).thenReturn(password);
        return mockStatement;
    }

    private void verifyOutput(String expectedOutput) {
        String output = outputStream.toString();
        assertTrue(output.contains(expectedOutput), output);
    }

    @Test
    void testPerformSignIn() throws Exception {
        String input = "signInUser user1 password1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockDatabase(true, "password1");

        clientHandler.run();

        InOrder inOrder = inOrder(mockStatement);
        inOrder.verify(mockStatement).setString(1, "user1");
        inOrder.verify(mockStatement).setString(2, "password1");

        verifyOutput("User logged in");
    }

    @Test
    void testPerformSignInUserDoesNotExist() throws Exception {
        String input = "signInUser user2 password2";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockDatabase(false, null);

        clientHandler.run();

        InOrder inOrder = inOrder(mockStatement);
        inOrder.verify(mockStatement).setString(1, "user2");

        verifyOutput("User with this name not found");
    }

    @Test
    void testPerformSignInInvalidCommandFormat() throws Exception {
        String input = "signInUserInvalidFormat";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    private PreparedStatement setupPreparedStatementMock(String sqlQuery, boolean resultSetHasNext) throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(connection.prepareStatement(sqlQuery)).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(resultSetHasNext);
        return mockStatement;
    }

    @Test
    void testCheckIsUserRegistered_UserExists() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ?";
        PreparedStatement mockStatement = setupPreparedStatementMock(sqlQuery, true);

        assertTrue(server.new ClientHandler(clientSocket).checkIsUserRegistered("user1"));

        verify(mockStatement).setString(1, "user1");
    }

    @Test
    void testCheckIsUserRegistered_UserDoesNotExist() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ?";
        PreparedStatement mockStatement = setupPreparedStatementMock(sqlQuery, false);

        assertFalse(server.new ClientHandler(clientSocket).checkIsUserRegistered("user2"));

        verify(mockStatement).setString(1, "user2");
    }

    @Test
    void testCheckPassword_CorrectPassword() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement mockStatement = setupPreparedStatementMock(sqlQuery, true);

        assertTrue(server.new ClientHandler(clientSocket).checkPassword("user1", "password1"));

        verify(mockStatement).setString(1, "user1");
        verify(mockStatement).setString(2, "password1");
    }

    @Test
    void testCheckPassword_IncorrectPassword() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement mockStatement = setupPreparedStatementMock(sqlQuery, false);

        assertFalse(server.new ClientHandler(clientSocket).checkPassword("user1", "wrongPassword"));

        verify(mockStatement).setString(1, "user1");
        verify(mockStatement).setString(2, "wrongPassword");
    }

    private PreparedStatement setupMockPreparedStatementForAddUser() throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO users (username, password, password_salt) VALUES (?, ?, ?)")).thenReturn(mockStatement);
        return mockStatement;
    }

    @Test
    void testPerformAddUser_CorrectFormat() throws Exception {
        String input = "addUser user1 password1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockPreparedStatementForAddUser();

        clientHandler.run();

        InOrder inOrder = inOrder(mockStatement);
        inOrder.verify(mockStatement).setString(1, "user1");
        inOrder.verify(mockStatement).setString(2, "password1");
        inOrder.verify(mockStatement).setString(3, Integer.toString("password1".hashCode()));

        verifyOutput("New user added");
    }

    @Test
    void testPerformAddUser_InvalidCommandFormat() throws Exception {
        String input = "addUserInvalidFormat";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testAddUser() throws SQLException {
        PreparedStatement mockStatement = setupMockPreparedStatementForAddUser();
        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);

        clientHandler.addUser("user1", "password1", Integer.toString("password1".hashCode()));

        InOrder inOrder = inOrder(mockStatement);
        inOrder.verify(mockStatement).setString(1, "user1");
        inOrder.verify(mockStatement).setString(2, "password1");
        inOrder.verify(mockStatement).setString(3, Integer.toString("password1".hashCode()));
        inOrder.verify(mockStatement).executeUpdate();
    }

    private PreparedStatement setupMockCheckUserStatement(boolean userExists) throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT * FROM users WHERE username = ?")).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(userExists);
        return mockStatement;
    }

    private PreparedStatement setupMockInsertUserStatement() throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO users (username, password, password_salt) VALUES (?, ?, ?)")).thenReturn(mockStatement);
        return mockStatement;
    }

    @Test
    void testPerformSignUp_Success() throws Exception {
        String input = "signUpUser user3 password3 salt3";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement checkUserStatement = setupMockCheckUserStatement(false);
        PreparedStatement insertUserStatement = setupMockInsertUserStatement();

        clientHandler.run();

        verify(checkUserStatement).setString(1, "user3");
        verify(checkUserStatement).executeQuery();

        verify(insertUserStatement).setString(1, "user3");
        verify(insertUserStatement).setString(2, "password3");
        verify(insertUserStatement).setString(3, "salt3");
        verify(insertUserStatement).executeUpdate();

        verifyOutput("User is registered");
    }

    @Test
    void testPerformSignUp_UserAlreadyExists() throws Exception {
        String input = "signUpUser user3 password3 salt3";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement checkUserStatement = setupMockCheckUserStatement(true);

        clientHandler.run();

        verify(checkUserStatement).setString(1, "user3");
        verify(checkUserStatement).executeQuery();

        verifyOutput("User with the same name already exists");
    }

    @Test
    void testPerformSignUp_InvalidCommandFormat() throws Exception {
        String input = "signUpUserInvalidFormat";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    private PreparedStatement setupMockDocumentRetrieval() throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT filename, file_path, access FROM user_documents WHERE username = ?")).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        return mockStatement;
    }

    @Test
    void testPerformGetDocuments_Success() throws Exception {
        String input = "getDocuments user1";
        Server.ClientHandler clientHandler = createClientHandler(input);

        PreparedStatement mockStatement = setupMockDocumentRetrieval();
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("filename")).thenReturn("file1");
        when(mockResultSet.getString("file_path")).thenReturn("/path/to/file1");
        when(mockResultSet.getString("access")).thenReturn("read");

        clientHandler.run();

        verify(mockStatement).setString(1, "user1");
        verifyOutput("readfile1 /path/to/file1");
    }

    @Test
    void testPerformGetDocuments_InvalidCommandFormat() throws Exception {
        String input = "getDocumentsInvalidFormat";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testGetUserDocuments() throws SQLException {
        PreparedStatement mockStatement = setupMockDocumentRetrieval();
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("filename")).thenReturn("file1");
        when(mockResultSet.getString("file_path")).thenReturn("/path/to/file1");
        when(mockResultSet.getString("access")).thenReturn("read");

        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);
        Map<String, String> documents = clientHandler.getUserDocuments("user1");

        verify(mockStatement).setString(1, "user1");
        assertEquals(1, documents.size());
        assertEquals("/path/to/file1", documents.get("readfile1"));
    }

    private PreparedStatement mockPreparedStatement(String query, boolean fileExists, String filePath, String access) throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(query)).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(fileExists);

        if (fileExists) {
            when(resultSet.getString("file_path")).thenReturn(filePath);
            when(resultSet.getString("access")).thenReturn(access);
        }

        return statement;
    }

    private void verifyPreparedStatement(PreparedStatement statement, String username, int fileId) throws SQLException {
        verify(statement).setString(1, username);
        verify(statement).setInt(2, fileId);
    }

    @Test
    void testPerformOpenDocumentById() throws Exception {
        String input = "openDocumentById user1 1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement selectStatement = mockPreparedStatement("SELECT file_path, access FROM user_documents WHERE username = ? AND file_id = ?", true, "/path/to/file1", "read");
        PreparedStatement updateStatement = mockPreparedStatement("UPDATE user_documents SET lock = 1 WHERE username = ? AND file_id = ?", true, null, null);

        clientHandler.run();

        verifyPreparedStatement(selectStatement, "user1", 1);
        verifyPreparedStatement(updateStatement, "user1", 1);

        String output = outputStream.toString();
        assertTrue(output.contains("/path/to/file1 read"));
    }


    @Test
    void testCheckAndUpdateFileLock_FileExists() throws SQLException, IOException {
        String username = "user1";
        int fileId = 123;
        String expectedFilePath = "/path/to/file";
        String expectedAccess = "read";
        mockPreparedStatement("SELECT file_path, access FROM user_documents WHERE username = ? AND file_id = ?", true, expectedFilePath, expectedAccess);
        PreparedStatement updateStatement = mockPreparedStatement("UPDATE user_documents SET lock = 1 WHERE username = ? AND file_id = ?", true, null, null);
        Server.ClientHandler clientHandler = createClientHandler("");

        String result = clientHandler.checkAndUpdateFileLock(username, fileId);

        assertEquals(expectedFilePath + " " + expectedAccess, result);
        verifyPreparedStatement(updateStatement, username, fileId);
    }

    @Test
    void testCheckAndUpdateFileLock_FileDoesNotExist() throws SQLException, IOException {
        String username = "user1";
        int fileId = 123;
        mockPreparedStatement("SELECT file_path, access FROM user_documents WHERE username = ? AND file_id = ?", false, null, null);
        Server.ClientHandler clientHandler = createClientHandler("");

        String result = clientHandler.checkAndUpdateFileLock(username, fileId);

        assertEquals("No such file", result);
    }
}
