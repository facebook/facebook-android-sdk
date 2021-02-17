/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.example.rps;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import org.json.JSONException;
import org.json.JSONObject;

public class AutoApplinkFragment extends Fragment {

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_auto_applink, container, false);
    final EditText viewAppID = (EditText) view.findViewById(R.id.auto_applink_app_id);
    final EditText viewProductID = (EditText) view.findViewById(R.id.auto_applink_product_id);
    Button sendButton = (Button) view.findViewById(R.id.auto_applink_button);

    sendButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            String appID = viewAppID.getText().toString();
            String productID = viewProductID.getText().toString();
            if (appID.length() == 0) {
              showAlert("Invalid App ID!");
            } else if (productID.length() == 0) {
              showAlert("Invalid Product ID!");
            } else {
              String autoAppLink = "fb" + appID + "://applinks?al_applink_data=";
              JSONObject data = new JSONObject();
              try {
                data.put("product_id", productID);
                data.put("is_auto_applink", true);
              } catch (JSONException e) {
                showAlert("Cannot generate auto applink url!");
              }
              String dataString = data.toString();
              autoAppLink = autoAppLink + Uri.encode(dataString);
              Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(autoAppLink));
              startActivity(intent);
            }
          }
        });

    return view;
  }

  private void showAlert(String message) {
    new AlertDialog.Builder(getActivity())
        .setTitle("Error")
        .setMessage(message)
        .setPositiveButton("Close", null)
        .show();
  }
}
