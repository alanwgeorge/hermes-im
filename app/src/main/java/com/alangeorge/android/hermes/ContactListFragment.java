package com.alangeorge.android.hermes;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.ModelException;
import com.alangeorge.android.hermes.model.dao.DBHelper;
import com.alangeorge.android.hermes.model.provider.HermesContentProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ContactListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ContactListFragment";

    private SimpleCursorAdapter adapter;

    public ContactListFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_list_container, container, false);
        fillData();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contact, menu);

        if (BuildConfig.DEBUG) {
            menu.add(1, R.id.action_add_contact_self, 1, "Add Self");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_contact_add:
                IntentIntegrator intentIntegrator = IntentIntegrator.forSupportFragment(this);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                intentIntegrator.initiateScan();
                break;
            case R.id.action_display_address_qr_code:
                displayContactQrCode();
                break;
            case R.id.action_add_contact_self:
                addContactSelf();
                break;
            default:
                Log.d(TAG, "unknown menu id: " + id);
        }
        return super.onOptionsItemSelected(item);
    }

    private void addContactSelf() {
        ContentValues values = new ContentValues();
        values.put(DBHelper.CONTACT_COLUMN_GCM_ID, App.getGcmRegistrationId());
        values.put(DBHelper.CONTACT_COLUMN_NAME, "Myself");
        values.put(DBHelper.CONTACT_COLUMN_PUBLIC_KEY, Base64.encodeToString(App.getMyKeyPair().getPublic().getEncoded(), Base64.NO_WRAP));
        getActivity().getContentResolver().insert(HermesContentProvider.CONTACTS_CONTENT_URI, values);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Log.d(TAG, "onListItemClick(" + listView + ", " + view + ", " + position + ", " + id + ")");
        super.onListItemClick(listView, view, position, id);

        Contact contact = (Contact) view.getTag(R.id.contact_view_tag_id);

        Log.d(TAG, "contact from view = " + contact);

        Intent detailIntent = new Intent(getActivity(), ConversationActivity.class);
        detailIntent.putExtra(ConversationActivity.ARG_ITEM_ID, contact.getId());
        startActivity(detailIntent);
    }

    /**
     * Here we process the result of a QR scan.
     * Steps:
     *
     * 1. retrieve the QR scan JSON text
     * 2. marshall QR JSON text in to Contact object
     * 3. prompt user for Contact name
     * 4. use contentprovider to persist contact
     *
     * @param requestCode requestCode
     * @param resultCode resultCode
     * @param data intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        final EditText contactNameInput = new EditText(getActivity());
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(getActivity(), "Scan Cancelled", Toast.LENGTH_LONG).show();
            } else {

                Log.d(TAG, "Scanned from fragment: " + result.getContents());

                GsonBuilder builder = new GsonBuilder();
                builder.excludeFieldsWithoutExposeAnnotation();
                Gson gson = builder.create();

                final Contact contact = gson.fromJson(result.getContents(), Contact.class);

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                alertBuilder.setTitle(R.string.title_getname_dialog)
                        .setMessage(R.string.message_getname_dialog);

                alertBuilder.setView(contactNameInput);

                alertBuilder.setPositiveButton(R.string.label_getname_dialag_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String contactName = contactNameInput.getText().toString();
                        boolean error = false;

                        if (TextUtils.isEmpty(contactName)) {
                            Log.d(TAG, "no name found from dialog");
                            return;
                        } else {
                            Log.d(TAG, "name = " + contactName);
                        }

                        contact.setName(contactName);

                        if (contact.validateForInsert()) {
                            ContentValues values = new ContentValues();

                            values.put(DBHelper.CONTACT_COLUMN_NAME, contact.getName());
                            values.put(DBHelper.CONTACT_COLUMN_GCM_ID, contact.getGcmId());
                            values.put(DBHelper.CONTACT_COLUMN_PUBLIC_KEY, contact.getPublicKeyEncoded());

                            Uri insertedContactUri = getActivity().getContentResolver().insert(HermesContentProvider.CONTACTS_CONTENT_URI, values);

                            try {
                                Contact insertedContact = new Contact(insertedContactUri);
                                Log.d(TAG, "contact added: " + insertedContact);
                            } catch (ModelException e) {
                                Log.e(TAG, "unable to load saved contact", e);
                                error = true;
                            }

                        } else {
                            error = true;
                        }
                        if (error) {
                            Toast.makeText(getActivity(), "Unable to save contact", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                alertBuilder.setNegativeButton(R.string.label_getname_dialag_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getActivity(), "Scan Cancelled", Toast.LENGTH_LONG).show();
                    }
                });

                alertBuilder.show();
            }
        }
    }

    private void displayContactQrCode() {
        Intent intent = new Intent(getActivity(), QrDisplayActivity.class);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG, "onCreateLoader()");

        return new CursorLoader(
                getActivity(),
                HermesContentProvider.CONTACTS_CONTENT_URI,
                DBHelper.CONTACT_ALL_COLUMNS,
                null,
                null,
                DBHelper.CONTACT_COLUMN_NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished()");
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG, "onLoaderReset()");
        adapter.swapCursor(null);
    }

    private void fillData() {
        String[] from = new String[] {
                DBHelper.CONTACT_COLUMN_NAME
        };

        int[] to = new int[] {
                R.id.contact_name
        };

        //getLoaderManager().enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);

        adapter = new ContactCursorAdapter(getActivity(), R.layout.list_item_contact, null, from, to, 0);

        setListAdapter(adapter);
    }

    private static class ContactCursorAdapter extends SimpleCursorAdapter {
        private static final String TAG = "ContactCursorAdapter";
        private Context context;

        public ContactCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            try {
                // here we set a convenience data object on the view for easy access to data about this Contact item
                view.setTag(R.id.contact_view_tag_id, Contact.cursorToContact(getCursor()));
            } catch (ModelException e) {
                Log.e(TAG, "error converting cursor to Contact", e);
                Toast.makeText(context, "Unable to retrieve Contact from database", Toast.LENGTH_LONG).show();
            }

            return view;
        }
    }
}
