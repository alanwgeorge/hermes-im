package com.alangeorge.android.hermes;

import android.content.Intent;
import android.os.Bundle;
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
import com.alangeorge.android.hermes.model.ModelException;


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

        public ContactDetailFragment() { }

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
        }
    }
}
