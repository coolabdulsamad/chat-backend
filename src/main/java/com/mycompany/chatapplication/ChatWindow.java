/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.chatapplication;

import Application_Connector.db.DatabaseHelper;
import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;
//import com.sun.glass.ui.Clipboard;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.awt.datatransfer.Clipboard;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;

import java.util.List;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.filechooser.FileSystemView;
import javax.sound.sampled.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChatWindow extends JPanel {
    private JPanel friendsListPanel;
    private JLabel sentline;
    private JPanel chatPanel;
    private JLabel chatHeader;
    private JLabel chatProfilePic;
    private JLabel chatStatusLabel;
    private JTextField messageInputField;
    private JButton sendButton;
    private JButton attachButton;
    private JPanel chatMessagesPanel;
    private JScrollPane chatScrollPane;
    private int loggedInUserId;
    private int currentChatFriendId;
    private File selectedAttachment;
    private byte[] attachmentData;
    
    private JPanel replyPanel;
private JLabel replyTextLabel;
private int replyingToMessageId = -1;

private int messageId;
private       String messageText;

private JLabel replyLabel;

private static final String URL = "jdbc:sqlite:chatting_app.db";

private int messageIdCounter = 1;

private JPanel pinnedMessagesPanel;

private JLabel unreadMessagesBadge;
private int unreadMessageCount = 0;

private final Set<Integer> notifiedFriends = new HashSet<>();

private JPanel friendsSectionPanel;
private JPanel groupsSectionPanel;
private int currentGroupId = -1; // -1 means no group is selected
private int lastGroupMessageId = -1;
private Timer groupTypingStatusPollingTimer;
private JButton micButton;
private boolean isRecording = false;
private TargetDataLine audioLine;
private File recordedAudioFile;
private int chatUserId;
private String chatUsername;
private Dashboard parentDashboard;

private int lastLoadedMessageId = -1;


private int generateMessageId() {
    // Example: Get max message_id from DB and add 1 (Simple logic)
    try (Connection conn = DriverManager.getConnection(URL);
         PreparedStatement stmt = conn.prepareStatement("SELECT MAX(message_id) FROM Messages");
         ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
            return rs.getInt(1) + 1;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 1; // Default if no messages
}

    
     public ChatWindow(String username, int userId, Dashboard dashboard) {
        this.loggedInUserId = userId;
        this.chatUserId = userId;
    this.chatUsername = username;
    this.parentDashboard = dashboard;   
    
     System.out.println("ChatWindow opened for: " + chatUsername + " (User ID: " + chatUserId + ")");
    //setTitle("Chat with " + chatUsername); // Ensure correct user is displayed
    //loadChatMessages();
    
        setSize(500, 600);
        setLayout(new BorderLayout());
        
        URL iconURL = MainTab.class.getClassLoader().getResource("icons/novamobile.jpg");
if (iconURL != null) {
    //setIconImage(new ImageIcon(iconURL).getImage());
} else {
    System.err.println("Icon image not found.");
}

        startPollingForGroups();
        startPollingForMessages();
        startPollingOnlineStatus();
        startTypingStatusPolling();
        
        addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) {
        stopSyncing(); // ✅ Stop when closing
        Window window = SwingUtilities.getWindowAncestor(chatPanel);
if (window != null) {
    window.dispose(); // ✅ Close window safely
}

    }
});


    // ✅ Initialize Panels First (Prevents NullPointerException)
    friendsSectionPanel = new JPanel();
    friendsSectionPanel.setLayout(new BoxLayout(friendsSectionPanel, BoxLayout.Y_AXIS));

    groupsSectionPanel = new JPanel();
    groupsSectionPanel.setLayout(new BoxLayout(groupsSectionPanel, BoxLayout.Y_AXIS));

    friendsListPanel = new JPanel();
    friendsListPanel.setLayout(new BoxLayout(friendsListPanel, BoxLayout.Y_AXIS));

    chatMessagesPanel = new JPanel();
    chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));

    // ✅ Add Components to the UI
    JScrollPane friendsScrollPane = new JScrollPane(friendsSectionPanel);
    friendsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    JScrollPane groupsScrollPane = new JScrollPane(groupsSectionPanel);
    groupsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    JPanel splitPanel = new JPanel(new GridLayout(2, 1));
    splitPanel.add(friendsScrollPane);
    splitPanel.add(groupsScrollPane);

    JPanel friendsContainerPanel = new JPanel(new BorderLayout());
    friendsContainerPanel.add(splitPanel, BorderLayout.CENTER);
    add(friendsContainerPanel, BorderLayout.WEST);

    // ✅ NOW Call fetchFriends() & fetchGroups() After Initialization


        fetchFriends();
        fetchGroups();

        // Chat Panel (Right Side)
        chatPanel = new JPanel(new BorderLayout());
        add(chatPanel, BorderLayout.CENTER);

        // Chat Header (Friend’s Details)
        JPanel chatHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chatProfilePic = new JLabel(); // Profile image
        chatHeader = new JLabel("Select a friend to chat");
        chatStatusLabel = new JLabel(""); // Typing status
        sentline = new JLabel("");
        chatHeaderPanel.add(chatProfilePic);
        chatHeaderPanel.add(chatHeader);
        chatHeaderPanel.add(chatStatusLabel);
        chatHeaderPanel.add(sentline);
        chatPanel.add(chatHeaderPanel, BorderLayout.NORTH); 

        // Chat Messages Area
chatMessagesPanel = new JPanel();
chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));

pinnedMessagesPanel = new JPanel();
pinnedMessagesPanel.setLayout(new BoxLayout(pinnedMessagesPanel, BoxLayout.Y_AXIS));
pinnedMessagesPanel.setBackground(new Color(240, 240, 240));
pinnedMessagesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
pinnedMessagesPanel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));


JLabel pinnedTitleLabel = new JLabel("📌 Pinned Messages");
pinnedTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
pinnedTitleLabel.setForeground(Color.DARK_GRAY);
pinnedTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

pinnedMessagesPanel.add(pinnedTitleLabel);

JSeparator separator = new JSeparator();
pinnedMessagesPanel.add(separator);

pinnedMessagesPanel.add(pinnedTitleLabel);

// Combined Panel for pinned + messages
JPanel mainMessagesPanel = new JPanel();
mainMessagesPanel.setLayout(new BoxLayout(mainMessagesPanel, BoxLayout.Y_AXIS));
mainMessagesPanel.add(pinnedMessagesPanel);
mainMessagesPanel.add(chatMessagesPanel);

chatScrollPane = new JScrollPane(mainMessagesPanel);
chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

chatScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
    JScrollBar scrollBar = chatScrollPane.getVerticalScrollBar();
    boolean isAtBottom = scrollBar.getValue() + scrollBar.getVisibleAmount() == scrollBar.getMaximum();

    if (isAtBottom) {
        unreadMessagesBadge.setVisible(false);
        unreadMessageCount = 0;
    }
});
chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        
        replyLabel = new JLabel();
replyLabel.setForeground(Color.GRAY);
replyLabel.setVisible(false);
// Add this above the message input field in your layout


        
        // Message Input Field, Send & Attach Button
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageInputField = new JTextField();
        sendButton = new JButton("Send");
        attachButton = new JButton("Attach");
        
         messageInputField.addKeyListener(new KeyAdapter() {
    private Timer typingTimer = new Timer(2000, e -> setTypingStatus("Not Typing")); // Stop typing after 2 sec

    @Override
    public void keyPressed(KeyEvent e) {
        setTypingStatus("Typing...");
        typingTimer.restart();
    }
});
         messageInputField.addKeyListener(new KeyAdapter() {
    private Timer typingTimer;

    @Override
    public void keyTyped(KeyEvent e) {
        if (currentGroupId != -1) { // Only for group chats
            updateTypingStatus(currentGroupId, true);
            if (typingTimer != null) {
                typingTimer.stop();
            }
            typingTimer = new Timer(2000, evt -> updateTypingStatus(currentGroupId, false));
            typingTimer.setRepeats(false);
            typingTimer.start();
        }
    }
});
                 micButton = new JButton("🎤"); // Mic Icon
        micButton.setFocusPainted(false);
        micButton.addActionListener(e -> handleMicButtonClick());
        
         
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); // Panel to hold Mic + Send Buttons
rightButtonPanel.add(micButton);
rightButtonPanel.add(sendButton);

inputPanel.add(attachButton, BorderLayout.WEST);
inputPanel.add(messageInputField, BorderLayout.CENTER);
inputPanel.add(rightButtonPanel, BorderLayout.EAST); // Mic + Send in Right Panel
inputPanel.add(replyLabel, BorderLayout.NORTH);


        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // Attach Button Action
        attachButton.addActionListener(e -> selectAttachment());

        // Send Button Action
        sendButton.addActionListener(e -> {
            String message = messageInputField.getText().trim();
            if (!message.isEmpty() || selectedAttachment != null) {
                sendMessage(message, selectedAttachment);
                selectedAttachment = null; // Reset attachment after sending
                
            }
        });
        

        
        unreadMessagesBadge = new JLabel();
unreadMessagesBadge.setOpaque(true);
unreadMessagesBadge.setBackground(Color.RED);
unreadMessagesBadge.setForeground(Color.WHITE);
unreadMessagesBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
unreadMessagesBadge.setHorizontalAlignment(SwingConstants.CENTER);
unreadMessagesBadge.setVisible(false);
unreadMessagesBadge.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

// Create a layered pane to overlay the badge on top of the chatScrollPane
JLayeredPane layeredPane = new JLayeredPane();
layeredPane.setLayout(null); // Absolute positioning
layeredPane.setPreferredSize(new Dimension(480, 400)); // Adjust based on your chatPanel size

chatScrollPane.setBounds(0, 0, 840, 620); // Adjust according to your window size
unreadMessagesBadge.setBounds(180, 450, 120, 30); // Position the badge towards the bottom center

layeredPane.add(chatScrollPane, JLayeredPane.DEFAULT_LAYER);
layeredPane.add(unreadMessagesBadge, JLayeredPane.POPUP_LAYER);

