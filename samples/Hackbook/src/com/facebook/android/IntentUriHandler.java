package com.facebook.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * A transient activity which handles fbgraphex: URIs and passes those to the
 * GraphExplorer class This is used to linkify the Object IDs in the graph api
 * response
 */
public class IntentUriHandler extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            Uri intentUri = incomingIntent.getData();
            if (intentUri != null) {
                Utility.objectID = intentUri.getHost();
                Intent graphIntent = new Intent(getApplicationContext(), GraphExplorer.class);
                graphIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(graphIntent);
            }
            finish();
        }
    }
}
