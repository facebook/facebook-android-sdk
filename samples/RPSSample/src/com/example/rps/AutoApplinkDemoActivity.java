package com.example.rps;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.facebook.applinks.AppLinkData;
import com.facebook.applinks.AppLinks;

import org.json.JSONObject;

public class AutoApplinkDemoActivity extends AppCompatActivity {

    private String coffeeName;
    private String coffeeDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_applink_demo);

        TextView coffeeTitleView = (TextView) findViewById(R.id.auto_applink_demo_title);
        TextView coffeeDescView = (TextView) findViewById(R.id.auto_applink_demo_desc);
        TextView coffeeDataView = (TextView) findViewById(R.id.auto_applink_demo_data);

        coffeeName = coffeeTitleView.getText().toString();
        coffeeDesc = coffeeDescView.getText().toString();

        AppLinkData appLinkData =
                getIntent().getParcelableExtra(AppLinks.AUTO_APPLINK_DATA_KEY);
        if (appLinkData != null) {
            JSONObject json = appLinkData.getAppLinkData();
            if (json != null) {
                String productID = json.optString("product_id");
                coffeeTitleView.setText(coffeeName + productID);
                coffeeDescView.setText(coffeeDesc + productID);
            }
            coffeeDataView.setText(json.toString());
        }
    }
}
