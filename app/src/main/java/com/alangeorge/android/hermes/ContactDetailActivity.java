package com.alangeorge.android.hermes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;
import com.alangeorge.android.hermes.model.ModelException;
import com.alangeorge.android.hermes.services.MessageSenderService;

import java.security.KeyPair;

import static com.alangeorge.android.hermes.services.MessageSenderService.ARG_GCM_ID;
import static com.alangeorge.android.hermes.services.MessageSenderService.ARG_MESSAGE_TEXT;
import static com.alangeorge.android.hermes.services.MessageSenderService.MSG_SEND_FAILED;
import static com.alangeorge.android.hermes.services.MessageSenderService.MSG_SEND_MESSAGE;
import static com.alangeorge.android.hermes.services.MessageSenderService.MSG_SEND_SUCCESS;


public class ContactDetailActivity extends ActionBarActivity {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "ContactDetailActivity";
    public static final String ARG_ITEM_ID = "arg_item_id";

    private ContactDetailFragment fragment = new ContactDetailFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.activity_contact);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contact_detail, menu);
        return true;
    }

    /**
     * Maps the Home (android.R.id.home) selection to ending this {@link android.app.Activity}
     *
     * @param item menu item selected
     * @return where the menu selection was handled or not
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class ContactDetailFragment extends Fragment implements View.OnClickListener {
        private static final String TAG = "ContactDetailFragment";

        private EditText messageEditText;
        private Contact contact;

        private Messenger messageSenderServiceMessenger = null;
        private Messenger contactDetailFragmentMessenger = new Messenger(new MessageSenderServiceInboundMessageHandler());

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

        public ContactDetailFragment() { }

        @Override
        public void onStart() {
            super.onStart();
            if (! getActivity().bindService(new Intent(getActivity(), MessageSenderService.class), messageSenderServiceConnection, Context.BIND_AUTO_CREATE)) {
                Log.e(TAG, "failed to bind to MessageSenderService");
                isServiceBound = false;
            } else {
                isServiceBound = true;
            }
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
            long contactId = intent.getLongExtra(ARG_ITEM_ID, 0);

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

            TextView contactName = (TextView) view.findViewById(R.id.contact_name);
            contactName.setText(contact.getName());

            Button sendMessageButton = (Button) view.findViewById(R.id.send_button);
            sendMessageButton.setOnClickListener(this);

            return view;
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick()");
            Log.d(TAG, "message text = " + messageEditText.getText());

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

            android.os.Message serviceMessage = android.os.Message.obtain(null, MSG_SEND_MESSAGE);
            serviceMessage.getData().putString(ARG_MESSAGE_TEXT, message.toJson());
            serviceMessage.getData().putString(ARG_GCM_ID, contact.getGcmId());
            serviceMessage.replyTo = contactDetailFragmentMessenger;
            try {
                messageSenderServiceMessenger.send(serviceMessage);
            } catch (RemoteException e) {
                Log.e(TAG, "error communicating with MessageSenderService", e);
                Toast.makeText(getActivity(), "error communicating with MessageSenderService", Toast.LENGTH_LONG).show();
            }
        }

        private class MessageSenderServiceInboundMessageHandler extends Handler {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_SEND_SUCCESS:
                        Log.d(TAG, "Message send success");
                        break;
                    case MSG_SEND_FAILED:
                        Log.e(TAG, "Message send failed");
                        break;
                    default:
                        Log.d(TAG, "unknown message : " + msg.what);
                        super.handleMessage(msg);
                }
            }
        }
    }
}
