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
    private static final int DEFAULT_SIZE_INCREMENT = MAX_CUSTOM_SIZES / 2;
    private static final String PICTURE_SIZE_TYPE_KEY = "PictureSizeType";

    private int pictureSizeType = ProfilePictureView.CUSTOM;
    private String firstUserId;

    private ProfilePictureView profilePic;
    private Button smallerButton;
    private Button largerButton;
    private TextView sizeLabel;
    private View presetSizeView;
    private SeekBar customSizeView;
    private CheckBox cropToggle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile_picture_sample, parent, false);

        profilePic = (ProfilePictureView)v.findViewById(R.id.profilepic);
        smallerButton = (Button)v.findViewById(R.id.smallerButton);
        largerButton = (Button)v.findViewById(R.id.largerButton);
        sizeLabel = (TextView)v.findViewById(R.id.sizeLabel);
        presetSizeView = v.findViewById(R.id.presetSizeView);
        customSizeView = (SeekBar)v.findViewById(R.id.customSizeView);
        cropToggle = (CheckBox)v.findViewById(R.id.squareCropToggle);

        LinearLayout container = (LinearLayout)v.findViewById(R.id.userbuttoncontainer);
        int numChildren = container.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            View childView = container.getChildAt(i);
            Object tag = childView.getTag();
            if (tag != null && childView instanceof Button) {
                setupUserButton((Button)childView);
                if (i == 0) {
                    // Initialize the image to the first user
                    firstUserId = tag.toString();
                }
            }
        }

        cropToggle.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton checkbox, boolean checked) {
                profilePic.setCropped(checked);
            }
        });

        final Button sizeToggle = (Button)v.findViewById(R.id.sizeToggle);
        sizeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pictureSizeType != ProfilePictureView.CUSTOM) {
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

        restoreState(savedInstanceState);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Store the size type since we will use that to switch the Fragment's UI
        // between CUSTOM & PRESET modes
        // Other state (userId & isCropped) will be saved/restored directly by
        // ProfilePictureView
        outState.putInt(PICTURE_SIZE_TYPE_KEY, pictureSizeType);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Is we have saved state, restore the Fragment to it.
            // UserId & isCropped will be restored directly by ProfilePictureView
            pictureSizeType = savedInstanceState.getInt(
                    PICTURE_SIZE_TYPE_KEY, ProfilePictureView.LARGE);

            if (pictureSizeType == ProfilePictureView.CUSTOM) {
                switchToCustomSize();
            } else {
                switchToPresetSize(pictureSizeType);
            }
        } else {
            // No saved state. Let's go to a default state
            switchToPresetSize(ProfilePictureView.LARGE);
            profilePic.setCropped(cropToggle.isChecked());

            // Setting userId last so that only one network request is sent
            profilePic.setUserId(firstUserId);
        }
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
        pictureSizeType = ProfilePictureView.CUSTOM;
        presetSizeView.setVisibility(View.GONE);
        customSizeView.setVisibility(View.VISIBLE);

        profilePic.setPresetSize(pictureSizeType);

        customSizeView.setProgress(DEFAULT_SIZE_INCREMENT);
        updateProfilePicForCustomSizeIncrement(DEFAULT_SIZE_INCREMENT);
    }

    private void switchToPresetSize(int sizeType) {
        customSizeView.setVisibility(View.GONE);
        presetSizeView.setVisibility(View.VISIBLE);

        switch(sizeType) {
            case ProfilePictureView.SMALL:
                largerButton.setEnabled(true);
                smallerButton.setEnabled(false);
                sizeLabel.setText(R.string.small_image_size);
                pictureSizeType = sizeType;
                break;
            case ProfilePictureView.NORMAL:
                largerButton.setEnabled(true);
                smallerButton.setEnabled(true);
                sizeLabel.setText(R.string.normal_image_size);
                pictureSizeType = sizeType;
                break;
            case ProfilePictureView.LARGE:
            default:
                largerButton.setEnabled(false);
                smallerButton.setEnabled(true);
                sizeLabel.setText(R.string.large_image_size);
                pictureSizeType = ProfilePictureView.LARGE;
                break;
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );

        profilePic.setLayoutParams(params);
        profilePic.setPresetSize(pictureSizeType);
    }

    private void updateProfilePicForCustomSizeIncrement(int i) {
        if (pictureSizeType != ProfilePictureView.CUSTOM) {
            return;
        }

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
