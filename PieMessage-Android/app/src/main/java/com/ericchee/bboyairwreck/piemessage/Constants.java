package com.ericchee.bboyairwreck.piemessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by eric on 12/8/15.
 */
<<<<<<< HEAD
class Constants {

    static long getNowEpochSeconds() {
        return (getNowMilliseconds() - get2001Milliseconds()) / 1000;
    }

    static long get2001Milliseconds() {
        Date date2001 = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateInString = "01-01-2001 00:00:00";

            date2001 = sdf.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date2001 != null ? date2001.getTime() : 0;
    }

    private static long getNowMilliseconds() {
        return System.currentTimeMillis();
    }

    static final String KEY_TEXT_REPLY = "key_text_reply";

    static final String CHARSET = "UTF-8";
    static final String AES = "AES";
    static final String AES_PADDING = "AES/CBC/PKCS5PADDING";
    static final String IV = "iv";
    static final int SECRET_PAD_LEN = 16;

    static final String ME = "Me";
    static final String PRIVATE_MSG_PREFIX = "iMessage;-;";

    static final String NUM_MESSAGES = "numMessages";
    static final String ACTION = "action";
    static final String TEST_DATA = "Pie Message encryption test";
    static final String ENCRYPTED = "encryptedMsg";
    static final String SUCCESS = "success";
    static final String INCOMING = "incomingMessages";

    static class Action {
        static final String EST = "establish";
        static final String REQ = "requestNew";
        static final String SEND = "sendMessage";
        static final String READ = "markRead";
    }

    static class Col {
        static final String ID = "id";
        static final String DATE = "date";
        static final String MSG = "msg";
        static final String SENDER = "sender";
        static final String IS_SENT = "isSent";
        static final String IS_READ = "isRead";
        static final String IS_FROM_ME = "isFromMe";
        static final String CHAT_ID = "chatId";
        static final String CHAT_NAME = "chatName";
    }
=======
public class Constants {
    public static final String socketAddress = "127.0.0.1"; // INSERT YOUR PUBLIC IP HERE linked to OSX Client

    public static final String chatROWID = "chat_rowid";
    public static final String chatHandlesString = "chatHandlesString";
>>>>>>> parent of fec3bbf... Merge pull request #17 from bboyairwreck/ericChee-UniversalAPKWithIPAddressInput
}
