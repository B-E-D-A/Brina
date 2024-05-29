package org.hse.brina.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Класс Client представляет собой клиент для сетевого взаимодействия с сервером приложения.
 */

public class Client {
    private static final Logger logger = LogManager.getLogger();
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;


    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            logger.info("Connected to server");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public String receiveMessage() {
        try {
            return in.readLine();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public void stop() {
        try {
            in.close();
            out.close();
            socket.close();
            logger.info("Connection closed");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public String getName() {
        return username;
    }

    public void setName(String name) {
        this.username = name;
    }
}