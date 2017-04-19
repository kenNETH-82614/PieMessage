package com.ericchee.bboyairwreck.piemessage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import java.util.ArrayList;
import java.util.TreeSet;

public class MessageActivity extends Activity {
    private static final String TAG = MessageActivity.class.getSimpleName();
    private TextView tvTarget;
    private EditText etTarget;
    private EditText etMessage;
    private ListView lvMessages;
    private TreeSet<Message> setOfMessages;
    private MessagesAdapter adapter;
    private ImageButton btnSend;
    private String chatId = "";
    private String chatName = "";
    private boolean isNewChat = true;
    private int notificationId = 0;

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating activity");

        if (getIntent() != null) {
            chatId = getIntent().getStringExtra(Constants.Col.CHAT_ID);
            chatName = getIntent().getStringExtra(Constants.Col.CHAT_NAME);
            notificationId = getIntent().getIntExtra(Constants.Col.ID, 0);
            if (!chatId.equals("")) {
                isNewChat = false;
            }
        }


        Bundle remoteInput = RemoteInput.getResultsFromIntent(getIntent());

        if (remoteInput != null) {
            String inputString = remoteInput.getCharSequence(Constants.KEY_TEXT_REPLY).toString();

            Message messageInProgress = new Message(inputString, chatId, chatName);
            Message lastMessage = new TreeSet<>(PieMessageApplication.getInstance().getChats().values()).last().getLastMessage();
            messageInProgress.setId(lastMessage.getId() + 1);
            messageInProgress.setDate(lastMessage.getDate() + 1);

            PieMessageApplication.getInstance().addMessage(messageInProgress);
            PieMessageApplication.getInstance().getServerBridge().sendMessage(messageInProgress, this, true);
            updateNotification();
            finish();
            return;
        }

        setContentView(R.layout.activity_message);
        setStatusBarColor();    // set status bar color

        tvTarget = (TextView) findViewById(R.id.tvTarget);
        etTarget = (EditText) findViewById(R.id.etTarget);

        setTvTargetListener();
        initMessagesListAdapter();

        btnSend = (ImageButton) findViewById(R.id.btnSend);
        etMessage = (EditText) findViewById(R.id.etMessage);

        btnSend.setEnabled(true);

        setSendOnClickListener();
        setBackButtonListener();

        if (PieMessageApplication.getInstance().getChats().containsKey(chatId)) {
            for (Message msg : PieMessageApplication.getInstance().getChats().get(chatId).getMessages()) {
                if (!msg.isRead()) {
                    msg.setRead(true);
                }
            }
        }

        PieMessageApplication.getInstance().getServerBridge().addActivity(this);
    }

    private void setSendOnClickListener() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "pressed");

                updateTargetValue();
                if (hasSetTargetNumber()) {
                    // if has number, send msg
                    String message = etMessage.getText().toString().trim();

                    etMessage.setText(message);
                    etTarget.setVisibility(View.GONE);
                    tvTarget.setVisibility(View.VISIBLE);

                    if (message.length() > 0) {
                        addSentMessageToListView(message);
                    } else {
                        Log.i(TAG, "Message text has no length");
                    }
                } else {
                    // Hasn't set target value
                    Log.d(TAG, "Has not set target value");
                }
            }
        });
    }

    private void addSentMessageToListView(String message) {
        Message messageInProgress = new Message(message, chatId, chatName);
        Message lastMessage = new TreeSet<>(PieMessageApplication.getInstance().getChats().values()).last().getLastMessage();
        messageInProgress.setId(lastMessage.getId() + 1);
        messageInProgress.setDate(lastMessage.getDate() + 1);

        PieMessageApplication.getInstance().addMessage(messageInProgress);
        PieMessageApplication.getInstance().getServerBridge().sendMessage(messageInProgress, this, false);
        setOfMessages.add(messageInProgress);
        etMessage.setText(null);
        adapter.notifyDataSetChanged();
        lvMessages.smoothScrollToPosition(adapter.getCount() - 1);
    }

    private void setTvTargetListener() {
        if (!isNewChat) {
            // If previous chat, Set the Handle ID
            chatId = getIntent().getStringExtra(Constants.Col.CHAT_ID);
            chatName = getIntent().getStringExtra(Constants.Col.CHAT_NAME);
            tvTarget.setText(chatName);
            etTarget.setText(chatName);
        } else {
            tvTarget.setVisibility(View.GONE);
            etTarget.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasSetTargetNumber() {
        return !tvTarget.getText().toString().equals(getString(R.string.insert_number));
    }

    private void updateTargetValue() {
        String valueOfTarget = etTarget.getText().toString().trim();
        if (isNewChat && !chatId.startsWith(Constants.PRIVATE_MSG_PREFIX)) {
            chatName = valueOfTarget;
            if (valueOfTarget.matches("\\d+")) {
                valueOfTarget = "+1" + valueOfTarget;
            }
            chatId = Constants.PRIVATE_MSG_PREFIX + valueOfTarget;
        }

        if (valueOfTarget.length() > 0) {
            tvTarget.setText(valueOfTarget);
        } else {
            tvTarget.setText(getString(R.string.recipient));
            Log.i(TAG, "Target value is invalid");
            Toast.makeText(
                    getApplicationContext(),
                    "Please enter a valid recipient",
                    Toast.LENGTH_SHORT)
                    .show();
        }
        etTarget.setText(valueOfTarget);    // set etTarget to trimmed string
    }

    // Construct data and set in custom adapter
    void initMessagesListAdapter() {
        if (!isNewChat) {
            // grab all messages from chat in sqlite db
            setOfMessages = PieMessageApplication.getInstance().getChats().get(chatId).getMessages();
        } else {
            // If new chat, initiate new array of messages
            setOfMessages = new TreeSet<>();
        }
        adapter = new MessagesAdapter(this, new ArrayList<>(setOfMessages));

        // Attach adapter to listView
        lvMessages = (ListView) findViewById(R.id.lvMessages);
        lvMessages.setAdapter(adapter);
        lvMessages.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                ArrayList<Message> messages = new ArrayList<>(setOfMessages);
                for (int i = 0; i < visibleItemCount; i++) {
                    Message msg = messages.get(i + firstVisibleItem);
                    if (!msg.isRead()) {
                        msg.setRead(true);
                    }
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setStatusBarColor() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getApplicationContext().getColor(R.color.darkGrey));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Resuming activity");
        PieMessageApplication.getInstance().getServerBridge().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroying activity");
        PieMessageApplication.getInstance().getServerBridge().removeActivity(this);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Pausing activity");
        PieMessageApplication.getInstance().getServerBridge().removeActivity(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("etTarget", etTarget.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        String etTargetText = savedInstanceState.getString("etTarget");
        etTarget.setText(etTargetText);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void setBackButtonListener() {
        ImageButton ibMABackArrow = (ImageButton) findViewById(R.id.backArrow);
        ibMABackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatsIntent = new Intent(MessageActivity.this, ChatsActivity.class);
                startActivity(chatsIntent);
            }
        });
    }

    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    String getChatId() {
        return chatId;
    }
}