chatPanel.add(layeredPane, BorderLayout.CENTER);

    }

     private void openGroupCreationDialog() {
    List<GroupCreationDialog.Friend> friends = fetchFriendListForGroupDialog();
JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
GroupCreationDialog dialog = new GroupCreationDialog(parentFrame, friends, loggedInUserId);


    dialog.setVisible(true);

    // ✅ After dialog closes, refresh group list
    fetchGroups();
}


     private void startPollingForGroups() {
    Timer groupPollingTimer = new Timer(5000, e -> fetchGroups());
    groupPollingTimer.start();
}


     private List<GroupCreationDialog.Friend> fetchFriendListForGroupDialog() {
    List<GroupCreationDialog.Friend> friends = new ArrayList<>();
    String sql = """
        SELECT u.user_id, u.username
        FROM Friends f
        JOIN Users u ON (f.friend_id = u.user_id AND f.user_id = ?) OR (f.user_id = u.user_id AND f.friend_id = ?)
        WHERE f.status = 'Accepted'
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, loggedInUserId);
        pstmt.setInt(2, loggedInUserId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            friends.add(new GroupCreationDialog.Friend(
                rs.getInt("user_id"),
                rs.getString("username")
            ));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return friends;
}
     
private void syncMessages() {
    try {
        URL url = new URL("http://localhost:5000/syncMessages?lastMessageId=" + lastLoadedMessageId + "&groupId=" + currentGroupId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String rawResponse = response.toString().trim();
                if (rawResponse.startsWith("{")) {
                    JSONObject singleMessage = new JSONObject(rawResponse);
                    JSONArray messages = new JSONArray();
                    messages.put(singleMessage);
                    processMessages(messages);
                } else if (rawResponse.startsWith("[")) {
                    JSONArray messages = new JSONArray(rawResponse);
                    processMessages(messages);
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}


private void processMessages(JSONArray messages) {
    for (int i = 0; i < messages.length(); i++) {
        JSONObject msg = messages.getJSONObject(i);
        int messageId = msg.getInt("messageId");

        // ✅ Skip if already loaded
        if (messageId <= lastLoadedMessageId) {
            // ✅ Still check for status update
            String newReadStatus = msg.getString("readStatus");
            String newDeliveryStatus = msg.getString("deliveryStatus");
            updateMessageStatus(messageId, newReadStatus, newDeliveryStatus);
            continue;
        }

        String messageType = msg.getString("messageType");
        String senderId = msg.getString("senderId");
        String senderName = msg.getString("senderName"); // ✅ For group chat display
        String messageText = msg.getString("message");
        String sentAt = msg.getString("sentAt");
        String readStatus = msg.getString("readStatus");
        String deliveryStatus = msg.getString("deliveryStatus");
        String attachmentName = msg.optString("attachmentName", null);
        String attachmentData = msg.optString("attachment", null);
        int replyTo = msg.optInt("replyTo", -1);
        boolean isSender = senderId.equals(String.valueOf(loggedInUserId));

        byte[] fileData = null;
        if (attachmentData != null && !attachmentData.isEmpty()) {
            fileData = Base64.getDecoder().decode(attachmentData);
        }

        // ✅ Display sender name in group chat
        String displayText = isSender ? messageText : senderName + ": " + messageText;

        if ("text".equals(messageType)) {
            addMessageToChat(
                messageId,
                displayText,
                isSender,
                sentAt,
                messageType,
                readStatus,
                deliveryStatus,
                replyTo
            );
        } else if (fileData != null) {
            if ("audio".equals(messageType)) {
                addAudioToChat(
                    messageId,
                    attachmentName,
                    fileData,
                    isSender,
                    sentAt,
                    readStatus,
                    deliveryStatus,
                    displayText
                );
            } else {
                addAttachmentToChat(
                    messageId,
                    attachmentName,
                    messageType,
                    fileData,
                    isSender,
                    sentAt,
                    readStatus,
                    deliveryStatus,
                    displayText
                );
            }
        }

        // ✅ Update last loaded message ID
        lastLoadedMessageId = Math.max(lastLoadedMessageId, messageId);
    }
}

private void updateMessageStatus(int messageId, String readStatus, String deliveryStatus) {
    for (Component comp : chatMessagesPanel.getComponents()) {
        if (comp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) alignmentPanel.getComponent(0);
            Integer panelMessageId = (Integer) messagePanel.getClientProperty("messageId");

            if (panelMessageId != null && panelMessageId == messageId) {
                for (Component innerComp : messagePanel.getComponents()) {
                    if (innerComp instanceof JPanel infoPanel) {
                        for (Component infoComp : ((JPanel) innerComp).getComponents()) {
                            if (infoComp instanceof JLabel statusLabel) {
                                if ("Read".equals(readStatus)) {
                                    statusLabel.setText("✔✔");
                                    statusLabel.setForeground(Color.BLUE);
                                } else {
                                    statusLabel.setText("✔");
                                    statusLabel.setForeground(Color.GRAY);
                                }
                            }
                        }
                    }
                }

                // ✅ Update SQLite for consistency
                try (Connection conn = DatabaseHelper.connect()) {
                    PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE Messages SET read_status = ?, delivery_status = ? WHERE message_id = ?"
                    );
                    pstmt.setString(1, readStatus);
                    pstmt.setString(2, deliveryStatus);
                    pstmt.setInt(3, messageId);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                chatMessagesPanel.revalidate();
                chatMessagesPanel.repaint();
                break;
            }
        }
    }
}

private void syncGroupMessages() {
    System.out.println("Starting group sync...");
    try {
        URL url = new URL("http://localhost:5000/syncGroupMessages?lastMessageId=" + lastGroupMessageId + "&groupId=" + currentGroupId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String rawResponse = response.toString().trim();
                if (rawResponse.startsWith("[")) {
                    JSONArray messages = new JSONArray(rawResponse);
                    processGroupMessages(messages);
                }
                System.out.println("Raw Group Response: " + response.toString());
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

// ✅ Separate reaction and status update
private void processGroupMessages(JSONArray messages) {
    for (int i = 0; i < messages.length(); i++) {
        JSONObject msg = messages.getJSONObject(i);
        int messageId = msg.getInt("messageId");

        if (messageId <= lastGroupMessageId) {
            continue;
        }

        String messageType = msg.getString("messageType");
        int senderId = msg.getInt("senderId");
        String senderName = msg.getString("senderName");
        String messageText = msg.getString("message");
        String sentAt = msg.getString("sentAt");
        String readStatus = msg.getString("readStatus");
        String deliveryStatus = msg.getString("deliveryStatus");
        String attachmentName = msg.optString("attachmentName", null);
        String attachmentData = msg.optString("attachment", null);
        int replyTo = msg.optInt("replyTo", -1);

        boolean isSender = senderId == loggedInUserId;

        // ✅ Decode attachment
        byte[] fileData = null;
        if (attachmentData != null && !attachmentData.isEmpty()) {
            fileData = Base64.getDecoder().decode(attachmentData);
        }

        // ✅ If message already displayed → only update status
        if (isMessageDisplayed(messageId)) {
            if (isSender) {
                updateMessageStatus(messageId, readStatus, deliveryStatus);
            }
            continue;
        }

        // ✅ Handle text message
        if ("text".equals(messageType)) {
            addMessageToChat(
                messageId,
                isSender ? messageText : senderName + ": " + messageText,
                isSender,
                sentAt,
                messageType,
                isSender ? readStatus : null,
                isSender ? deliveryStatus : null,
                replyTo > 0 ? replyTo : -1
            );
        }

        // ✅ Handle audio files separately (show play button)
        else if ("audio".equals(messageType) && fileData != null) {
            addAudioToChat(
                messageId,
                attachmentName,
                fileData,
                isSender,
                sentAt,
                isSender ? readStatus : null,
                isSender ? deliveryStatus : null,
                messageText
            );
        } 
        
        // ✅ Handle other file attachments
        else if (fileData != null) {
            addAttachmentToChat(
                messageId,
                attachmentName,
                messageType,
                fileData,
                isSender,
                sentAt,
                isSender ? readStatus : null,
                isSender ? deliveryStatus : null,
                messageText
            );
        }

        if (!isSender) {
            updateMessageReaction(messageId, readStatus, deliveryStatus);
        }

        lastGroupMessageId = Math.max(lastGroupMessageId, messageId);
    }
}



private void updateMessageReaction(int messageId, String readStatus, String deliveryStatus) {
    for (Component comp : chatMessagesPanel.getComponents()) {
        if (comp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) alignmentPanel.getComponent(0);
            Integer panelMessageId = (Integer) messagePanel.getClientProperty("messageId");

            if (panelMessageId != null && panelMessageId == messageId) {
                for (Component c : messagePanel.getComponents()) {
                    if (c instanceof JPanel infoPanel) {
                        for (Component infoComponent : ((JPanel) c).getComponents()) {
                            if (infoComponent instanceof JLabel statusLabel) {
                                if ("Read".equals(readStatus)) {
                                    statusLabel.setText("✔✔");
                                    statusLabel.setForeground(Color.BLUE);
                                } else {
                                    statusLabel.setText("✔");
                                    statusLabel.setForeground(Color.GRAY);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}


     private Timer syncTimer;
     private ScheduledExecutorService syncScheduler;

private void startSyncing() {
    stopSyncing(); // Ensure no duplicate sync runs

    syncScheduler = Executors.newSingleThreadScheduledExecutor();
    syncScheduler.scheduleAtFixedRate(() -> {
        if (currentGroupId != -1) {
            syncGroupMessages();
        } else if (currentChatFriendId != -1) {
            syncMessages();
        }
    }, 0, 2, TimeUnit.SECONDS);
}


private void stopSyncing() {
    if (syncScheduler != null && !syncScheduler.isShutdown()) {
        syncScheduler.shutdown();
    }
}

    public void openChat(int friendId) {
    this.currentChatFriendId = friendId;
    lastLoadedMessageId = -1;
    loadMessages(friendId);
    startSyncing(); // ✅ Start real-time syncing
}
 
     

private void fetchGroups() {
    groupsSectionPanel.removeAll(); // Clear ONLY Groups Section

    // ✅ Create a "Groups" Header Panel with Proper Height
    JPanel groupsHeaderPanel = new JPanel(new BorderLayout());
    groupsHeaderPanel.setBackground(new Color(30, 144, 255)); // Blue background
    groupsHeaderPanel.setPreferredSize(new Dimension(270, 35)); // Reduce height
    groupsHeaderPanel.setMaximumSize(new Dimension(320, 35));
    groupsHeaderPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

    // ✅ Groups Title
    JLabel groupsTitleLabel = new JLabel("Groups");
    groupsTitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
    groupsTitleLabel.setForeground(Color.WHITE);

    // ✅ Create "Create Group" Button
    JButton createGroupButton = new JButton();
    createGroupButton.setPreferredSize(new Dimension(28, 28)); 
    createGroupButton.setBorderPainted(false);
    createGroupButton.setContentAreaFilled(false);
    createGroupButton.setFocusPainted(false);
    createGroupButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

    try {
        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/add_group.png")); 
        Image img = icon.getImage().getScaledInstance(17, 17, Image.SCALE_SMOOTH);
        createGroupButton.setIcon(new ImageIcon(img));
    } catch (Exception e) {
        createGroupButton.setText("+"); 
        System.err.println("Icon not found: " + e.getMessage());
    }
    createGroupButton.addActionListener(e -> openGroupCreationDialog());

    // ✅ Create "Join Group" Button
    JButton joinGroupButton = new JButton();
    joinGroupButton.setPreferredSize(new Dimension(28, 28));
    joinGroupButton.setBorderPainted(false);
    joinGroupButton.setContentAreaFilled(false);
    joinGroupButton.setFocusPainted(false);
    joinGroupButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

    try {
        ImageIcon joinIcon = new ImageIcon(getClass().getResource("/icons/join_group.png")); // Use your actual path
        Image joinImg = joinIcon.getImage().getScaledInstance(17, 17, Image.SCALE_SMOOTH);
        joinGroupButton.setIcon(new ImageIcon(joinImg));
    } catch (Exception e) {
        joinGroupButton.setText("🔗"); // Fallback if icon is missing
        System.err.println("Join Icon not found: " + e.getMessage());
    }
    joinGroupButton.addActionListener(e -> promptForInviteCode()); // ✅ Opens input dialog for invite code

    // ✅ Panel to hold both buttons side by side
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttonsPanel.setOpaque(false);
    buttonsPanel.add(joinGroupButton);
    buttonsPanel.add(createGroupButton);

    // ✅ Add to the Header Panel (Title Left, Buttons Right)
    groupsHeaderPanel.add(groupsTitleLabel, BorderLayout.WEST);
    groupsHeaderPanel.add(buttonsPanel, BorderLayout.EAST);

    // ✅ Add Header to the Groups Section
    groupsSectionPanel.add(groupsHeaderPanel);

    // ✅ Fetch Groups from Database
    String sql = """
        SELECT g.group_id, g.group_name, g.icon 
        FROM Groups g
        JOIN GroupMembers gm ON g.group_id = gm.group_id
        WHERE gm.user_id = ?
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, loggedInUserId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            int groupId = rs.getInt("group_id");
            String groupName = rs.getString("group_name");
            byte[] groupIcon = rs.getBytes("icon");

            addGroup(groupId, groupName, groupIcon);
        }

        groupsSectionPanel.revalidate();
        groupsSectionPanel.repaint();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private void promptForInviteCode() {
    String input = JOptionPane.showInputDialog(this, "Enter Invite Code or Link:", "Join Group", JOptionPane.PLAIN_MESSAGE);

    if (input != null && !input.trim().isEmpty()) {
        String inviteCode = extractInviteCode(input.trim());
        joinGroupWithInvite(inviteCode);
    } else {
        JOptionPane.showMessageDialog(this, "Invite code cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}

// ✅ Extracts only the invite code from a full link if needed
private String extractInviteCode(String input) {
    if (input.startsWith("https://yourapp.com/join/")) {
        return input.substring("https://yourapp.com/join/".length()); // ✅ Extracts only the code
    }
    return input; // If it's already a code, return as is
}


private void joinGroupWithInvite(String inviteCode) {
    String sql = "SELECT group_id FROM GroupInvites WHERE invite_code = ? AND expires_at > CURRENT_TIMESTAMP AND is_used = FALSE";

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, inviteCode);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            int groupId = rs.getInt("group_id");
            addUserToGroup(groupId);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid or expired invite code.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to join group.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}


private void addUserToGroup(int groupId) {
    String sql = "INSERT INTO GroupMembers (group_id, user_id) VALUES (?, ?)";

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, groupId);
        pstmt.setInt(2, loggedInUserId);
        pstmt.executeUpdate();

        // ✅ Mark invite as used (Optional: If invite is one-time use)
        String updateSql = "UPDATE GroupInvites SET is_used = TRUE WHERE group_id = ? AND is_used = FALSE";
        try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
            updatePstmt.setInt(1, groupId);
            updatePstmt.executeUpdate();
        }

        JOptionPane.showMessageDialog(this, "Successfully joined the group!");
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to join group.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}





