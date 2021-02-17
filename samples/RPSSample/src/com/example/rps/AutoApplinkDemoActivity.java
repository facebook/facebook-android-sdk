package com.example.rps;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

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

    String productID = getIntent().getStringExtra("product_id");
    if (productID != null) {
      coffeeTitleView.setText(coffeeName + productID);
      coffeeDescView.setText(coffeeDesc + productID);
    }
    coffeeDataView.setText(getIntent().getExtras().toString());
  }
}
