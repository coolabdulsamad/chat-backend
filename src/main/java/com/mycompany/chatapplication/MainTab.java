/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.chatapplication;

import Application_Connector.db.DatabaseHelper;
import MainStart_Test.TypingEffectDemo;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainTab {
public static void main(String[] args) {
    System.out.println("Main method reached!");
    
    try {
    Class.forName("org.sqlite.JDBC");
} catch (ClassNotFoundException e) {
    e.printStackTrace();
}


    new Thread(() -> {
        try {
            System.out.println("Attempting to create server on port 5000...");
            HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);

            // Health Check
            server.createContext("/", exchange -> {
                String response = "NovaMobile is running!";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                System.out.println("Health check received");
            });

            // Login Handler
            server.createContext("/login", exchange -> {
                System.out.println("Login request received");
                if ("POST".equals(exchange.getRequestMethod())) {
                    try {
                        InputStream inputStream = exchange.getRequestBody();
                        String requestBody = new String(inputStream.readAllBytes());
                        inputStream.close();

                        JSONObject json = new JSONObject(requestBody);
                        String usernameOrEmail = json.getString("username");
                        String password = json.getString("password");

                        DatabaseHelper dbHelper = new DatabaseHelper();
                        String userId = dbHelper.validateLogin(usernameOrEmail, password);

                        if (userId != null) {
                            String response = "Login successful!";
                            exchange.sendResponseHeaders(200, response.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        } else {
                            String response = "Invalid username or password";
                            exchange.sendResponseHeaders(401, response.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        String response = "Server error: " + e.getMessage();
                        exchange.sendResponseHeaders(500, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });

            // Signup Handler
            server.createContext("/signup", exchange -> {
                System.out.println("Signup request received");
                if ("POST".equals(exchange.getRequestMethod())) {
                    try {
                        InputStream inputStream = exchange.getRequestBody();
                        String requestBody = new String(inputStream.readAllBytes());
                        inputStream.close();

                        JSONObject json = new JSONObject(requestBody);
                        String username = json.getString("username");
                        String email = json.getString("email");
                        String password = json.getString("password");
                        String firstName = json.getString("firstName");
                        String lastName = json.getString("lastName");
                        String phoneNumber = json.getString("phoneNumber");
                        String country = json.getString("country");
                        String dob = json.getString("dob");

                        DatabaseHelper dbHelper = new DatabaseHelper();

                        if (dbHelper.doesUserExist(username, email)) {
                            String response = "User already exists";
                            exchange.sendResponseHeaders(409, response.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                            return;
                        }

                        boolean isSuccess = dbHelper.signup(username, email, password, firstName, lastName, dob, country, phoneNumber);

                        if (isSuccess) {
                            String response = "Signup successful!";
                            exchange.sendResponseHeaders(200, response.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        } else {
                            String response = "Signup failed!";
                            exchange.sendResponseHeaders(500, response.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        String response = "Server error: " + e.getMessage();
                        exchange.sendResponseHeaders(500, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });
            
            // Sync Group Messages Handler
server.createContext("/syncGroupMessages", exchange -> {
    System.out.println("Group sync request received");
    
    if ("GET".equals(exchange.getRequestMethod())) {
        try {
            String query = exchange.getRequestURI().getQuery(); // Extract query params
            int lastMessageId = 0;
            int groupId = 0;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if ("lastMessageId".equals(pair[0])) {
                        lastMessageId = Integer.parseInt(pair[1]);
                    } else if ("groupId".equals(pair[0])) {
                        groupId = Integer.parseInt(pair[1]);
                    }
                }
            }

            DatabaseHelper dbHelper = new DatabaseHelper();
            JSONArray messages = dbHelper.getGroupMessages(lastMessageId, groupId);

            String response = messages.toString();
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

            System.out.println("Sent group messages: " + response);

        } catch (Exception e) {
            e.printStackTrace();
            String response = "Error: " + e.getMessage();
            exchange.sendResponseHeaders(500, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    } else {
        exchange.sendResponseHeaders(405, -1); // Method Not Allowed
    }
});


            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            System.out.println("Server started successfully on port 5000");

        } catch (IOException e) {
            System.out.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }).start(); // <-- Start server on a separate thread!



    SwingUtilities.invokeLater(() -> {
        if (!attemptAutoLogin()) {  // If auto-login fails, show Welcome Screen
            showWelcomeScreen();
        }
    });

    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:chatting_app.db");
         Statement stmt = conn.createStatement()) {
        stmt.execute("PRAGMA journal_mode=WAL;");
    } catch (SQLException e) {
        e.printStackTrace();
    }}


    // Auto-login method (returns true if login succeeds)
    private static boolean attemptAutoLogin() {
        Preferences prefs = Preferences.userNodeForPackage(MainTab.class);
        String savedEmail = prefs.get("rememberedEmail", "");
        String savedPassword = prefs.get("rememberedPassword", "");

        if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
            DatabaseHelper dbHelper = new DatabaseHelper();
            String userId = dbHelper.validateLogin(savedEmail, savedPassword);

            if (userId != null) {
                int loggedInUserId = Integer.parseInt(userId);
                dbHelper.updateOnlineStatus(loggedInUserId, "Online");

                String username = dbHelper.getUsernameById(loggedInUserId);
                //JOptionPane.showMessageDialog(null, "Auto-login successful!");

                new Dashboard(username).setVisible(true);
                UserSession.setCurrentUsername(loggedInUserId, username);
                return true;  // Auto-login successful, no need to show Welcome Screen
            }
        }
        return false;  // Auto-login failed, show Welcome Screen
    }

    // Show the Welcome Screen (MainTab)
    private static void showWelcomeScreen() {
        JFrame frame = new JFrame("NovaMobile");
        frame.setSize(1000, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        frame.getContentPane().setBackground(new Color(225, 233, 241));
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        URL iconURL = MainTab.class.getClassLoader().getResource("icons/novamobile.jpg");
        if (iconURL != null) {
            frame.setIconImage(new ImageIcon(iconURL).getImage());
        } else {
            System.err.println("Icon image not found.");
        }

        JPanel typingPanel = TypingEffectDemo.createTypingEffectPanel(frame);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        frame.add(typingPanel, gbc);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}




// Window.setResizable(false)