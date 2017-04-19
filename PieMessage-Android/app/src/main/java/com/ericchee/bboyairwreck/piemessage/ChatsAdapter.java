package com.ericchee.bboyairwreck.piemessage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by echee on 12/16/15.
 */
public class ChatsAdapter extends ArrayAdapter<Chat> {
    private static final String TAG = ChatsAdapter.class.getSimpleName();
    private Context context;
    private TextView messagesTitle;
    private int numUnread = 0;

    ChatsAdapter(Context context, ArrayList<Chat> chats, TextView messagesTitle) {
        super(context, 0, chats);
        this.context = context;
        this.messagesTitle = messagesTitle;
        messagesTitle.setText(context.getString(R.string.messages));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "Hitting ChatsAdapter to insert chat");

        Chat chat = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat, parent, false);
        }

        TextView tvChatName = (TextView) convertView.findViewById(R.id.tvChatName);
        TextView tvLastMessage = (TextView) convertView.findViewById(R.id.tvLastMessage);
        TextView tvLastDate = (TextView) convertView.findViewById(R.id.tvLastDate);
        View vUnreadCircle = convertView.findViewById(R.id.vUnreadCircle);

        String name = chat.getName();
        String lastMessageText = chat.getLastMessage().getMsg();
        if (chat.getLastMessage().getMessageType() == MessageType.RECEIVED) {
            lastMessageText = chat.getLastMessage().getSender() + ": " + lastMessageText;
        }
        tvLastDate.setText(getDateStr(chat.getLastMessage().getDate()));
        tvChatName.setText(name);
        tvLastMessage.setText(lastMessageText);

        for (Message message : chat.getMessages()) {
            if (!message.isRead()) {
                vUnreadCircle.setVisibility(View.VISIBLE);
                messagesTitle.setText(context.getString(R.string.messages) + " (" + ++numUnread + ")");
                break;
            }
        }


        return convertView;
    }

    int getNumUnread() {
        return numUnread;
    }

    private String getDateStr(long dateSeconds) {
        long dateDiff = Constants.getNowEpochSeconds() - dateSeconds;
        dateSeconds = dateSeconds * 1000 + Constants.get2001Milliseconds(); // Change to milliseconds since epoch
        Date date = new Date(dateSeconds);

        SimpleDateFormat dateStr = new SimpleDateFormat("M/d/yy", Locale.US);
        SimpleDateFormat dayOfWeek = new SimpleDateFormat("E", Locale.US);
        SimpleDateFormat time = new SimpleDateFormat("h:mm a", Locale.US);
        if (dateDiff > 7*24*60*60) { // > 1 week
            return dateStr.format(date);
        } else if (dateDiff > 2*24*60*60) { // > 2 days
            return dayOfWeek.format(date);
        } else if (dateDiff > 24*60*60) {// 1 day
            return "Yesterday";
        } else {
            return time.format(date);
        }
    }
}
