package com.ericchee.bboyairwreck.piemessage;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by david on 4/14.
 *
 * @author david
 */
class ServerBridge {
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;

    private boolean isConnected = false;
    private boolean shouldPing = true;

    private SecretKeySpec secretKey;
    private Cipher cipher;
    private byte[] iv;

    private TreeMap<String, Chat> chats = PieMessageApplication.getInstance().getChats();
    private HashSet<ChatsActivity> chatsActivities;
    private HashSet<MessageActivity> messageActivities;

    ServerBridge() throws IOException {
        chatsActivities = new HashSet<>();
        messageActivities = new HashSet<>();
        final PieMessageApplication.SocketInfo sInfo = PieMessageApplication.getInstance().getSocket();
        try {
            cipher = Cipher.getInstance(Constants.AES_PADDING);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(sInfo.getSocketAddress(), sInfo.getPort());
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (shouldPing) {
                    try {
                        Thread.sleep(3000);

                        if (!socket.isConnected()) {
                            isConnected = false;
                            continue;
                        }
                        if (!isConnected) establishConnection();
                        else requestNew(false, new ResponseCallback() {
                            @Override
                            public void run(JSONObject responseJson) {
                                try {
                                    if (responseJson.getInt(Constants.NUM_MESSAGES) > 0) {
                                        HashSet<String> openChatIds = new HashSet<>();
                                        for (final MessageActivity messageActivity : messageActivities) {
                                            openChatIds.add(messageActivity.getChatId());
                                        }
                                        for (Message message : parseMessages(responseJson)) {
                                            PieMessageApplication.getInstance().addMessage(message);
                                            if (!message.isRead() && !openChatIds.contains(message.getChatId())) {
                                                Notification.postNotification(message.getMsg(), message.getSender(), message.getChatId(), message.getChatName());
                                            }
                                        }
                                        for (final ChatsActivity chatsActivity : chatsActivities) {
                                            chatsActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    chatsActivity.reloadChatListAndAdapter();
                                                }
                                            });
                                        }
                                        for (final MessageActivity messageActivity : messageActivities) {
                                            messageActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    messageActivity.initMessagesListAdapter();
                                                }
                                            });
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private synchronized void establishConnection() {
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PieMessageApplication.getInstance());
            String password = sharedPreferences.getString(PieMessageApplication.getInstance().getString(R.string.pref_password), Constants.TEST_DATA);
            secretKey = genSecretKey(password);

            iv = new byte[Constants.SECRET_PAD_LEN];
            SecureRandom prng = new SecureRandom();
            prng.nextBytes(iv);

            JSONObject connectJson = new JSONObject();
            connectJson.put(Constants.ACTION, Constants.Action.EST);
            connectJson.put(Constants.Col.MSG, Constants.TEST_DATA);
            String connectStr = connectJson.toString();
            connectJson = new JSONObject();
            try {
                connectJson.put(Constants.ENCRYPTED, encrypt(connectStr));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            connectJson.put(Constants.IV, Base64.encodeToString(iv, Base64.NO_WRAP));
            Log.i(ServerBridge.class.getSimpleName(), "Waiting for verification...");
            sendNoEncrypt(connectJson, new ResponseCallback() { // Step 1 of connecting: send test data to server (encrypted)
                @Override
                public void run(JSONObject responseJson) {
                    isConnected = false;
                    try {
                        if (responseJson.getBoolean(Constants.SUCCESS)) { // Step 2 of connecting: receive server's response, whether connection successful (encrypted)
                            Log.i(ServerBridge.class.getSimpleName(), "Success!");
                            isConnected = true;
                        } else {
                            Log.i(ServerBridge.class.getSimpleName(), "Failure!");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SecretKeySpec genSecretKey(String password) throws UnsupportedEncodingException {
        if (password.length() < Constants.SECRET_PAD_LEN) {
            int missingLength = Constants.SECRET_PAD_LEN - password.length();
            StringBuilder passwordBuilder = new StringBuilder(password);
            for (int i = 0; i < missingLength; i++) {
                passwordBuilder.append(" ");
            }
            password = passwordBuilder.toString();
        }
        byte[] key = password.substring(0, Constants.SECRET_PAD_LEN).getBytes(Constants.CHARSET);
        return new SecretKeySpec(key, Constants.AES);
    }

    void requestNew(boolean getAll, ResponseCallback callback) {
        long date = 0;
        if (!getAll && !chats.isEmpty()) date = new TreeSet<>(this.chats.values()).last().getLastMessage().getDate();
        JSONObject message = new JSONObject();
        try {
            message.put(Constants.ACTION, Constants.Action.REQ);
            message.put(Constants.Col.DATE, date);
        } catch (JSONException ignored) {}
        sendRaw(message, callback);
    }

    void markAsRead(long messageId) {
        JSONObject message = new JSONObject();
        try {
            message.put(Constants.ACTION, Constants.Action.READ);
            message.put(Constants.Col.ID, messageId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendRaw(message, null);
    }

    void sendMessage(final Message tempMessage, final MessageActivity messageActivity, final boolean isReply) {
        final JSONObject message = new JSONObject();
        try {
            message.put(Constants.ACTION, Constants.Action.SEND);
            message.put(Constants.Col.CHAT_ID, tempMessage.getChatId());
            message.put(Constants.Col.MSG, tempMessage.getMsg());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendRaw(message, new ResponseCallback() {
            @Override
            public void run(JSONObject responseJson) {
                try {
                    Message newMessage = parseMessage(responseJson);
                    Chat chat = newMessage.getChat();
                    chat.removeMessage(tempMessage.getId());
                    chat.addMessage(newMessage.getId(), newMessage);
                    if (!isReply) {
                        messageActivity.initMessagesListAdapter();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendRaw(JSONObject message, final ResponseCallback callback) {
        while (!isConnected) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String messageString = message.toString();
        message = new JSONObject();
        try {
            message.put(Constants.ENCRYPTED, encrypt(messageString));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendNoEncrypt(message, new ResponseCallback() {
            @Override
            public void run(JSONObject response) {
                if (response.has(Constants.ENCRYPTED)) {
                    try {
                        response = new JSONObject(decrypt(response.getString(Constants.ENCRYPTED)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (callback != null) callback.run(response);
            }
        });
    }

    private synchronized void sendNoEncrypt(final JSONObject message, final ResponseCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                output.println(message.toString());
                try {
                    String response = input.readLine();
                    if (response == null) {
                        isConnected = false;
                        return;
                    }
                    JSONObject responseJson = new JSONObject(response);
                    if (callback != null) callback.run(responseJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    static TreeSet<Message> parseMessages(JSONObject messagesJson) throws JSONException {
        TreeSet<Message> messages = new TreeSet<>();
        JSONArray messagesArray = messagesJson.getJSONArray(Constants.INCOMING);

        for (int i = 0; i < messagesArray.length(); i++) {
            messages.add(parseMessage((JSONObject) messagesArray.get(i)));
        }
        return messages;
    }

    private static Message parseMessage(JSONObject messageJson) throws JSONException {
        long messageId = messageJson.getLong(Constants.Col.ID);
        String msg = messageJson.getString(Constants.Col.MSG);
        long date = messageJson.getLong(Constants.Col.DATE);
        int isFromMe = messageJson.getInt(Constants.Col.IS_FROM_ME);
        int isSent = messageJson.getInt(Constants.Col.IS_SENT);
        String sender = messageJson.getString(Constants.Col.SENDER);
        int isRead = messageJson.getInt(Constants.Col.IS_READ);
        String chatId = messageJson.getString(Constants.Col.CHAT_ID);
        String chatName = messageJson.getString(Constants.Col.CHAT_NAME);

        return new Message(messageId, msg, date, sender, isSent, isFromMe, isRead, chatId, chatName);
    }

    private String encrypt(String data) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return Base64.encodeToString(cipher.doFinal(data.getBytes(Constants.CHARSET)), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String decrypt(String data) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Constants.CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void stop() {
        try {
            socket.close();
            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cipher = null;
        isConnected = false;
        shouldPing = false;
    }

    void addActivity(Activity activity) {
        if (activity instanceof ChatsActivity) {
            chatsActivities.add((ChatsActivity) activity);
        } else if (activity instanceof MessageActivity) {
            messageActivities.add(((MessageActivity) activity));
        }
    }

    void removeActivity(Activity activity) {
        if (activity instanceof ChatsActivity) {
            chatsActivities.remove(activity);
        } else if (activity instanceof MessageActivity) {
            messageActivities.remove(activity);
        }
    }

    interface ResponseCallback {
        void run(JSONObject response);
    }
}
