package com.alangeorge.android.hermes;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;
import com.alangeorge.android.hermes.model.ModelException;
import com.alangeorge.android.hermes.model.dao.DBHelper;
import com.alangeorge.android.hermes.model.provider.HermesContentProvider;
import com.alangeorge.android.hermes.services.MessageSenderService;

import java.security.KeyPair;

public class ConversationFragment extends ListFragment implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ConversationFragment";

    private EditText messageEditText;
    private Contact contact;
    private SimpleCursorAdapter adapter;

    private HandlerThread conversationMessageThread;
    private Messenger messageSenderServiceMessenger;
    private Messenger contactDetailFragmentMessenger;

    private ServiceConnection messageSenderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected(" + name + ", " + service + ")");
            messageSenderServiceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected(" + name + ")");
            messageSenderServiceMessenger = null;
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    private boolean isServiceBound = false;

    public ConversationFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        conversationMessageThread = new HandlerThread("ConversationMessageThread", android.os.Process.THREAD_PRIORITY_DEFAULT);
        conversationMessageThread.start();
        contactDetailFragmentMessenger = new Messenger(new MessageSenderServiceInboundMessageHandler(conversationMessageThread.getLooper()));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        conversationMessageThread.quitSafely();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (! getActivity().bindService(new Intent(getActivity(), MessageSenderService.class), messageSenderServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "failed to bind to MessageSenderService");
            isServiceBound = false;
        } else {
            isServiceBound = true;
        }

        getActivity().setTitle(contact.getName());
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(messageSenderServiceConnection);
        isServiceBound = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        long contactId = intent.getLongExtra(ConversationActivity.ARG_ITEM_ID, 0);

        if (contactId == 0) {
            Toast.makeText(getActivity(), "Unable to find contact id in Intent", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

        try {
            contact = new Contact(contactId);
        } catch (ModelException e) {
            Log.d(TAG, "unable to load contact with id " + contactId, e);
            Toast.makeText(getActivity(), "Unable to find contact", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        messageEditText = (EditText) view.findViewById(R.id.message_text);

        ImageButton sendMessageButton = (ImageButton) view.findViewById(R.id.send_button);
        sendMessageButton.setOnClickListener(this);

        fillConversationData();

        return view;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick()");
        Log.d(TAG, "message text = " + messageEditText.getText());

        if (messageEditText.getText() == null || "".equals(messageEditText.getText().toString())) {
            Toast.makeText(getActivity(), "Empty Message", Toast.LENGTH_LONG).show();
            return;
        }

        if (messageSenderServiceMessenger == null) {
            Toast.makeText(getActivity(), "Unable to send messages, not bound to message sending service", Toast.LENGTH_LONG).show();
            return;
        }

        String gcmId = App.getGcmRegistrationId();
        KeyPair fromKeyPair = App.getMyKeyPair();

        if (gcmId == null || "".equals(gcmId)) {
            Toast.makeText(getActivity(), "Unable to send messages with a null GCM registration ID", Toast.LENGTH_LONG).show();
            return;
        }

        if (fromKeyPair == null) {
            Toast.makeText(getActivity(), "Unable to send messages with a null KeyPair", Toast.LENGTH_LONG).show();
            return;
        }

        MessageMaker messageMaker = new MessageMaker();
        Message message;

        try {
            message = messageMaker.make(messageEditText.getText().toString(), contact, App.getGcmRegistrationId(), App.getMyKeyPair());
        } catch (MessageMakerException e) {
            Toast.makeText(getActivity(), "Unable to create message", Toast.LENGTH_LONG).show();
            return;
        }

        message.setInbound(false);

        ContentValues messageValues = new ContentValues();
        messageValues.put(DBHelper.MESSAGE_COLUMN_CONTACT_ID, contact.getId());
        messageValues.put(DBHelper.MESSAGE_COLUMN_SENDER_ENCODED_KEY, message.getSenderEncodedSymKey());
        messageValues.put(DBHelper.MESSAGE_COLUMN_MESSAGE_JSON, message.toJson());
        messageValues.put(DBHelper.MESSAGE_CONTACT_COLUMN_MESSAGE_IS_INBOUND, false);

        getActivity().getContentResolver().insert(HermesContentProvider.MESSAGES_CONTENT_URI, messageValues);

        android.os.Message serviceMessage = android.os.Message.obtain(null, MessageSenderService.MSG_SEND_MESSAGE);
        serviceMessage.getData().putString(MessageSenderService.ARG_MESSAGE_TEXT, message.toJson());
        serviceMessage.getData().putString(MessageSenderService.ARG_GCM_ID, contact.getGcmId());
        serviceMessage.replyTo = contactDetailFragmentMessenger;
        try {
            messageSenderServiceMessenger.send(serviceMessage);
        } catch (RemoteException e) {
            Log.e(TAG, "error communicating with MessageSenderService", e);
            Toast.makeText(getActivity(), "error communicating with MessageSenderService", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader()");
        return new CursorLoader(
                getActivity(),
                HermesContentProvider.MESSAGES_CONTACT_CONTENT_URI,
                DBHelper.MESSAGE_CONTACT_ALL_COLUMNS,
                DBHelper.MESSAGE_CONTACT_COLUMN_CONTACT_ID + " = ?",
                new String[] {Long.toString(contact.getId())},
                DBHelper.MESSAGE_CONTACT_COLUMN_CONTACT_CREATE_TIME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished()");
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset()");
        adapter.swapCursor(null);
    }

    private void fillConversationData() {
        String[] from = new String[] {
                DBHelper.MESSAGE_COLUMN_MESSAGE_JSON
        };

        int[] to = new int[] {
                R.id.message_text
        };

        //getLoaderManager().enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);

        adapter = new MessageCursorAdapter(getActivity(), R.layout.list_item_conversaion, null, from, to, 0);

        setListAdapter(adapter);
    }

    public void deleteConversation() {
        if (contact == null) {
            Toast.makeText(getActivity(), "Unable to remove conversation: contact == null", Toast.LENGTH_LONG).show();
            return;
        }

        getActivity().getContentResolver().delete(Uri.parse(HermesContentProvider.MESSAGES_CONTACT_CONTENT_URI + "/" + contact.getId()), null, null);
    }

    private class MessageSenderServiceInboundMessageHandler extends Handler {

        private MessageSenderServiceInboundMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message message) {
            switch (message.what) {
                case MessageSenderService.MSG_SEND_SUCCESS:
                    Log.d(TAG, "Message send success");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageEditText.setText("");
                        }
                    });
                    break;
                case MessageSenderService.MSG_SEND_FAILED:
                    Log.e(TAG, "Message send failed");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Message send failed", Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                default:
                    Log.d(TAG, "unknown message : " + message.what);
                    super.handleMessage(message);
            }
        }
    }

    private class MessageCursorAdapter extends SimpleCursorAdapter {
        private static final String TAG = "MessageCursorAdapter";
        @SuppressWarnings("UnusedDeclaration")
        private Context context;
        private RelativeLayout.LayoutParams inboundLayout;
        private RelativeLayout.LayoutParams outboundLayout;

        public MessageCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags) {
            super(context, layout, cursor, from, to, flags);
            this.context = context;

            inboundLayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            outboundLayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            inboundLayout.addRule(RelativeLayout.ALIGN_PARENT_START);
            inboundLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            outboundLayout.addRule(RelativeLayout.ALIGN_PARENT_END);
            outboundLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            TextView messageTextView = (TextView) view.findViewById(R.id.message_text);

            Message message;
            try {
                message = Message.cursorToMessage(getCursor());
            } catch (ModelException e) {
                Log.e(TAG, "error converting cursor to Message", e);
                Toast.makeText(context, "Unable to retrieve Message from database", Toast.LENGTH_LONG).show();
                messageTextView.setText("Unable to retrieve Message from database");
                return view;
            }

            if (message.isInbound()) {
                messageTextView.setLayoutParams(inboundLayout);
            } else {
                messageTextView.setLayoutParams(outboundLayout);
            }

            try {
                if (message.isInbound()) {
                    messageTextView.setText(message.getInboundMessageClearText(App.getMyKeyPair().getPrivate()));
                } else {
                    messageTextView.setText(message.getOutboundMessageClearText(App.getMyKeyPair().getPrivate()));                    }
            } catch (Exception e) {
                Log.e(TAG, "error while decrypting message", e);
                Toast.makeText(getActivity(), "Unable to decrypt message:" + e.getMessage(), Toast.LENGTH_LONG).show();
                messageTextView.setText("Unable to retrieve Message from database");
                return view;
            }

            return view;
        }
    }
}
