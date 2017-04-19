package com.ericchee.bboyairwreck.piemessage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class ChatsActivity extends Activity {
    private static final String TAG = ChatsActivity.class.getSimpleName();
    private static final int SETTINGS_RESULT = 1;
    private SwipeRefreshLayout swipeContainer;
    private ListView lvChats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarChat);
//        setSupportActionBar(toolbar);

        Log.i(TAG, "creating ChatsActivity");

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                PieMessageApplication.getInstance().getServerBridge().requestNew(true, new ServerBridge.ResponseCallback() {
                    @Override
                    public void run(JSONObject responseJson) {
                        try {
                            if (responseJson.getInt(Constants.NUM_MESSAGES) > 0) {
                                for (Message message : ServerBridge.parseMessages(responseJson)) {
                                    PieMessageApplication.getInstance().addMessage(message);
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (swipeContainer.isRefreshing()) swipeContainer.setRefreshing(false);
                                    reloadChatListAndAdapter();
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_dark);

        reloadChatListAndAdapter();

        // Navigate to MessageActivity when selecting chat item
        lvChats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Chat chat = (Chat) parent.getItemAtPosition(position);

                Intent messageIntent = new Intent(ChatsActivity.this, MessageActivity.class);
                messageIntent.putExtra(Constants.Col.CHAT_ID, chat.getId());
                messageIntent.putExtra(Constants.Col.CHAT_NAME, chat.getName());
                startActivity(messageIntent);
            }
        });

        ImageButton composeButton = (ImageButton) findViewById(R.id.composeButton);
        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent messageIntent = new Intent(ChatsActivity.this, MessageActivity.class);
                messageIntent.putExtra(Constants.Col.CHAT_ID, "");
                messageIntent.putExtra(Constants.Col.CHAT_NAME, "");
                startActivity(messageIntent);
            }
        });

        PieMessageApplication.getInstance().getServerBridge().addActivity(this);
    }

    void reloadChatListAndAdapter() {

        ArrayList<Chat> chatsList = new ArrayList<>(PieMessageApplication.getInstance().getChats().values());
        Collections.sort(chatsList, new Comparator<Chat>() {
            @Override
            public int compare(Chat o1, Chat o2) {
                return o2.compareTo(o1);
            }
        });

        // Set adapter for chats list view
        lvChats = (ListView) findViewById(R.id.lvChats);
        TextView messagesTitle = (TextView) findViewById(R.id.tvMessageTitle);
        ChatsAdapter chatsAdapter = new ChatsAdapter(this, chatsList, messagesTitle);
        lvChats.setAdapter(chatsAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent piePreferenceIntent = new Intent(this, PiePreferenceActivity.class);
            startActivityForResult(piePreferenceIntent, SETTINGS_RESULT);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Resuming ChatsActivity");
        PieMessageApplication.getInstance().getServerBridge().addActivity(this);
        reloadChatListAndAdapter();
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
}
