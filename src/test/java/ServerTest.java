import org.hse.brina.Config;
import org.hse.brina.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Класс ServerTest - unit тесты, проверяющие каждый из методов класса Server на корректность
 */

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
    void testServerInitialization() {
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
    void testClientConnection() {
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
    void testMultipleClientConnections() {
        try {
            Socket clientSocket1 = new Socket("localhost", 8080);
            Socket clientSocket2 = new Socket("localhost", 8080);
            Socket clientSocket3 = new Socket("localhost", 8080);

            assertTrue(clientSocket1.isConnected());
            assertTrue(clientSocket2.isConnected());
            assertTrue(clientSocket3.isConnected());
        } catch (IOException e) {
            fail("Failed to connect to the server: " + e.getMessage());
        } finally {
            server.stop();
        }
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
        out.flush();
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

    private PreparedStatement setupMockForSignIn(String sqlQuery, boolean resultSetHasNext) throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(connection.prepareStatement(sqlQuery)).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(resultSetHasNext);
        return mockStatement;
    }

    @Test
    void testCheckIsUserRegistered() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ?";
        PreparedStatement mockStatement = setupMockForSignIn(sqlQuery, true);

        assertTrue(server.new ClientHandler(clientSocket).checkIsUserRegistered("user1"));

        verify(mockStatement).setString(1, "user1");
    }

    @Test
    void testCheckIsUserRegisteredUserDoesNotExist() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ?";
        PreparedStatement mockStatement = setupMockForSignIn(sqlQuery, false);

        assertFalse(server.new ClientHandler(clientSocket).checkIsUserRegistered("user2"));

        verify(mockStatement).setString(1, "user2");
    }

    @Test
    void testCheckPassword() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement mockStatement = setupMockForSignIn(sqlQuery, true);

        assertTrue(server.new ClientHandler(clientSocket).checkPassword("user1", "password1"));

        verify(mockStatement).setString(1, "user1");
        verify(mockStatement).setString(2, "password1");
    }

    @Test
    void testCheckPasswordIncorrectPassword() throws SQLException {
        String sqlQuery = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement mockStatement = setupMockForSignIn(sqlQuery, false);

        assertFalse(server.new ClientHandler(clientSocket).checkPassword("user1", "wrongPassword"));

        verify(mockStatement).setString(1, "user1");
        verify(mockStatement).setString(2, "wrongPassword");
    }

    private PreparedStatement setupMockForAddUser() throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO users (username, password, password_salt) VALUES (?, ?, ?)")).thenReturn(mockStatement);
        return mockStatement;
    }

    @Test
    void testPerformAddUser() throws Exception {
        String input = "addUser user1 password1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockForAddUser();

        clientHandler.run();

        InOrder inOrder = inOrder(mockStatement);
        inOrder.verify(mockStatement).setString(1, "user1");
        inOrder.verify(mockStatement).setString(2, "password1");
        inOrder.verify(mockStatement).setString(3, Integer.toString("password1".hashCode()));

        verifyOutput("New user added");
    }

    @Test
    void testPerformAddUserInvalidCommandFormat() throws Exception {
        String input = "addUserInvalidFormat";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testAddUser() throws SQLException {
        PreparedStatement mockStatement = setupMockForAddUser();
        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);

        clientHandler.addUser("user1", "password1", Integer.toString("password1".hashCode()));

        InOrder inOrder = inOrder(mockStatement);
        inOrder.verify(mockStatement).setString(1, "user1");
        inOrder.verify(mockStatement).setString(2, "password1");
        inOrder.verify(mockStatement).setString(3, Integer.toString("password1".hashCode()));
        inOrder.verify(mockStatement).executeUpdate();
    }

    private PreparedStatement setupMockCheckUser(boolean userExists) throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT * FROM users WHERE username = ?")).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(userExists);
        return mockStatement;
    }

    @Test
    void testPerformSignUp() throws Exception {
        String input = "signUpUser user3 password3 salt3";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement checkUserStatement = setupMockCheckUser(false);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO users (username, password, password_salt) VALUES (?, ?, ?)")).thenReturn(mockStatement);

        clientHandler.run();

        verify(checkUserStatement).setString(1, "user3");
        verify(checkUserStatement).executeQuery();

        verify(mockStatement).setString(1, "user3");
        verify(mockStatement).setString(2, "password3");
        verify(mockStatement).setString(3, "salt3");
        verify(mockStatement).executeUpdate();

        verifyOutput("User is registered");
    }

    @Test
    void testPerformSignUpUserAlreadyExists() throws Exception {
        String input = "signUpUser user3 password3 salt3";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement checkUserStatement = setupMockCheckUser(true);

        clientHandler.run();

        verify(checkUserStatement).setString(1, "user3");
        verify(checkUserStatement).executeQuery();

        verifyOutput("User with the same name already exists");
    }

    @Test
    void testPerformSignUpInvalidCommandFormat() throws Exception {
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
    void testPerformGetDocuments() throws Exception {
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
    void testPerformGetDocumentsInvalidCommandFormat() throws Exception {
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

    private PreparedStatement mockFilePrepare(String query, boolean isFileExists, String filePath, String access) throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(query)).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(isFileExists);

        if (isFileExists) {
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
        PreparedStatement selectStatement = mockFilePrepare("SELECT file_path, access FROM user_documents WHERE username = ? AND file_id = ?", true, "/path/to/file1", "read");
        PreparedStatement updateStatement = mockFilePrepare("UPDATE user_documents SET lock = 1 WHERE username = ? AND file_id = ?", true, null, null);

        clientHandler.run();

        verifyPreparedStatement(selectStatement, "user1", 1);
        verifyPreparedStatement(updateStatement, "user1", 1);

        verifyOutput("/path/to/file1 read");
    }


    @Test
    void testCheckAndUpdateFileLock() throws SQLException, IOException {
        String username = "user1";
        int fileId = 123;
        String expectedFilePath = "/path/to/file";
        String expectedAccess = "read";
        mockFilePrepare("SELECT file_path, access FROM user_documents WHERE username = ? AND file_id = ?", true, expectedFilePath, expectedAccess);
        PreparedStatement updateStatement = mockFilePrepare("UPDATE user_documents SET lock = 1 WHERE username = ? AND file_id = ?", true, null, null);
        Server.ClientHandler clientHandler = createClientHandler("");

        String result = clientHandler.checkAndUpdateFileLock(username, fileId);

        assertEquals(expectedFilePath + " " + expectedAccess, result);
        verifyPreparedStatement(updateStatement, username, fileId);
    }

    @Test
    void testCheckAndUpdateFileLockFileDoesNotExist() throws SQLException, IOException {
        String username = "user1";
        int fileId = 123;
        mockFilePrepare("SELECT file_path, access FROM user_documents WHERE username = ? AND file_id = ?", false, null, null);
        Server.ClientHandler clientHandler = createClientHandler("");

        String result = clientHandler.checkAndUpdateFileLock(username, fileId);

        assertEquals("No such file", result);
    }

    private void verifyAddDocument(PreparedStatement statement, String username, String filename, String name, int m_lock) throws SQLException {
        verify(statement).setString(1, username);
        verify(statement).setString(2, filename);
        String expectedFilePath = Config.getProjectPath().substring(0, Config.getProjectPath().length() - Config.getAppNameLength()) + "documents/" + filename;
        verify(statement).setString(3, expectedFilePath);
        verify(statement).setInt(4, Math.abs(name.hashCode()));
        verify(statement).setString(5, "w");
        verify(statement).setInt(6, m_lock);
    }

    private PreparedStatement setupMockStatement() throws SQLException {
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(mockStatement);
        return mockStatement;
    }

    @Test
    void testPerformSaveDocument() throws Exception {
        String input = "saveDocument file1 user1 documentName";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();

        clientHandler.run();

        verifyAddDocument(mockStatement, "user1", "file1", "documentName", 0);
        verify(mockStatement).executeUpdate();
        verifyOutput("Document saved");
    }

    @Test
    void testPerformSaveDocumentInvalidCommandFormat() throws Exception {
        String input = "saveDocument file1 user1";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testAddDocument() throws SQLException {
        PreparedStatement mockStatement = setupMockStatement();
        String username = "user1";
        String filename = "file1";
        String name = "documentName";
        int m_lock = 0;
        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);

        clientHandler.addDocument(filename, username, name, m_lock);

        verifyAddDocument(mockStatement, username, filename, name, m_lock);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testPerformAddDocumentById() throws Exception {
        String input = "addDocumentById user1 file1 read";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();

        clientHandler.run();

        verifySaveDocumentById(mockStatement, "user1", "file1", "read");
        verify(mockStatement).executeUpdate();
        verifyOutput("Document saved");
    }

    private void verifySaveDocumentById(PreparedStatement statement, String username, String filename, String access) throws SQLException {
        verify(statement).setString(1, username);
        String documentName = filename + ".rtfx";
        verify(statement).setString(2, documentName);
        String expectedFilePath = Config.getProjectPath().substring(0, Config.getProjectPath().length() - Config.getAppNameLength()) + "documents/" + documentName;
        verify(statement).setString(3, expectedFilePath);
        verify(statement).setInt(4, Math.abs(filename.hashCode()));
        verify(statement).setString(5, access);
        verify(statement).setInt(6, 1);
    }

    @Test
    void testPerformAddDocumentByIdInvalidInput() throws Exception {
        String input = "addDocumentById user1 file1";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testSaveDocumentById() throws SQLException {
        PreparedStatement mockStatement = setupMockStatement();
        String username = "user1";
        String filename = "file1";
        String access = "read";
        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);

        clientHandler.saveDocumentById(username, filename, access);

        verifySaveDocumentById(mockStatement, username, filename, access);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testPerformUnlockDocument() throws Exception {
        String documentId = "123";
        int documentIdHash = documentId.hashCode();
        String input = "unlockDocument " + documentId;
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = setupMockResultSet(mockStatement);

        when(mockResultSet.next()).thenReturn(true);

        clientHandler.run();

        verify(mockStatement, times(1)).setInt(1, documentIdHash);
        verify(mockStatement).executeQuery();
        verify(mockStatement).setInt(1, 0);
        verify(mockStatement).executeUpdate();
        verifyOutput("Document unlocked");
    }

    @Test
    void testPerformUnlockDocumentInvalidInput() throws Exception {
        String input = "unlockDocument";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testPerformLockDocumentDocumentLocked() throws Exception {
        String input = "lockDocument 123";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = setupMockResultSet(mockStatement);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("lock")).thenReturn(0);

        clientHandler.run();

        verify(mockStatement, times(2)).setInt(1, 123);
        verify(mockStatement).executeUpdate();
        verifyOutput("Document locked");
    }

    private ResultSet setupMockResultSet(PreparedStatement mockStatement) throws SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        return mockResultSet;
    }

    @Test
    void testPerformLockDocumentDocumentClosed() throws Exception {
        String input = "lockDocument 123";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = setupMockResultSet(mockStatement);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("lock")).thenReturn(1);

        clientHandler.run();

        verify(mockStatement, times(2)).setInt(1, 123);
        verify(mockStatement).executeUpdate();
        verifyOutput("Document closed");
    }

    @Test
    void testPerformLockDocumentInvalidInput() throws Exception {
        String input = "lockDocument";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testPerformGetLock() throws Exception {
        String input = "getLock documentName.rtfx";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = setupMockResultSet(mockStatement);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("lock")).thenReturn(1);

        clientHandler.run();

        verify(mockStatement).setInt(1, "documentName".hashCode());
        verifyOutput("1");
    }

    @Test
    void testPerformGetLockInvalidInput() throws Exception {
        String input = "getLock";
        Server.ClientHandler clientHandler = createClientHandler(input);

        clientHandler.run();

        verifyOutput("Invalid command format");
    }

    @Test
    void testSetLockStatus() throws SQLException {
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = setupMockResultSet(mockStatement);
        when(mockResultSet.next()).thenReturn(true);

        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);
        clientHandler.setLockStatus(123, 0);

        verify(mockStatement).setInt(1, 123);
        verify(mockStatement).executeQuery();
        verify(mockStatement).setInt(1, 0);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testGetLockStatus() throws SQLException {
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = setupMockResultSet(mockStatement);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("lock")).thenReturn(1);

        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);
        int lockStatus = clientHandler.getLockStatus(123);

        verify(mockStatement).setInt(1, 123);
        assertEquals(1, lockStatus);
    }

    @Test
    void testPerformAddFriendUserExists() throws Exception {
        String input = "addFriend user1 friend1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = setupMockResultSet(mockStatement);

        when(mockResultSet.next()).thenReturn(true);

        clientHandler.run();

        verify(mockStatement, times(5)).setString(anyInt(), anyString());
        verify(mockStatement, times(2)).executeUpdate();
        verifyOutput("Friend added successfully");
    }

    @Test
    void testPerformAddFriendUserDoesNotExist() throws Exception {
        String input = "addFriend user1 friend1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        clientHandler.run();

        verify(mockStatement, never()).executeUpdate();
        verifyOutput("User friend1 not found");
    }

    @Test
    void testAddFriend() throws SQLException {
        PreparedStatement mockStatement = setupMockStatement();
        String username = "user1";
        String friendName = "friend1";
        Server.ClientHandler clientHandler = server.new ClientHandler(clientSocket);

        clientHandler.addFriend(username, friendName);

        verify(mockStatement).setString(1, username);
        verify(mockStatement).setString(2, friendName);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testPerformGetFriendsListFriendsExists() throws Exception {
        String input = "getFriendsList user1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("friend")).thenReturn("friend1").thenReturn("friend2");

        clientHandler.run();

        verifyOutput("friend1 friend2");
    }

    @Test
    void testPerformGetFriendsListFriendsDoesntExists() throws Exception {
        String input = "getFriendsList user1";
        Server.ClientHandler clientHandler = createClientHandler(input);
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        clientHandler.run();

        verifyOutput("No friends found");
    }

    @Test
    void testGetFriendsListFriendsExists() throws SQLException {
        String username = "user1";
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("friend")).thenReturn("friend1").thenReturn("friend2");

        Server.ClientHandler clientHandler = server.new ClientHandler(mock(Socket.class));
        String friendsList = clientHandler.getFriendsList(username);

        assertEquals("friend1 friend2", friendsList);
    }

    @Test
    void testGetFriendsListFriendsDoesntExists() throws SQLException {
        String username = "user1";
        PreparedStatement mockStatement = setupMockStatement();
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        Server.ClientHandler clientHandler = server.new ClientHandler(mock(Socket.class));
        String friendsList = clientHandler.getFriendsList(username);

        assertEquals("", friendsList);
    }
}