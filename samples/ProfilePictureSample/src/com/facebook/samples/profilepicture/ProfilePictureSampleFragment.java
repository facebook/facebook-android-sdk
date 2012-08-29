package com.facebook.samples.profilepicture;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.ProfilePictureView;

public class ProfilePictureSampleFragment extends Fragment {

    // Keeping the number of custom sizes low to prevent excessive network chatter.
    private static final int MAX_CUSTOM_SIZES = 6;

    boolean showingPresetSize = true;

    private ProfilePictureView profilePic;
    private Button smallerButton;
    private Button largerButton;
    private TextView sizeLabel;
    private View presetSizeView;
    private SeekBar customSizeView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile_picture_sample, parent, false);

        profilePic = (ProfilePictureView)v.findViewById(R.id.profilepic);
        smallerButton = (Button)v.findViewById(R.id.smallerButton);
        largerButton = (Button)v.findViewById(R.id.largerButton);
        sizeLabel = (TextView)v.findViewById(R.id.sizeLabel);
        presetSizeView = v.findViewById(R.id.presetSizeView);
        customSizeView = (SeekBar)v.findViewById(R.id.customSizeView);

        LinearLayout container = (LinearLayout)v.findViewById(R.id.userbuttoncontainer);
        int numChildren = container.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            View childView = container.getChildAt(i);
            Object tag = childView.getTag();
            if (tag != null && childView instanceof Button) {
                setupUserButton((Button)childView);
                if (i == 0) {
                    // Initialize the image to the first user
                    profilePic.setUserId(tag.toString());
                }
            }
        }

        CheckBox cropToggle = (CheckBox)v.findViewById(R.id.squareCropToggle);
        cropToggle.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton checkbox, boolean checked) {
                profilePic.setCropped(checked);
            }
        });
        cropToggle.setChecked(true);

        final Button sizeToggle = (Button)v.findViewById(R.id.sizeToggle);
        sizeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (showingPresetSize) {
                    sizeToggle.setText(R.string.preset_size_button_text);
                    switchToCustomSize();
                } else {
                    sizeToggle.setText(R.string.custom_size_button_text);
                    switchToPresetSize(ProfilePictureView.LARGE);
                }
            }
        });

        smallerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(profilePic.getPresetSize()) {
                    case ProfilePictureView.LARGE:
                        switchToPresetSize(ProfilePictureView.NORMAL);
                        break;
                    case ProfilePictureView.NORMAL:
                        switchToPresetSize(ProfilePictureView.SMALL);
                        break;
                }
            }
        });

        largerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(profilePic.getPresetSize()) {
                    case ProfilePictureView.NORMAL:
                        switchToPresetSize(ProfilePictureView.LARGE);
                        break;
                    case ProfilePictureView.SMALL:
                        switchToPresetSize(ProfilePictureView.NORMAL);
                        break;
                }
            }
        });

        // We will fetch a new image for each change in the SeekBar. So keeping the count low
        // to prevent too much network chatter. SeekBar reports 0-max, so we will get max+1
        // notifications of change.
        customSizeView.setMax(MAX_CUSTOM_SIZES);
        customSizeView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateProfilePicForCustomSizeIncrement(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // NO-OP
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // NO-OP
            }
        });

        return v;
    }

    private void setupUserButton(Button b) {
        b.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = v.getTag();
                if (tag != null) {
                    profilePic.setUserId(tag.toString());
                }
            }
        });
    }

    private void switchToCustomSize() {
        showingPresetSize = false;
        presetSizeView.setVisibility(View.GONE);
        customSizeView.setVisibility(View.VISIBLE);

        profilePic.setPresetSize(ProfilePictureView.CUSTOM);

        int sizeIncrement = MAX_CUSTOM_SIZES/2;
        customSizeView.setProgress(sizeIncrement);
        updateProfilePicForCustomSizeIncrement(sizeIncrement);
    }

    private void switchToPresetSize(int sizeType) {
        showingPresetSize = true;
        customSizeView.setVisibility(View.GONE);
        presetSizeView.setVisibility(View.VISIBLE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );

        profilePic.setLayoutParams(params);

        switch(sizeType) {
            case ProfilePictureView.SMALL:
                largerButton.setEnabled(true);
                smallerButton.setEnabled(false);
                sizeLabel.setText(R.string.small_image_size);
                profilePic.setPresetSize(ProfilePictureView.SMALL);
                break;
            case ProfilePictureView.NORMAL:
                largerButton.setEnabled(true);
                smallerButton.setEnabled(true);
                sizeLabel.setText(R.string.normal_image_size);
                profilePic.setPresetSize(ProfilePictureView.NORMAL);
                break;
            case ProfilePictureView.LARGE:
            default:
                largerButton.setEnabled(false);
                smallerButton.setEnabled(true);
                sizeLabel.setText(R.string.large_image_size);
                profilePic.setPresetSize(ProfilePictureView.LARGE);
                break;
        }
    }

    private void updateProfilePicForCustomSizeIncrement(int i) {
        // This will ensure a minimum size of 51x68 and will scale the image at
        // a ratio of 3:4 (w:h) as the SeekBar is moved.
        //
        // Completely arbitrary
        //
        // NOTE: The numbers are in dips.
        float width = (i * 21) + 51;
        float height = (i * 28) + 68;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int)(width * getResources().getDisplayMetrics().density),
                (int)(height * getResources().getDisplayMetrics().density));
        profilePic.setLayoutParams(params);
    }
}
