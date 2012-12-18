/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android;

import android.app.Dialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TabHost.TabSpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class FieldsConnectionsDialog extends Dialog {

    private final static int TAB_HEIGHT = 50;

    private Button mGetFieldsButton;
    private ListView fieldsList, connectionsList;
    private BaseAdapter fieldsAdapter, connectionsAdapter;

    private GraphExplorer explorerActivity;

    protected Vector<String> fieldsVector;
    private ArrayList<JSONObject> fieldsArray;
    private ArrayList<String> connectionsArray;

    public FieldsConnectionsDialog(GraphExplorer explorerActivity, JSONObject metadata) {
        super(explorerActivity);
        this.explorerActivity = explorerActivity;

        /*
         * Sort the fields and connections
         */
        try {
            sortFields(metadata.getJSONArray("fields"));
            sortConnections(metadata.getJSONObject("connections").names());
        } catch (JSONException e) {
            Toast.makeText(explorerActivity.getBaseContext(),
                    "Fields/Connections could not be fetched.", Toast.LENGTH_SHORT).show();
        }

        setTitle(explorerActivity.getString(R.string.fields_and_connections));
        fieldsVector = new Vector<String>();
    }

    /*
     * Sort fields which are returned as JSONObject in the JSONArray
     */
    public void sortFields(JSONArray jsonFieldsArray) {
        this.fieldsArray = new ArrayList<JSONObject>(jsonFieldsArray.length());
        for (int i = 0; i < jsonFieldsArray.length(); i++) {
            try {
                this.fieldsArray.add(jsonFieldsArray.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(this.fieldsArray, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject object1, JSONObject object2) {
                try {
                    return object1.getString("name").compareToIgnoreCase(object2.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }

    /*
     * Sort the Connections returned in the JSONArray
     */
    public void sortConnections(JSONArray jsonConnectionsArray) {
        this.connectionsArray = new ArrayList<String>(jsonConnectionsArray.length());
        for (int i = 0; i < jsonConnectionsArray.length(); i++) {
            try {
                this.connectionsArray.add(jsonConnectionsArray.get(i).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(this.connectionsArray);
    }

    /*
     * Layout the dialog
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fields_connections_list);
        LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.FILL_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        fieldsList = (ListView) findViewById(R.id.fields_list);
        connectionsList = (ListView) findViewById(R.id.connections_list);

        fieldsAdapter = new FieldsListAdapter();
        if (this.fieldsArray == null) {
            fieldsList.setAdapter(new ArrayAdapter<String>(explorerActivity,
                    android.R.layout.simple_list_item_1, new String[] { "No fields available" }));
        } else {
            fieldsList.setAdapter(fieldsAdapter);
        }

        connectionsAdapter = new ConnectionsListAdapter();
        if (this.connectionsArray == null) {
            connectionsList.setAdapter(new ArrayAdapter<String>(explorerActivity,
                    android.R.layout.simple_list_item_1,
                    new String[] { "No connections available" }));
        } else {
            connectionsList.setAdapter(connectionsAdapter);
        }

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabSpec spec1 = tabHost.newTabSpec("Tab 1");
        spec1.setIndicator(explorerActivity.getString(R.string.fields));
        spec1.setContent(R.id.fields_layout);

        TabSpec spec2 = tabHost.newTabSpec("Tab 2");
        spec2.setIndicator(explorerActivity.getString(R.string.connections));
        spec2.setContent(R.id.connections_list);

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.setCurrentTab(0);
        tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = TAB_HEIGHT;
        tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = TAB_HEIGHT;

        mGetFieldsButton = (Button) findViewById(R.id.get_fields_button);
        mGetFieldsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Source Tag:
                 */
                FieldsConnectionsDialog.this.dismiss();
                if (!fieldsVector.isEmpty()) {
                    explorerActivity.getFields(fieldsVector);
                } else {
                    Toast.makeText(explorerActivity.getBaseContext(), "No Fields selected.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        connectionsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                FieldsConnectionsDialog.this.dismiss();
                explorerActivity.getConnection(connectionsArray.get(position));
            }
        });
    }

    /**
     * Definition of the list adapter
     */
    public class FieldsListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        boolean[] isChecked;

        public FieldsListAdapter() {
            mInflater = LayoutInflater.from(explorerActivity.getBaseContext());
            isChecked = new boolean[fieldsArray.size()];
        }

        @Override
        public int getCount() {
            return fieldsArray.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View hView = convertView;
            ViewHolder holder;
            JSONObject fieldObject = null;
            fieldObject = fieldsArray.get(position);

            if (hView == null) {
                hView = mInflater.inflate(R.layout.fields_item, null);
                holder = new ViewHolder();
                holder.checkbox = (CheckBox) hView.findViewById(R.id.fields_checkbox);
                holder.fieldsInfo = (TextView) hView.findViewById(R.id.fields_info);
                hView.setTag(holder);
            } else {
                holder = (ViewHolder) hView.getTag();
            }
            try {
                holder.checkbox.setText(fieldObject.getString("name"));
            } catch (JSONException e) {
                holder.checkbox.setText("");
            }
            try {
                holder.fieldsInfo.setText(fieldObject.getString("description"));
            } catch (JSONException e) {
                holder.fieldsInfo.setText("");
            }
            holder.checkbox.setId(position);
            holder.checkbox.setChecked(isChecked[position]);
            holder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    isChecked[button.getId()] = checked;
                    String field = button.getText().toString();
                    if (checked) {
                        fieldsVector.add(field);
                    } else if (fieldsVector.contains(field)) {
                        fieldsVector.remove(field);
                    }
                }
            });

            return hView;
        }
    }

    class ViewHolder {
        CheckBox checkbox;
        TextView fieldsInfo;
    }

    /**
     * Definition of the list adapter
     */
    public class ConnectionsListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public ConnectionsListAdapter() {
            mInflater = LayoutInflater.from(explorerActivity.getBaseContext());
        }

        @Override
        public int getCount() {
            return connectionsArray.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View hView = convertView;
            TextView connection;
            if (hView == null) {
                hView = mInflater.inflate(R.layout.connection_item, null);
                connection = (TextView) hView.findViewById(R.id.connection_name);
                hView.setTag(connection);
            } else {
                connection = (TextView) hView.getTag();
            }
            SpannableString name;
            name = new SpannableString(connectionsArray.get(position));
            name.setSpan(new UnderlineSpan(), 0, name.length(), 0);
            connection.setText(name);
            return hView;
        }
    }
}
