package com.alangeorge.android.hermes;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class ContactDetailActivity extends ActionBarActivity {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "Hermes.ContactDetailActivity";
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
}
