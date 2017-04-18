package com.ericchee.bboyairwreck.piemessage;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by eric on 11/13/15.
 */
public class MessagesAdapter extends ArrayAdapter<Message> {
    private static final String TAG = MessagesAdapter.class.getSimpleName();

    MessagesAdapter(Context context, ArrayList<Message> messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "Hitting MessagesAdapter to insert message");

        // Get data item from this postion
        Message message = getItem(position);

        View messageTypeContainer;

        // Check if existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_message, parent, false);
        }

        if (message.getChatName().equals(message.getSender())) {
            convertView.findViewById(R.id.tvSender).setVisibility(View.GONE);
        }

        if (message.getMessageType() == MessageType.RECEIVED) {
            messageTypeContainer = convertView.findViewById(R.id.llItemMessageReceived);
            convertView.findViewById(R.id.llItemMessageReceived).setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.llItemMessageSent).setVisibility(View.GONE);
        } else {
            messageTypeContainer = convertView.findViewById(R.id.llItemMessageSent);
            convertView.findViewById(R.id.llItemMessageSent).setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.llItemMessageReceived).setVisibility(View.GONE);
        }

        // Find views to populate data in the subviews
        TextView tvMessage = (TextView) messageTypeContainer.findViewById(R.id.tvMessage);

        tvMessage.setText(message.getMsg());

        if (message.getMessageType() != MessageType.RECEIVED) {
            Drawable tvMessageBackground = tvMessage.getBackground();
            switch (message.getMessageStatus()) {
                case SUCCESSFUL:
                    tvMessageBackground = messageTypeContainer.getResources().getDrawable(R.drawable.round_rectangle_blue);  // TODO getDrawable(id, Theme) for lollipop
                    break;
                case UNSUCCESSFUL:
                    tvMessageBackground = messageTypeContainer.getResources().getDrawable(R.drawable.round_rectangle_aquamarine);  // TODO getDrawable(id, Theme) for lollipop
                    break;
                case IN_PROGRESS:
                    tvMessageBackground = messageTypeContainer.getResources().getDrawable(R.drawable.round_rectangle_grey);  // TODO getDrawable(id, Theme) for lollipop
                    break;
            }

            tvMessage.setBackground(tvMessageBackground);
        } else {
            TextView tvHandleID = (TextView) messageTypeContainer.findViewById(R.id.tvSender);
            tvHandleID.setText(message.getSender());
        }


        return convertView;
    }
}