private void addGroup(int groupId, String groupName, byte[] groupIconBytes) {
    System.out.println("Adding Group: " + groupName); // Debugging

    JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
    groupPanel.setPreferredSize(new Dimension(270, 60));
    groupPanel.setMaximumSize(new Dimension(320, 60));
    groupPanel.setBackground(Color.WHITE);
    groupPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

    JLabel groupIconLabel;
    if (groupIconBytes != null) {
        ImageIcon icon = new ImageIcon(groupIconBytes);
        Image image = icon.getImage().getScaledInstance(45, 45, Image.SCALE_SMOOTH);
        groupIconLabel = new JLabel(new ImageIcon(image));
    } else {
        groupIconLabel = new JLabel("🧑‍🤝‍🧑"); // Default icon if no image
        groupIconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
    }
    groupIconLabel.setPreferredSize(new Dimension(45, 45));

    JLabel groupNameLabel = new JLabel(groupName);
    groupNameLabel.setFont(new Font("Arial", Font.BOLD, 14));

    groupPanel.add(groupIconLabel);
    groupPanel.add(groupNameLabel);

    groupPanel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            openGroupChat(groupId, groupName, groupIconBytes);
        }
    });

    groupsSectionPanel.add(groupPanel);
    friendsListPanel.revalidate();
    friendsListPanel.repaint();
}



private Timer groupMessagePollingTimer;
public GroupDetailsWindow groupDetailsWindow = null;

private void openGroupChat(int groupId, String groupName, byte[] groupIconBytes) {
    stopSyncing(); // Stop existing sync
    currentChatFriendId = -1;
    currentGroupId = groupId;

    chatHeader.setText(groupName);
    
        

    lastGroupMessageId = -1;

    loadGroupMessages(groupId);
    startSyncing(); // ✅ Start group sync

    if (groupIconBytes != null) {
        ImageIcon icon = new ImageIcon(groupIconBytes);
        Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        chatProfilePic.setIcon(new ImageIcon(image));
    } else {
        chatProfilePic.setText("🧑‍🤝‍🧑");
    }

    // ✅ Remove all previous listeners before adding a new one
    for (MouseListener listener : chatHeader.getMouseListeners()) {
        chatHeader.removeMouseListener(listener);
    }

    // ✅ Click Header to Open Group Details
    chatHeader.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (groupDetailsWindow != null) {
                groupDetailsWindow.dispose(); // Close previous window
            }

            // ✅ Open new GroupDetailsWindow
            groupDetailsWindow = new GroupDetailsWindow(ChatWindow.this, groupId, groupName, groupIconBytes, loggedInUserId);

            // ✅ Ensure the window reference is cleared when closed
            groupDetailsWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    groupDetailsWindow = null;
                }
            });
        }
    });

    loadGroupMessages(groupId);
    markGroupMessagesAsRead(groupId);
}



public void clearChatMessages() {
    chatMessagesPanel.removeAll();
    chatMessagesPanel.revalidate();
    chatMessagesPanel.repaint();
}

public void sendGroupMessage(int groupId, String message) {
    System.out.println("📩 Attempting to send message to group " + groupId + ": " + message);

    // ✅ FIX: Use `message_text` instead of `message`
    String sql = "INSERT INTO GroupMessages (group_id, sender_id, message_text, sent_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, groupId);
        pstmt.setInt(2, loggedInUserId);
        pstmt.setString(3, message);
        int rowsAffected = pstmt.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("✅ Message successfully inserted into database.");
        } else {
            System.out.println("❌ ERROR: Message was NOT inserted into database.");
        }

        // ✅ Refresh chat UI to show the message
        loadGroupMessages(groupId);
    } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("❌ SQL ERROR: " + e.getMessage());
    }
}




