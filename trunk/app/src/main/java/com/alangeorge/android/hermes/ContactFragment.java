package com.alangeorge.android.hermes;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.google.zxing.integration.android.IntentIntegrator;

import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_ALL_COLUMNS;
import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_NAME;
import static com.alangeorge.android.hermes.model.provider.HermesContentProvider.CONTACTS_CONTENT_URI;

public class ContactFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ContactFragment";

    private SimpleCursorAdapter adapter;

    public ContactFragment() { }

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_contact_add:
                IntentIntegrator integrator = new IntentIntegrator(getActivity());
                integrator.initiateScan();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG, "onCreateLoader()");

        return new CursorLoader(
                getActivity(),
                CONTACTS_CONTENT_URI,
                CONTACT_ALL_COLUMNS,
                null,
                null,
                CONTACT_COLUMN_NAME);
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
                CONTACT_COLUMN_NAME
        };

        int[] to = new int[] {
                R.id.contactNameTextView
        };

        //getLoaderManager().enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);

        adapter = new SimpleCursorAdapter(getActivity(), R.layout.list_item_contact, null, from, to, 0);

        setListAdapter(adapter);

    }
}