public void loadGroupMessages(int groupId) {
    String sql = """
    SELECT gm.message_id, gm.sender_id, gm.message_text, gm.message_type, 
           gm.sent_at, gm.read_status, gm.delivery_status, gm.reply_to, 
           u.username, gm.attachment, gm.attachment_name
    FROM GroupMessages gm
    JOIN Users u ON gm.sender_id = u.user_id
    WHERE gm.group_id = ?
    ORDER BY gm.sent_at ASC
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, groupId);
        ResultSet rs = pstmt.executeQuery();

        chatMessagesPanel.removeAll();

        while (rs.next()) {
            int messageId = rs.getInt("message_id");
            int senderId = rs.getInt("sender_id");
            String messageText = rs.getString("message_text");
            String messageType = rs.getString("message_type");
            String sentAt = rs.getString("sent_at");
            String senderName = rs.getString("username");
            String readStatus = rs.getString("read_status");
            String deliveryStatus = rs.getString("delivery_status");
            int replyToMessageId = rs.getInt("reply_to");
            if (rs.wasNull()) replyToMessageId = -1;

            String attachmentName = rs.getString("attachment_name");
            byte[] fileData = rs.getBytes("attachment");

            boolean isSentByMe = (senderId == loggedInUserId);

            // ✅ Handle text message
            if ("text".equals(messageType)) {
                addMessageToChat(
                    messageId,
                    isSentByMe ? messageText : senderName + ": " + messageText,
                    isSentByMe,
                    sentAt,
                    messageType,
                    isSentByMe ? readStatus : null,
                    isSentByMe ? deliveryStatus : null,
                    replyToMessageId
                );
            } 
            
            // ✅ Handle audio files separately (to add play button)
            else if ("audio".equals(messageType) && fileData != null) {
                addAudioToChat(
                    messageId,
                    attachmentName,
                    fileData,
                    isSentByMe,
                    sentAt,
                    isSentByMe ? readStatus : null,
                    isSentByMe ? deliveryStatus : null,
                    messageText
                );
            } 
            
            // ✅ Handle other attachments (images, videos, PDFs, etc.)
            else if (fileData != null) {
                addAttachmentToChat(
                    messageId,
                    attachmentName,
                    messageType,
                    fileData,
                    isSentByMe,
                    sentAt,
                    isSentByMe ? readStatus : null,
                    isSentByMe ? deliveryStatus : null,
                    messageText
                );
            }

            lastGroupMessageId = Math.max(lastGroupMessageId, messageId);
        }

        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();

    } catch (SQLException e) {
        e.printStackTrace();
    }
}



private String getGroupMessageStatus(int groupId, String readBy) {
    if (readBy == null || readBy.isEmpty()) {
        return "";
    }

    // Get number of group members
    int totalMembers = getGroupMemberCount(groupId);

    // Count number of IDs in `read_by` field
    String[] readByIds = readBy.split(",");
    int readCount = (int) Arrays.stream(readByIds)
        .filter(s -> !s.isEmpty())
        .distinct()
        .count();

    if (readCount == totalMembers) {
        return "Read by All";
    } else if (readCount > 0) {
        return "Delivered";
    } else {
        return "Sent";
    }
}

private int getGroupMemberCount(int groupId) {
    String sql = "SELECT COUNT(*) FROM GroupMembers WHERE group_id = ?";
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, groupId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
}

    
private void showContextMenu(MouseEvent e, JPanel messagePanel) {
    int messageId = (int) messagePanel.getClientProperty("messageId");
    String messageText = (String) messagePanel.getClientProperty("messageText");
    Boolean isSenderProp = (Boolean) messagePanel.getClientProperty("isSender");
boolean isSender = isSenderProp != null && isSenderProp;


    JPopupMenu contextMenu = new JPopupMenu();

    JMenuItem deleteItem = new JMenuItem("Delete");
    JMenuItem copyItem = new JMenuItem("Copy");
    JMenuItem selectItem = new JMenuItem("Select");
    JMenuItem pinItem = new JMenuItem("Pin");
    JMenuItem unpinItem = new JMenuItem("Unpin");
    JMenuItem replyItem = new JMenuItem("Reply");
    JMenuItem reactItem = new JMenuItem("React");




    deleteItem.addActionListener(evt -> deleteMessage(messageId));
    copyItem.addActionListener(evt -> copyMessageToClipboard(messageText));
    selectItem.addActionListener(evt -> selectMessage(messageId));
    pinItem.addActionListener(evt -> pinMessage(messageId));
    unpinItem.addActionListener(evt -> unpinMessage(messageId));
    replyItem.addActionListener(evt -> replyToMessage(messageId, messageText));
    reactItem.addActionListener(evt -> showReactionPopup(messageId));

    contextMenu.add(deleteItem);
    contextMenu.add(copyItem);
    contextMenu.add(selectItem);
    contextMenu.add(pinItem);
    contextMenu.add(unpinItem);
    contextMenu.add(reactItem);
    contextMenu.add(replyItem);

    // ✅ Only Add "Edit" for Sent Messages
    if (isSender) {
        JMenuItem editItem = new JMenuItem("Edit");
        editItem.addActionListener(evt -> editMessage(messageId, messageText));
        contextMenu.add(editItem);
        loadMessages(currentChatFriendId); // Refresh private chat
    }

    contextMenu.show(messagePanel, e.getX(), e.getY());
}


private void showReactionPopup(int messageId) {
    JPopupMenu reactionMenu = new JPopupMenu();

    String[] reactions = {"👍 Like", "❤️ Love", "😂 Haha", "😢 Sad", "😡 Angry"};
    String[] reactionValues = {"Like", "Love", "Haha", "Sad", "Angry"};

    for (int i = 0; i < reactions.length; i++) {
        String displayReaction = reactions[i];
        String dbReaction = reactionValues[i];

        JMenuItem reactionItem = new JMenuItem(displayReaction);
        reactionItem.addActionListener(e -> addOrUpdateReaction(messageId, dbReaction));
        reactionMenu.add(reactionItem);
    }

    reactionMenu.show(this, 250, 300); // Adjust position as you like
}

private void addOrUpdateReaction(int messageId, String reaction) {
    String checkQuery = "SELECT reaction_id FROM MessageReactions WHERE message_id = ? AND user_id = ?";
    String insertQuery = "INSERT INTO MessageReactions (message_id, user_id, reaction) VALUES (?, ?, ?)";
    String updateQuery = "UPDATE MessageReactions SET reaction = ?, reacted_at = CURRENT_TIMESTAMP WHERE message_id = ? AND user_id = ?";

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
         PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
         PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

        checkStmt.setInt(1, messageId);
        checkStmt.setInt(2, loggedInUserId);

        ResultSet rs = checkStmt.executeQuery();
        boolean reactionExists = rs.next(); // Check if a reaction exists

        if (reactionExists) {
            // ✅ Reaction already exists → Update it
            updateStmt.setString(1, reaction);
            updateStmt.setInt(2, messageId);
            updateStmt.setInt(3, loggedInUserId);
            updateStmt.executeUpdate();
            System.out.println("🔄 Reaction updated for Message ID: " + messageId);
        } else {
            // ✅ No reaction → Insert new reaction
            insertStmt.setInt(1, messageId);
            insertStmt.setInt(2, loggedInUserId);
            insertStmt.setString(3, reaction);
            insertStmt.executeUpdate();
            System.out.println("➕ Reaction added for Message ID: " + messageId);
        }

        // ✅ Ensure the UI updates properly
        SwingUtilities.invokeLater(() -> loadReactionsForMessage(messageId));

    } catch (SQLException e) {
        System.err.println("❌ Error updating reaction for Message ID: " + messageId);
        e.printStackTrace();
    }
}


private void loadReactionsForMessage(int messageId) {
    String query = "SELECT reaction FROM MessageReactions WHERE message_id = ?";
    StringBuilder reactionsText = new StringBuilder();

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement stmt = conn.prepareStatement(query)) {

        stmt.setInt(1, messageId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String reaction = rs.getString("reaction");
            String emoji = switch (reaction) {
                case "Like" -> "👍";
                case "Love" -> "❤️";
                case "Haha" -> "😂";
                case "Sad" -> "😢";
                case "Angry" -> "😡";
                default -> "";
            };
            if (!emoji.isEmpty()) {
                reactionsText.append(emoji).append(" ");
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    String finalReactions = reactionsText.toString().trim();

    boolean foundPanel = false; // ✅ Debugging flag

    for (Component comp : chatMessagesPanel.getComponents()) {
        if (comp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) alignmentPanel.getComponent(0);
            Integer panelMessageId = (Integer) messagePanel.getClientProperty("messageId");

            if (panelMessageId != null && panelMessageId == messageId) {
                JPanel reactionPanel = (JPanel) messagePanel.getClientProperty("reactionPanel");

                if (reactionPanel == null) {
                    // ✅ Ensure reactionPanel exists
                    reactionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    reactionPanel.setOpaque(false);
                    reactionPanel.setVisible(false);
                    messagePanel.putClientProperty("reactionPanel", reactionPanel);
                    messagePanel.add(reactionPanel);
                }

                reactionPanel.removeAll(); // Clear previous reactions

                if (!finalReactions.isEmpty()) {
                    JLabel reactionLabel = new JLabel(finalReactions);
                    reactionLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                    reactionPanel.add(reactionLabel);
                    reactionPanel.setVisible(true);
                } else {
                    reactionPanel.setVisible(false);
                }

                reactionPanel.revalidate();
                reactionPanel.repaint();

                foundPanel = true;
                break;
            }
        }
    }

    if (!foundPanel) {
        System.out.println("❌ Reaction Panel Not Found for Message ID: " + messageId);
    } else {
        System.out.println("✅ Reaction Updated for Message ID: " + messageId);
    }
}






  private void deleteMessage(int messageId) {
    Component[] components = chatMessagesPanel.getComponents();
    for (Component comp : components) {
        if (comp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) alignmentPanel.getComponent(0);
            int panelMessageId = (int) messagePanel.getClientProperty("messageId");
            if (panelMessageId == messageId) {
                chatMessagesPanel.remove(alignmentPanel);
                chatMessagesPanel.revalidate();
                chatMessagesPanel.repaint();
                deleteMessageFromDatabase(messageId);
                scrollToBottom();
                break;
            }
        }
    }
}


private void deleteMessageFromDatabase(int messageId) {
    // Your database deletion logic here
    try (Connection conn = DriverManager.getConnection(URL);
         PreparedStatement stmt = conn.prepareStatement("DELETE FROM Messages WHERE message_id = ?")) {
        stmt.setInt(1, messageId);
        stmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}



    private void copyMessageToClipboard(String messageText) {
    StringSelection stringSelection = new StringSelection(messageText);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
}
    
    private Set<Integer> selectedMessages = new HashSet<>();


private void selectMessage(int messageId) {
    if (selectedMessages.contains(messageId)) {
        selectedMessages.remove(messageId);
        highlightMessage(messageId, false);
    } else {
        selectedMessages.add(messageId);
        highlightMessage(messageId, true);
    }
}





 private void highlightMessage(int messageId, boolean highlight) {
    for (Component alignmentComp : chatMessagesPanel.getComponents()) {
        if (alignmentComp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) ((JPanel) alignmentPanel).getComponent(0);
            int panelMessageId = (int) messagePanel.getClientProperty("messageId");

            if (panelMessageId == messageId) {
                if (highlight) {
                    messagePanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                } else {
                    messagePanel.setBorder(null);
                }
                messagePanel.repaint();
                break;
            }
        }
    }
}


    
private void pinMessage(int messageId) {
    // Check if message is already pinned
    for (Component comp : pinnedMessagesPanel.getComponents()) {
        // ✅ Skip non-panel components (JLabel, JSeparator, etc.)
        if (!(comp instanceof JPanel)) {
            continue;
        }

        JPanel pinnedWrapper = (JPanel) comp;
        if (pinnedWrapper.getComponentCount() == 0) {
            continue;
        }

        Component innerComp = pinnedWrapper.getComponent(0);
        if (innerComp instanceof JPanel pinnedMessagePanel) {
            Integer pinnedMessageId = (Integer) pinnedMessagePanel.getClientProperty("messageId");
            if (pinnedMessageId != null && pinnedMessageId == messageId) {
                JOptionPane.showMessageDialog(this, "Message is already pinned!");
                return;
            }
        }
    }

    // Find the original message in chatMessagesPanel
    JPanel messagePanelToPin = null;
    for (Component comp : chatMessagesPanel.getComponents()) {
        if (comp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) alignmentPanel.getComponent(0);
            int panelMessageId = (int) messagePanel.getClientProperty("messageId");

            if (panelMessageId == messageId) {
                messagePanelToPin = messagePanel;
                break;
            }
        }
    }

    if (messagePanelToPin != null) {
        JPanel clonedPanel = createPinnedClone(messageId, getMessageTextFromPanel(messagePanelToPin));
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Helps with alignment
        wrapper.add(clonedPanel);

        pinnedMessagesPanel.add(wrapper);
        pinnedMessagesPanel.revalidate();
        pinnedMessagesPanel.repaint();

        // Update database to reflect pinned status
        updateMessagePinnedStatusInDatabase(messageId, true);
    }
}


private String getMessageTextFromPanel(JPanel messagePanel) {
    for (Component comp : messagePanel.getComponents()) {
        if (comp instanceof JPanel contentPanel) {
            for (Component innerComp : contentPanel.getComponents()) {
                if (innerComp instanceof JLabel messageLabel) {
                    return messageLabel.getText().replaceAll("<[^>]*>", "").trim(); // Remove HTML tags and extra spaces
                }
            }
        }
    }
    return "Message content unavailable";
}




private void unpinMessage(int messageId) {
    Component toRemove = null;

    // Search for the pinned message in pinnedMessagesPanel
    for (Component comp : pinnedMessagesPanel.getComponents()) {
        // Skip non-message components (title, separator, etc.)
        if (!(comp instanceof JPanel pinnedWrapper)) {
            continue;
        }

        if (pinnedWrapper.getComponentCount() == 0) {
            continue;
        }

        Component innerComp = pinnedWrapper.getComponent(0);
        if (innerComp instanceof JPanel pinnedMessagePanel) {
            Integer pinnedMessageId = (Integer) pinnedMessagePanel.getClientProperty("messageId");
            if (pinnedMessageId != null && pinnedMessageId == messageId) {
                toRemove = pinnedWrapper;
                break;
            }
        }
    }

    // If the message is found in pinned section, remove it
    if (toRemove != null) {
        pinnedMessagesPanel.remove(toRemove);
        pinnedMessagesPanel.revalidate();
        pinnedMessagesPanel.repaint();
    }

    // Always update database (even if the pinned version is not found)
    updateMessagePinnedStatusInDatabase(messageId, false);
}

private void updateMessagePinnedStatusInDatabase(int messageId, boolean pinned) {
    try (Connection conn = DriverManager.getConnection(URL);
         PreparedStatement stmt = conn.prepareStatement("UPDATE Messages SET pinned = ? WHERE message_id = ?")) {
        stmt.setBoolean(1, pinned);
        stmt.setInt(2, messageId);
        stmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}




private void editMessage(int messageId, String oldMessage) {
    JTextField editField = new JTextField(oldMessage, 20);
    int option = JOptionPane.showConfirmDialog(this, editField, "Edit Message", JOptionPane.OK_CANCEL_OPTION);
    if (option == JOptionPane.OK_OPTION) {
        String newMessage = editField.getText();
        if (currentGroupId != -1) {
            updateGroupMessageTextInDatabase(messageId, newMessage);
            loadGroupMessages(currentGroupId); // Refresh group chat
        } else {
            updateMessageTextInDatabase(messageId, newMessage);
            loadMessages(currentChatFriendId); // Refresh private chat
        }
    }
}

private void updateGroupMessageTextInDatabase(int messageId, String newMessage) {
    String sql = "UPDATE GroupMessages SET message_text = ? WHERE message_id = ?";
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, newMessage);
        pstmt.setInt(2, messageId);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}



private void updateMessageTextInDatabase(int messageId, String newMessage) {
    try (Connection conn = DriverManager.getConnection(URL);
         PreparedStatement stmt = conn.prepareStatement("UPDATE messages SET message_text = ? WHERE message_id = ?")) {
        stmt.setString(1, newMessage);
        stmt.setInt(2, messageId);
        stmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


    private String replyContextMessage = null;
    

private int replyToMessageId = -1;
private String replyToMessageText = null;
private int friendId;

private void replyToMessage(int messageId, String messageText) {
    replyToMessageId = messageId;
    replyToMessageText = messageText;
    replyLabel.setText("Replying to: " + messageText);
    replyLabel.setVisible(true);
    loadMessages(friendId);
    
}

private void insertMessageIntoDatabase(int messageId, String messageText, int replyTo) {
    try (Connection conn = DriverManager.getConnection(URL);
         PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Messages (message_id, message_text, reply_to) VALUES (?, ?, ?)")) {
        stmt.setInt(1, messageId);
        stmt.setString(2, messageText);
        if (replyTo != -1) {
            stmt.setInt(3, replyTo);
        } else {
            stmt.setNull(3, Types.INTEGER);
        }
        stmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private void fetchFriends() {
    friendsSectionPanel.removeAll(); // Clear ONLY Friends Section

    JLabel friendsTitleLabel = new JLabel("Friends");
    friendsTitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
    friendsTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    friendsSectionPanel.add(friendsTitleLabel);

    String sql = """
        SELECT u.user_id, u.username, u.profile_image, u.online_status 
        FROM Friends f
        JOIN Users u ON (f.friend_id = u.user_id AND f.user_id = ?) 
                         OR (f.user_id = u.user_id AND f.friend_id = ?) 
        WHERE f.status = 'Accepted'
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, loggedInUserId);
        pstmt.setInt(2, loggedInUserId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            addFriend(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getBytes("profile_image"),
                rs.getString("online_status")
            );
        }

        friendsSectionPanel.revalidate();
        friendsSectionPanel.repaint();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    
    

 private void addFriend(int friendId, String username, byte[] profileImageBytes, String status) {
    // Main friend panel
    JPanel friendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
    friendPanel.setPreferredSize(new Dimension(270, 60));
    friendPanel.setMaximumSize(new Dimension(320, 60));
    friendPanel.setBackground(Color.WHITE);
    friendPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

    // Profile Picture
    JLabel profileLabel;
    if (profileImageBytes != null) {
        ImageIcon icon = new ImageIcon(profileImageBytes);
        Image image = icon.getImage().getScaledInstance(45, 45, Image.SCALE_SMOOTH);
        profileLabel = new JLabel(new ImageIcon(image));
    } else {
        profileLabel = new JLabel("📷");
        profileLabel.setFont(new Font("Arial", Font.PLAIN, 18));
    }
    profileLabel.setPreferredSize(new Dimension(45, 45));

    // Text Panel (Username + Status)
    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
    textPanel.setBackground(Color.WHITE);

    JLabel nameLabel = new JLabel(username);
    nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

    JLabel statusLabel = new JLabel(status.equals("Online") ? "🟢 Online" : "⚪ Offline");
    statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
    statusLabel.setForeground(status.equals("Online") ? Color.GREEN : Color.GRAY);

    textPanel.add(nameLabel);
    textPanel.add(statusLabel);

    // Add components to friend panel
    friendPanel.add(profileLabel);
    friendPanel.add(textPanel);

    // 🔥 HOVER EFFECT - Change background color on mouse hover
    friendPanel.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            friendPanel.setBackground(new Color(230, 230, 230)); // Light gray on hover
        }

        public void mouseExited(java.awt.event.MouseEvent evt) {
            friendPanel.setBackground(Color.WHITE); // Back to normal
        }

        public void mouseClicked(java.awt.event.MouseEvent evt) {
            openChat(friendId, username, profileImageBytes, status);
        }
    });

    // 🔥 Add "Friends List" title only once
    if (friendsListPanel.getComponentCount() == 0) {
        JLabel title = new JLabel("Friends List");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(Color.DARK_GRAY);
        title.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        friendsListPanel.add(title);
    }

    // Add to friend list panel
    friendsSectionPanel.add(friendPanel);
    friendsListPanel.revalidate();
    friendsListPanel.repaint();
}

    private void openChat(int friendId, String username, byte[] profileImageBytes, String status) {
        currentGroupId = -1; // Clear group chat mode
        
        stopSyncing(); // ✅ Stop existing sync
            this.currentChatFriendId = friendId;
    lastLoadedMessageId = -1;
    loadMessages(friendId);
    startSyncing(); // ✅ Start real-time syncing

          if (groupMessagePollingTimer != null) {
        groupMessagePollingTimer.stop();
    }
        
    currentChatFriendId = friendId;
    notifiedFriends.remove(friendId); // Clear notification when you open the chat

    chatHeader.setText(username);
    
//    chatStatusLabel.setText(status.equals("Online") ? "🟢 Typing..." : "⚪ Offline");
    checkFriendTypingStatus();
    startTypingStatusPolling();
    
    if(status.equals("Online")){
        sentline.setText(""); // Clear the text
    }else {
        String lastSeen = getLastSeenTime(friendId);
System.out.println("Last seen for friendId " + friendId + ": " + lastSeen);  // Debugging line
sentline.setText("⚪ Last seen: " + lastSeen);

    }


    if (profileImageBytes != null) {
        ImageIcon icon = new ImageIcon(profileImageBytes);
        Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        chatProfilePic.setIcon(new ImageIcon(image));
    } else {
        chatProfilePic.setText("📷");
    }

    chatMessagesPanel.removeAll();
    loadMessages(friendId);
    markMessagesAsRead(friendId); // ← Mark messages as read when opening chat
    chatMessagesPanel.revalidate();
    chatMessagesPanel.repaint();
}

private void loadMessages(int friendId) {
    String sql = """
        SELECT m.message_id, m.sender_id, m.message_text, m.sent_at, m.message_type, m.read_status, m.delivery_status, m.pinned, m.reply_to,
               m.attachment_name, m.attachment
        FROM Messages m
        WHERE (m.sender_id = ? AND m.receiver_id = ?) 
           OR (m.sender_id = ? AND m.receiver_id = ?)
        ORDER BY m.sent_at ASC
    """;

    // ✅ Store existing message IDs before clearing the chat panel
    Set<Integer> existingMessageIds = new HashSet<>();
    for (Component comp : chatMessagesPanel.getComponents()) {
        if (comp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) alignmentPanel.getComponent(0);
            Integer panelMessageId = (Integer) messagePanel.getClientProperty("messageId");
            if (panelMessageId != null) {
                existingMessageIds.add(panelMessageId);
            }
        }
    }

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, loggedInUserId);
        pstmt.setInt(2, friendId);
        pstmt.setInt(3, friendId);
        pstmt.setInt(4, loggedInUserId);
        ResultSet rs = pstmt.executeQuery();

        boolean firstLoad = existingMessageIds.isEmpty();

        if (firstLoad) {
            chatMessagesPanel.removeAll();
            pinnedMessagesPanel.removeAll();
        }

        if (firstLoad) {
            JLabel pinnedTitleLabel = new JLabel("📌 Pinned Messages");
            pinnedTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            pinnedTitleLabel.setForeground(Color.DARK_GRAY);
            pinnedTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            pinnedMessagesPanel.add(pinnedTitleLabel);
            pinnedMessagesPanel.add(new JSeparator());
        }

        while (rs.next()) {
            int messageId = rs.getInt("message_id");

            // 🚫 Skip already loaded messages
            if (!firstLoad && existingMessageIds.contains(messageId)) {
                continue;
            }

            String messageType = rs.getString("message_type");
            String readStatus = rs.getString("read_status");
            String deliveryStatus = rs.getString("delivery_status");
            String caption = rs.getString("message_text");
            int replyToMessageId = rs.getInt("reply_to");
            if (rs.wasNull()) {
                replyToMessageId = -1;
            }

            boolean isSender = rs.getInt("sender_id") == loggedInUserId;
            String sentAt = rs.getString("sent_at");
            boolean isPinned = rs.getBoolean("pinned");

            // ✅ Load attachment directly from Messages table
            byte[] fileData = rs.getBytes("attachment");
            String attachmentName = rs.getString("attachment_name");

            // ✅ Handle text and other file types
            if ("text".equals(messageType)) {
                addMessageToChat(
                    messageId,
                    caption,
                    isSender,
                    sentAt,
                    messageType,
                    readStatus,
                    deliveryStatus,
                    replyToMessageId
                );
            } else if (fileData != null) {
                if ("audio".equals(messageType)) {
                    // ✅ Handle audio separately
                    addAudioToChat(
                        messageId,
                        attachmentName,
                        fileData,
                        isSender,
                        sentAt,
                        readStatus,
                        deliveryStatus,
                        caption != null ? caption : ""
                    );
                } else {
                    // ✅ Handle regular file types
                    addAttachmentToChat(
                        messageId,
                        attachmentName,
                        messageType,
                        fileData,
                        isSender,
                        sentAt,
                        readStatus,
                        deliveryStatus,
                        caption != null ? caption : ""
                    );
                }
            }

            // ✅ Add to pinned section if pinned
            if (isPinned) {
                JPanel pinnedClone = createPinnedClone(messageId, caption);
                pinnedMessagesPanel.add(pinnedClone);
            }
        }

        if (firstLoad) {
            chatMessagesPanel.revalidate();
            chatMessagesPanel.repaint();
        }

        pinnedMessagesPanel.revalidate();
        pinnedMessagesPanel.repaint();
    } catch (Exception e) {
        e.printStackTrace();
    }
}


private JPanel createPinnedClone(int messageId, String messageText) {
    JPanel pinnedPanel = new JPanel(new BorderLayout());
    pinnedPanel.setBackground(new Color(250, 250, 210)); // Light yellow for pin
    pinnedPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 0)));

    JLabel pinnedLabel = new JLabel("<html><div style='width: 200px; word-wrap: break-word;'>" + messageText + "</div></html>");
    pinnedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    pinnedLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    pinnedPanel.putClientProperty("messageId", messageId);
    pinnedPanel.add(pinnedLabel, BorderLayout.CENTER);

    pinnedPanel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
    pinnedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

   pinnedPanel.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            JPopupMenu unpinMenu = new JPopupMenu();
            JMenuItem unpinItem = new JMenuItem("Unpin");
            unpinItem.addActionListener(ev -> unpinMessage(messageId));
            unpinMenu.add(unpinItem);
            unpinMenu.show(pinnedPanel, e.getX(), e.getY());
        }
    }
});


    return pinnedPanel;
}


private Clip audioClip;

private void addAudioToChat(int messageId, String fileName, byte[] fileData, boolean isSentByUser, String sentAt, String readStatus, String deliveryStatus, String caption) {
    JPanel audioPanel = new JPanel(new BorderLayout());
    audioPanel.setBackground(isSentByUser ? new Color(173, 216, 230) : new Color(220, 220, 220));
    audioPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    audioPanel.putClientProperty("messageId", messageId);

    // ✅ Audio file label
    JLabel audioLabel = new JLabel(fileName);
    audioLabel.setFont(new Font("Arial", Font.BOLD, 12));
    audioPanel.add(audioLabel, BorderLayout.WEST);

    // ✅ Play button
    JButton playButton = new JButton("▶");
    playButton.setFocusPainted(false);
    playButton.setMargin(new Insets(0, 0, 0, 0));
    playButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));

    // ✅ Handle play/pause state
    playButton.addActionListener(e -> {
        try {
            if (audioClip == null || !audioClip.isOpen()) {
                File tempFile = File.createTempFile("audio", ".wav");
                tempFile.deleteOnExit(); // ✅ Clean up after closing

                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(fileData);
                }

                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(tempFile);
                audioClip = AudioSystem.getClip();
                audioClip.open(audioInputStream);

                audioClip.start();
                playButton.setText("⏸");

                audioClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        playButton.setText("▶");
                        audioClip.close(); // ✅ Clean up clip
                        audioClip = null; // ✅ Reset state
                    }
                });
            } else if (audioClip.isRunning()) {
                audioClip.stop();
                playButton.setText("▶");
            } else {
                audioClip.start(); // ✅ Resume from last position
                playButton.setText("⏸");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(chatPanel, "Failed to play audio: " + ex.getMessage());
        }
    });

    audioPanel.add(playButton, BorderLayout.EAST);

    // ✅ Info Panel (Time + Status)
    JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    infoPanel.setOpaque(false);

    JLabel timeLabel = new JLabel(sentAt);
    timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));

    JLabel statusLabel = new JLabel();
    if (isSentByUser) {
        if ("Read".equals(readStatus)) {
            statusLabel.setText("✔✔");
            statusLabel.setForeground(Color.BLUE);
        } else {
            statusLabel.setText("✔");
            statusLabel.setForeground(Color.GRAY);
        }
        infoPanel.add(statusLabel);
    }
    infoPanel.add(timeLabel);

    audioPanel.add(infoPanel, BorderLayout.SOUTH);

    // ✅ Align left or right based on sender
    JPanel alignmentPanel = new JPanel(new FlowLayout(isSentByUser ? FlowLayout.RIGHT : FlowLayout.LEFT));
    alignmentPanel.setOpaque(false);
    alignmentPanel.add(audioPanel);

    chatMessagesPanel.add(alignmentPanel);
    chatMessagesPanel.revalidate();
    chatMessagesPanel.repaint();
}



private void selectAttachment() {
    JFileChooser fileChooser = new JFileChooser();
    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        selectedAttachment = fileChooser.getSelectedFile();
        showAttachmentDialog(selectedAttachment);
    }
}

private void showAttachmentDialog(File file) {
    Window window = SwingUtilities.getWindowAncestor(this);
JDialog attachmentDialog = new JDialog((window instanceof JFrame) ? (JFrame) window : null, "Attachment Preview", true);

    attachmentDialog.setLayout(new BorderLayout());
    attachmentDialog.setSize(400, 200);
    attachmentDialog.setLocationRelativeTo(this);

    // Preview file icon and name
    JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    Icon fileIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
    JLabel fileLabel = new JLabel(file.getName(), fileIcon, JLabel.LEFT);
    previewPanel.add(fileLabel);

    // Caption input field
    JTextField captionField = new JTextField(25);

    // Send button
    JButton sendButton = new JButton("Send");
    sendButton.addActionListener(e -> {
        attachmentDialog.dispose();
        sendMessageWithAttachment(file, captionField.getText().trim());
    });

    // Bottom panel (Caption + Send)
    JPanel inputPanel = new JPanel(new FlowLayout());
    inputPanel.add(new JLabel("Caption:"));
    inputPanel.add(captionField);
    inputPanel.add(sendButton);

    // Add to dialog
    attachmentDialog.add(previewPanel, BorderLayout.CENTER);
    attachmentDialog.add(inputPanel, BorderLayout.SOUTH);

    attachmentDialog.setVisible(true);
}

private void sendMessageWithAttachment(File attachment, String caption) {
    if (currentChatFriendId == 0 || attachment == null) return;

    String messageType = getFileTypeFromExtension(attachment);
    String readStatus = "Unread";
    String deliveryStatus = "Delivered";
    int messageId = -1;

    try {
        URL url = new URL("http://localhost:5000/sendMessage"); // ✅ Send to backend
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // ✅ Create JSON payload
        JSONObject json = new JSONObject();
        json.put("senderId", loggedInUserId);
        json.put("receiverId", currentChatFriendId);
        json.put("message", caption != null ? caption : "");
        json.put("messageType", messageType);
        json.put("replyTo", replyToMessageId != -1 ? replyToMessageId : JSONObject.NULL);

        // ✅ Encode attachment to Base64
        byte[] fileData = Files.readAllBytes(attachment.toPath());
        String encodedFile = Base64.getEncoder().encodeToString(fileData);
        json.put("attachment", encodedFile);
        json.put("attachmentName", attachment.getName());

        // ✅ Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // ✅ If successful, save to SQLite for offline access
try (Connection conn = DatabaseHelper.connect()) {
    PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO Messages (sender_id, receiver_id, message_text, message_type, read_status, delivery_status, sent_at, reply_to, attachment, attachment_name) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
    );
    pstmt.setInt(1, loggedInUserId);
    pstmt.setInt(2, currentChatFriendId);
    pstmt.setString(3, caption != null ? caption : "");
    pstmt.setString(4, messageType);
    pstmt.setString(5, "Unread");
    pstmt.setString(6, "Delivered");
    
    if (replyToMessageId != -1) {
        pstmt.setInt(7, replyToMessageId);
    } else {
        pstmt.setNull(7, Types.INTEGER);
    }

    // ✅ Save attachment as Base64 string
    if (attachment != null) {
        try (InputStream inputStream = new FileInputStream(attachment)) {
    fileData = inputStream.readAllBytes();
}
        pstmt.setBytes(8, fileData);
        pstmt.setString(9, attachment.getName());
    } else {
        pstmt.setNull(8, Types.BLOB);
        pstmt.setNull(9, Types.VARCHAR);
    }

    pstmt.executeUpdate();

    ResultSet generatedKeys = pstmt.getGeneratedKeys();
    if (generatedKeys.next()) {
        messageId = generatedKeys.getInt(1);
    }
}


            // ✅ Display in chat window
            if (messageId != -1) {
                addAttachmentToChat(
                    messageId,
                    attachment.getName(),
                    messageType,
                    fileData,
                    true,
                    "Now",
                    readStatus,
                    deliveryStatus,
                    caption != null ? caption : ""
                );
            }
        } else {
            JOptionPane.showMessageDialog(chatPanel, "Failed to send attachment. Code: " + responseCode);
        }
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(chatPanel, "Error: " + e.getMessage());
    }

    // ✅ Clear reply context
    replyToMessageId = -1;
    replyLabel.setVisible(false);
}


private void sendMessage(String message, File attachment) {
    if ((currentChatFriendId == -1 && currentGroupId == -1) || (message == null && attachment == null)) {
        return; // ✅ Neither private chat nor group selected
    }

    String messageType;
    if (attachment != null) {
        messageType = getFileTypeFromExtension(attachment);
        if (!messageType.matches("image|video|audio|pdf|document")) {
            messageType = "file";
        }
    } else {
        messageType = "text";
    }

    String readStatus = "Unread";
    String deliveryStatus = "Delivered";
    int messageId = -1;

    try {
        URL url = new URL("http://localhost:5000/sendMessage");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JSONObject json = new JSONObject();
        json.put("senderId", loggedInUserId);

        if (currentGroupId != -1) {
            json.put("groupId", currentGroupId);
        } else {
            json.put("receiverId", currentChatFriendId);
        }

        json.put("message", message != null ? message : "");
        json.put("messageType", messageType);
        json.put("replyTo", replyToMessageId != -1 ? replyToMessageId : JSONObject.NULL);

        // ✅ Handle attachment (Base64 encoding)
        byte[] fileData = null;
        String encodedFile = null;
        if (attachment != null) {
            fileData = Files.readAllBytes(attachment.toPath());
            encodedFile = Base64.getEncoder().encodeToString(fileData);
            json.put("attachment", encodedFile);
            json.put("attachmentName", attachment.getName());
        }

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // ✅ Save to SQLite after successful response
            try (Connection conn = DatabaseHelper.connect()) {
                PreparedStatement pstmt;
                if (currentGroupId != -1) {
                    pstmt = conn.prepareStatement(
                        "INSERT INTO GroupMessages (group_id, sender_id, message_text, message_type, sent_at, reply_to, attachment, attachment_name) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                    );
                    pstmt.setInt(1, currentGroupId);
                    pstmt.setInt(2, loggedInUserId);
                    pstmt.setString(3, message != null ? message : "");
                    pstmt.setString(4, messageType);
                    if (replyToMessageId != -1) {
                        pstmt.setInt(5, replyToMessageId);
                    } else {
                        pstmt.setNull(5, Types.INTEGER);
                    }

                    if (fileData != null) {
                        pstmt.setBytes(6, fileData);
                        pstmt.setString(7, attachment.getName());
                    } else {
                        pstmt.setNull(6, Types.BLOB);
                        pstmt.setNull(7, Types.VARCHAR);
                    }

                    pstmt.executeUpdate();

                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        messageId = generatedKeys.getInt(1);
                    }
                }

                // ✅ Display immediately after sending (text + attachment)
                if (messageId != -1) {
                    if (message != null) {
                        addMessageToChat(
                            messageId,
                            message,
                            true,
                            "Now",
                            messageType,
                            readStatus,
                            deliveryStatus,
                            replyToMessageId
                        );
                    }

                    // ✅ Show attachment immediately after sending
                    if (fileData != null) {
                        if ("audio".equals(messageType)) {
                            addAudioToChat(
                                messageId,
                                attachment.getName(),
                                fileData,
                                true,
                                "Now",
                                readStatus,
                                deliveryStatus,
                                message
                            );
                        } else {
                            addAttachmentToChat(
                                messageId,
                                attachment.getName(),
                                messageType,
                                fileData,
                                true,
                                "Now",
                                readStatus,
                                deliveryStatus,
                                message
                            );
                        }
                    }

                    // ✅ Update lastMessageId immediately to prevent duplication
                    lastGroupMessageId = messageId;
                }
            }
            
                        // ✅ After successful save — reload group messages to show attachment directly
            if (messageId != -1) {
                loadGroupMessages(currentGroupId); // 🔥 Reload messages immediately after sending
            }
        } else {
            JOptionPane.showMessageDialog(chatPanel, "Failed to send message. Code: " + responseCode);
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(chatPanel, "Error: " + e.getMessage());
    }

    // ✅ Clear input & reply context after sending
    messageInputField.setText("");
    selectedAttachment = null;
    replyToMessageId = -1;
    replyLabel.setVisible(false);
}


private void playSound(String soundFileName) {
    try {
        File soundFile = new File("src/main/resources/sounds/" + soundFileName);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        clip.start();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void showPopupNotification(String friendName, String messageSnippet) {
    JDialog popupDialog = new JDialog((Frame) null, "New Message", false);
    popupDialog.setLayout(new BorderLayout());
    popupDialog.setSize(300, 100);
    popupDialog.setUndecorated(true);

    JLabel label = new JLabel("<html><b>New message from " + friendName + ":</b> " + messageSnippet + "</html>");
    label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    popupDialog.add(label, BorderLayout.CENTER);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    popupDialog.setLocation(screenSize.width - popupDialog.getWidth() - 20, screenSize.height - popupDialog.getHeight() - 60);

    popupDialog.setAlwaysOnTop(true); // ✅ Ensure it stays on top
    popupDialog.setVisible(true);

    new Timer(6000, e -> popupDialog.dispose()).start();
}


private boolean attachmentIsVoiceNote(File attachment) {
    if (attachment == null) return false;
    String fileName = attachment.getName().toLowerCase();
    return fileName.endsWith(".wav");
}


private String getFileTypeFromExtension(File file) {
    String fileName = file.getName().toLowerCase();
    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".gif")) {
        return "image";
    } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mkv")) {
        return "video";
    } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".ogg")) {
        return "audio";
    } else if (fileName.endsWith(".pdf")) {
        return "pdf";
    } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".txt") || fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
        return "document";
    } else {
        return "file";
    }
}




private void saveAttachment(int messageId, File file) {
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(
             "INSERT INTO Attachments (message_id, file_name, file_type, file_data, uploaded_at) " +
             "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)");
         FileInputStream fis = new FileInputStream(file)) {
        
        pstmt.setInt(1, messageId);
        pstmt.setString(2, file.getName());
        pstmt.setString(3, "file");
        pstmt.setBinaryStream(4, fis, (int) file.length());
        pstmt.executeUpdate();
        
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private Icon getFileTypeIcon(String fileType) {
    String iconPath;
    switch (fileType.toLowerCase()) {
        case "image":
            iconPath = "icons/image_icon.png";
            break;
        case "video":
            iconPath = "icons/video_icon.png";
            break;
        case "pdf":
            iconPath = "icons/pdf_icon.png";
            break;
        case "document":
        case "doc":
        case "docx":
            iconPath = "icons/doc_icon.png";
            break;
        default:
            iconPath = "icons/file_icon.png";
            break;
    }

    try {
        return new ImageIcon(iconPath);
    } catch (Exception e) {
        e.printStackTrace();
        return null; // In case icon fails to load
    }
}



private void addAttachmentToChat(int messageId, String fileName, String fileType, byte[] fileData, boolean isSent, String time, String readStatus, String deliveryStatus, String caption) {
    if (fileName == null || fileData == null) {
        return; // ✅ Prevent null attachment issues
    }

    JPanel attachmentPanel = new JPanel(new BorderLayout());
    attachmentPanel.setBackground(isSent ? new Color(173, 216, 230) : new Color(220, 220, 220));
    attachmentPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    attachmentPanel.putClientProperty("isSender", isSent);
    attachmentPanel.putClientProperty("messageId", messageId);
    attachmentPanel.putClientProperty("fileData", fileData);
attachmentPanel.putClientProperty("fileName", fileName);

    // ✅ File icon + name
    Icon fileIcon = getFileTypeIcon(fileType);
    JLabel fileLabel = new JLabel(fileName, fileIcon, JLabel.LEFT);
    fileLabel.setFont(new Font("Arial", Font.BOLD, 12));
    
    // ✅ Open file when clicked
    attachmentPanel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                showAttachmentContextMenu(e, attachmentPanel);
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                try {
                    // ✅ Create temp file and open it
                    File tempFile = File.createTempFile("temp", fileName);
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        fos.write(fileData);
                    }
                    Desktop.getDesktop().open(tempFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(chatPanel, "Failed to open attachment: " + ex.getMessage());
                }
            }
        }
    });

    // ✅ Caption (if available)
    JLabel captionLabel = new JLabel(caption != null && !caption.isEmpty() ? caption : "");
    captionLabel.setFont(new Font("Arial", Font.ITALIC, 12));
    captionLabel.setForeground(Color.DARK_GRAY);

    // ✅ Info panel (time + status)
    JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    infoPanel.setOpaque(false);

    JLabel timeLabel = new JLabel(time);
    timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));

    JLabel statusLabel = new JLabel();
    if (isSent) {
        if ("Read".equals(readStatus)) {
            statusLabel.setText("✔✔");
            statusLabel.setForeground(Color.BLUE);
        } else {
            statusLabel.setText("✔");
            statusLabel.setForeground(Color.GRAY);
        }
        infoPanel.add(statusLabel);
    }
    infoPanel.add(timeLabel);

    // ✅ Content panel (file + caption)
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setBackground(attachmentPanel.getBackground());
    contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    contentPanel.add(fileLabel);

    if (caption != null && !caption.isEmpty()) {
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(captionLabel);
    }

    attachmentPanel.add(contentPanel, BorderLayout.CENTER);
    attachmentPanel.add(infoPanel, BorderLayout.SOUTH);

    // ✅ Alignment handling
    JPanel alignmentPanel = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT));
    alignmentPanel.setOpaque(false);
    alignmentPanel.add(attachmentPanel);

    chatMessagesPanel.add(alignmentPanel);
    chatMessagesPanel.revalidate();
    chatMessagesPanel.repaint();
}


private void showAttachmentContextMenu(MouseEvent e, JPanel attachmentPanel) {
    byte[] fileData = (byte[]) attachmentPanel.getClientProperty("fileData");
    String fileName = (String) attachmentPanel.getClientProperty("fileName");

    JPopupMenu menu = new JPopupMenu();
    JMenuItem openItem = new JMenuItem("Open");
    JMenuItem saveItem = new JMenuItem("Save As");

    openItem.addActionListener(ev -> openFile(fileData, fileName));
    saveItem.addActionListener(ev -> saveFile(fileData, fileName));

    menu.add(openItem);
    menu.add(saveItem);
    menu.show(attachmentPanel, e.getX(), e.getY());
}

private void openFile(byte[] fileData, String fileName) {
    if (fileData == null || fileData.length == 0) {
        JOptionPane.showMessageDialog(null, "No file data available.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        File tempFile = File.createTempFile("temp", "_" + fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(fileData);
            fos.flush(); // ✅ Ensure all data is written before opening
        }
        Desktop.getDesktop().open(tempFile);
    } catch (IOException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to open file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

private void saveFile(byte[] fileData, String fileName) {
    if (fileData == null || fileData.length == 0) {
        JOptionPane.showMessageDialog(null, "No file data available.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setSelectedFile(new File(fileName));
    int returnValue = fileChooser.showSaveDialog(null);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
        File saveFile = fileChooser.getSelectedFile();
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            fos.write(fileData);
            fos.flush(); // ✅ Ensure all data is written before closing
            JOptionPane.showMessageDialog(null, "File saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to save file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}



private void addMessageToChat(int messageId, String message, boolean isSent, String time, 
                              String messageType, String readStatus, String deliveryStatus, int replyToMessageId) {
    for (Component comp : chatMessagesPanel.getComponents()) {
        if (comp instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) alignmentPanel.getComponent(0);
            Integer panelMessageId = (Integer) messagePanel.getClientProperty("messageId");
            if (panelMessageId != null && panelMessageId == messageId) {
                return;
            }
        }
    }

    JPanel messagePanel = new JPanel();
    messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
    messagePanel.setBackground(isSent ? new Color(173, 216, 230) : new Color(220, 220, 220));
    messagePanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
    messagePanel.putClientProperty("messageId", messageId);
    messagePanel.putClientProperty("messageText", message);
    messagePanel.putClientProperty("isSender", isSent);

    if (currentGroupId != -1 && !isSent) {
        String senderName = getSenderNameByMessageId(messageId);
        JLabel senderLabel = new JLabel(senderName);
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        senderLabel.setForeground(new Color(0, 102, 204));
        messagePanel.add(senderLabel);
    }

    if (replyToMessageId != -1) {
        String repliedMessageText = getMessageTextById(replyToMessageId);
        JLabel repliedLabel = new JLabel("<html><p style='width:180px; word-wrap:break-word;'><i>Replying to: " + repliedMessageText + "</i></p></html>");
        repliedLabel.setForeground(Color.GRAY);
        messagePanel.add(repliedLabel);
    }

    if ("voice_note".equalsIgnoreCase(messageType)) {
        JPanel voiceNotePanel = new JPanel();
        voiceNotePanel.setOpaque(false);
        voiceNotePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ImageIcon playIcon = new ImageIcon("C:\\Users\\coola\\Documents\\NetBeansProjects\\ChatApplication\\src\\main\\resources\\icons\\play.png");
        JButton playButton = new JButton(playIcon);
        playButton.setBorderPainted(false);
        playButton.setContentAreaFilled(false);
        playButton.setFocusPainted(false);
        playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> playAudio(message));
        JLabel voiceNoteLabel = new JLabel("Voice Note");
        voiceNoteLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        voiceNotePanel.add(playButton);
        voiceNotePanel.add(voiceNoteLabel);
        voiceNotePanel.setToolTipText(message);
        messagePanel.add(voiceNotePanel);
    } else {
        JLabel messageLabel = new JLabel("<html><p style='width:200px; word-wrap:break-word;'>" + message + "</p></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messagePanel.add(messageLabel);
    }

    JPanel reactionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    reactionPanel.setOpaque(false);
    reactionPanel.setVisible(false);
    messagePanel.putClientProperty("reactionPanel", reactionPanel);
    messagePanel.add(reactionPanel);
    SwingUtilities.invokeLater(() -> loadReactionsForMessage(messageId));

    JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    infoPanel.setOpaque(false);
    JLabel timeLabel = new JLabel(time);
    timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
    JLabel statusLabel = new JLabel();
    if (isSent) {
        if (currentGroupId != -1) {
            statusLabel.setText(readStatus);
            statusLabel.setForeground(Color.GRAY);
        } else {
            statusLabel.setText("Read".equalsIgnoreCase(readStatus) ? "✔✔" : "✔");
            statusLabel.setForeground("Read".equalsIgnoreCase(readStatus) ? Color.BLUE : Color.GRAY);
        }
        infoPanel.add(statusLabel);
    }
    infoPanel.add(timeLabel);
    messagePanel.add(infoPanel);
    messagePanel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                showContextMenu(e, messagePanel);
            }
        }
    });
    JPanel alignmentPanel = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT));
    alignmentPanel.setOpaque(false);
    alignmentPanel.add(messagePanel);
    chatMessagesPanel.add(alignmentPanel);
    chatMessagesPanel.revalidate();
    chatMessagesPanel.repaint();
    scrollToBottom();
}




private void playAudio(String filePath) {
    try {
        File audioFile = new File(filePath);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioStream);
        clip.start();
    } catch (Exception e) {
        e.printStackTrace();
    }
}




private String getSenderNameByMessageId(int messageId) {
    String sql = "SELECT u.username FROM GroupMessages gm JOIN Users u ON gm.sender_id = u.user_id WHERE gm.message_id = ?";
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, messageId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getString("username");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return "Unknown";
}




private String getMessageTextById(int messageId) {
    String sql;

    // If in a group chat, search in GroupMessages
    if (currentGroupId != -1) {
        sql = "SELECT message_text FROM GroupMessages WHERE message_id = ?";
    } else {
        sql = "SELECT message_text FROM Messages WHERE message_id = ?";
    }

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, messageId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getString("message_text");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return "(Message not found)";
}



    
    private void markMessagesAsRead(int friendId) {
    String sql = """
        UPDATE Messages 
        SET read_status = 'Read' 
        WHERE sender_id = ? AND receiver_id = ? AND read_status = 'Unread'
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, friendId);
        pstmt.setInt(2, loggedInUserId);
        pstmt.executeUpdate();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    
    private boolean isMessageFromCurrentFriend(int friendId) {
    return friendId == currentChatFriendId;
}

private String getFriendNameById(int friendId) {
    String sql = "SELECT username FROM Users WHERE user_id = ?";
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, friendId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getString("username");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return "Unknown";
}

private String getLastMessageSnippet(int friendId) {
    String sql = """
        SELECT message_text FROM Messages
        WHERE sender_id = ? OR receiver_id = ?
        ORDER BY sent_at DESC LIMIT 1
    """;
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, friendId);
        pstmt.setInt(2, friendId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            String message = rs.getString("message_text");
            return message.length() > 25 ? message.substring(0, 25) + "..." : message;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return "(New message)";
}


private void startPollingForMessages() {
    Timer timer = new Timer(2000, e -> {
        if (currentChatFriendId != -1) { // Only if a chat is open

            int previousMessageCount = chatMessagesPanel.getComponentCount();

            loadMessages(currentChatFriendId);
            markMessagesAsRead(currentChatFriendId);

            int newMessageCount = chatMessagesPanel.getComponentCount();

            if (newMessageCount > previousMessageCount) {
                boolean isAtBottom = chatScrollPane.getVerticalScrollBar().getValue() + chatScrollPane.getVerticalScrollBar().getVisibleAmount() == chatScrollPane.getVerticalScrollBar().getMaximum();

                if (!isAtBottom) {
                    unreadMessageCount += (newMessageCount - previousMessageCount);
                    unreadMessagesBadge.setText(unreadMessageCount + " new messages");
                    unreadMessagesBadge.setVisible(true);
                }

                playSound("message_received.wav");
            }
        }

        // Poll for messages from other friends
        checkForMessagesFromOtherFriends();
    });

    timer.start();
}

private void checkForMessagesFromOtherFriends() {
    String sql = """
        SELECT m.sender_id, m.message_text, u.username
        FROM Messages m
        JOIN Users u ON m.sender_id = u.user_id
        WHERE m.receiver_id = ? AND m.read_status = 'Unread'
        ORDER BY m.sent_at DESC LIMIT 1
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, loggedInUserId);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            int senderId = rs.getInt("sender_id");
            String messageSnippet = rs.getString("message_text");
            String senderName = rs.getString("username");

            // Only notify if we haven't already notified about this friend
            if (senderId != currentChatFriendId && !notifiedFriends.contains(senderId)) {
                showPopupNotification(senderName, messageSnippet);
                playSound("notification_alert.wav");
                notifiedFriends.add(senderId); // Mark this friend as notified
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}



private void startPollingOnlineStatus() {
    Timer onlineStatusTimer = new Timer(5000, new ActionListener() { // Poll every 5 seconds
        @Override
        public void actionPerformed(ActionEvent e) {
            fetchFriends(); // Refresh friends list to check online status
        }
    });
    onlineStatusTimer.start();
}

private void startTypingStatusPolling() {
    Timer timer = new Timer(1000, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            checkFriendTypingStatus();
        }
    });
    timer.start();
}



private void checkFriendTypingStatus() {
    if (currentChatFriendId == 0) return;

    String sql = "SELECT typing_status FROM TypingStatus WHERE user_id = ?";
    
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, currentChatFriendId);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            String status = rs.getString("typing_status");
            chatStatusLabel.setText(status.equals("Typing...") ? "🟢 Typing..." : ""); // Update UI
            chatStatusLabel.setForeground(Color.green);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private String getLastSeenTime(int friendId) {
    String sql = "SELECT last_seen FROM Users WHERE user_id = ?";
    String lastSeen = "Unknown";

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, friendId);
        ResultSet rs = pstmt.executeQuery();

        if (rs != null && rs.next()) {
            Timestamp timestamp = rs.getTimestamp("last_seen");
            if (timestamp != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                lastSeen = formatter.format(timestamp.toLocalDateTime());
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return lastSeen;
}

private void updateLastSeen(int userId) {
    String sql = "UPDATE Users SET last_seen = ? WHERE user_id = ?";

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
        pstmt.setInt(2, userId);
        pstmt.executeUpdate();

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private void setTypingStatus(String status) {
    String sql = "INSERT INTO TypingStatus (user_id, typing_status) VALUES (?, ?) " +
                 "ON CONFLICT(user_id) DO UPDATE SET typing_status = ?";

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, loggedInUserId);
        pstmt.setString(2, status);
        pstmt.setString(3, status);
        pstmt.executeUpdate();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void scrollToBottom() {
    SwingUtilities.invokeLater(() -> {
        JScrollBar verticalBar = chatScrollPane.getVerticalScrollBar();
        int bottomPosition = verticalBar.getMaximum() - verticalBar.getVisibleAmount();
        int currentPosition = verticalBar.getValue();

        // Only scroll if the user is already near the bottom
        if (currentPosition >= bottomPosition - 50) { // Adjust threshold as needed
            verticalBar.setValue(verticalBar.getMaximum());
        }
    });
}

private void pollGroupMessages() {
    if (currentGroupId == -1) {
        return; // No group chat is open
    }

    String sql = """
        SELECT gm.message_id, gm.sender_id, gm.message_text, gm.message_type, gm.sent_at, gm.read_by, gm.reply_to, u.username,
               a.file_name, a.file_type, a.file_data
        FROM GroupMessages gm
        JOIN Users u ON gm.sender_id = u.user_id
        LEFT JOIN Attachments a ON gm.message_id = a.message_id
        WHERE gm.group_id = ?
        ORDER BY gm.sent_at ASC
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, currentGroupId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            int messageId = rs.getInt("message_id");
            int senderId = rs.getInt("sender_id");
            String messageText = rs.getString("message_text");
            String messageType = rs.getString("message_type");
            String sentAt = rs.getString("sent_at");
            String readBy = rs.getString("read_by");
            int replyToMessageId = rs.getInt("reply_to");
            if (rs.wasNull()) replyToMessageId = -1;
            String senderName = rs.getString("username");

            String fileName = rs.getString("file_name");
            String fileType = rs.getString("file_type");
            byte[] fileData = rs.getBytes("file_data");

            boolean isSentByMe = (senderId == loggedInUserId);
            String messageStatus = getGroupMessageStatus(currentGroupId, readBy);

            if (messageId <= lastGroupMessageId) {
                // Update status or reactions if message is already displayed
                updateMessageStatusInUI(messageId, messageStatus);
                loadReactionsForMessage(messageId);
                continue;
            }

            if ("text".equals(messageType)) {
                addMessageToChat(
                    messageId,
                    messageText,
                    isSentByMe,
                    sentAt,
                    messageType,
                    messageStatus,
                    "Delivered",
                    replyToMessageId
                );
            } else {
                addAttachmentToChat(
                    messageId,
                    fileName,
                    fileType,
                    fileData,
                    isSentByMe,
                    sentAt,
                    messageStatus,
                    "Delivered",
                    messageText != null ? messageText : ""
                );
            }

            // Load reactions for each new message
            loadReactionsForMessage(messageId);

            lastGroupMessageId = messageId;
        }

        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


private boolean isMessageDisplayed(int messageId) {
    for (Component component : chatMessagesPanel.getComponents()) {
        if (component instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) ((JPanel) alignmentPanel).getComponent(0);
            Integer panelMessageId = (Integer) messagePanel.getClientProperty("messageId");

            if (panelMessageId != null && panelMessageId == messageId) {
                return true;
            }
        }
    }
    return false;
}


private void updateMessageStatusInUI(int messageId, String newStatus) {
    for (Component component : chatMessagesPanel.getComponents()) {
        if (component instanceof JPanel alignmentPanel) {
            JPanel messagePanel = (JPanel) ((JPanel) alignmentPanel).getComponent(0);
            Integer panelMessageId = (Integer) messagePanel.getClientProperty("messageId");

            if (panelMessageId != null && panelMessageId == messageId) {
                // Find the info panel (time + status) inside messagePanel
                for (Component subComp : messagePanel.getComponents()) {
                    if (subComp instanceof JPanel infoPanel) {
                        for (Component infoLabel : ((JPanel) subComp).getComponents()) {
                            if (infoLabel instanceof JLabel statusLabel) {
                                if (statusLabel.getText().contains("✔") || statusLabel.getText().contains("Read by All")) {
                                    statusLabel.setText(newStatus);
                                    statusLabel.revalidate();
                                    statusLabel.repaint();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}




private void markGroupMessagesAsRead(int groupId) {
    String sqlSelectUnread = """
        SELECT message_id, read_by
        FROM GroupMessages
        WHERE group_id = ? AND NOT read_by LIKE ?
    """;

    String sqlUpdateReadBy = """
        UPDATE GroupMessages
        SET read_by = ?
        WHERE message_id = ?
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement selectStmt = conn.prepareStatement(sqlSelectUnread)) {

        selectStmt.setInt(1, groupId);
        selectStmt.setString(2, "%," + loggedInUserId + ",%");

        ResultSet rs = selectStmt.executeQuery();

        while (rs.next()) {
            int messageId = rs.getInt("message_id");
            String readBy = rs.getString("read_by");

            // Update read_by field (avoid duplicates)
            if (readBy == null || readBy.isEmpty()) {
                readBy = "," + loggedInUserId + ",";
            } else if (!readBy.contains("," + loggedInUserId + ",")) {
                readBy = readBy + loggedInUserId + ",";
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(sqlUpdateReadBy)) {
                updateStmt.setString(1, readBy);
                updateStmt.setInt(2, messageId);
                updateStmt.executeUpdate();
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private void updateTypingStatus(int groupId, boolean isTyping) {
    String sql = """
        INSERT INTO GroupTypingStatus (group_id, user_id, typing, updated_at)
        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(group_id, user_id) 
        DO UPDATE SET typing = ?, updated_at = CURRENT_TIMESTAMP
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, groupId);
        pstmt.setInt(2, loggedInUserId);
        pstmt.setBoolean(3, isTyping);
        pstmt.setBoolean(4, isTyping);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private void pollGroupTypingStatus() {
    if (currentGroupId == -1) {
        return;
    }

    String sql = """
        SELECT u.username
        FROM GroupTypingStatus gts
        JOIN Users u ON gts.user_id = u.user_id
        WHERE gts.group_id = ? AND gts.typing = TRUE AND gts.user_id != ?
    """;

    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, currentGroupId);
        pstmt.setInt(2, loggedInUserId);

        ResultSet rs = pstmt.executeQuery();
        StringBuilder typingUsers = new StringBuilder();

        while (rs.next()) {
            if (typingUsers.length() > 0) {
                typingUsers.append(", ");
            }
            typingUsers.append(rs.getString("username"));
        }

        String groupName = getGroupNameById(currentGroupId);

        if (typingUsers.length() > 0) {
            chatHeader.setText(groupName + " • Typing: " + typingUsers);
        } else {
            chatHeader.setText(groupName);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}


private String getGroupNameById(int groupId) {
    String groupName = "Group"; // Default fallback
    String sql = "SELECT group_name FROM Groups WHERE group_id = ?";
    try (Connection conn = DatabaseHelper.connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, groupId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            groupName = rs.getString("group_name");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return groupName;
}

private void handleMicButtonClick() {
    if (!isRecording) {
        startRecording();
    } else {
        stopRecording();
    }
}

private void startRecording() {
    try {
        AudioFormat format = new AudioFormat(16000, 16, 2, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            JOptionPane.showMessageDialog(null, "Microphone not supported!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        audioLine = (TargetDataLine) AudioSystem.getLine(info);
        audioLine.open(format);
        audioLine.start();

        recordedAudioFile = new File("voice_note_" + System.currentTimeMillis() + ".wav");

        Thread recordingThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(audioLine)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, recordedAudioFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        recordingThread.start();

        micButton.setText("⏹️"); // Stop Icon
        isRecording = true;
    } catch (LineUnavailableException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to access microphone.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}

private void stopRecording() {
    if (audioLine != null) {
        audioLine.stop();
        audioLine.close();
        isRecording = false;
        micButton.setText("🎤");

        // Send the voice note as an attachment
        if (recordedAudioFile.exists()) {
            sendMessage(null, recordedAudioFile); // Send as attachment
        }
    }
}


    public static void main(String[] args) {
    Dashboard dashboard = new Dashboard("username");
        SwingUtilities.invokeLater(() -> new ChatWindow("username", 2, dashboard).setVisible(true));
    }
}

