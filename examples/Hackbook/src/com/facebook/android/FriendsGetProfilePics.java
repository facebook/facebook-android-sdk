package com.facebook.android;

import java.util.Hashtable;
import java.util.Stack;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.BaseAdapter;

/*
 * Fetch friends profile pictures request via AsyncTask
 */
public class FriendsGetProfilePics {

    Hashtable<String, Bitmap> friendsImages;
    Hashtable<String, String> positionRequested;
    BaseAdapter listener;
    int runningCount = 0;
    Stack<ItemPair> queue;

    /*
     * 15 max async tasks at any given time.
     */
    final static int MAX_ALLOWED_TASKS = 15;

    public FriendsGetProfilePics() {
        friendsImages = new Hashtable<String, Bitmap>();
        positionRequested = new Hashtable<String, String>();
        queue = new Stack<ItemPair>();
    }

    /*
     * Inform the listener when the image has been downloaded. listener is
     * FriendsList here.
     */
    public void setListener(BaseAdapter listener) {
        this.listener = listener;
        reset();
    }

    public void reset() {
        positionRequested.clear();
        runningCount = 0;
        queue.clear();
    }

    /*
     * If the profile picture has already been downloaded and cached, return it
     * else execute a new async task to fetch it - if total async tasks >15,
     * queue the request.
     */
    public Bitmap getImage(String uid, String url) {
        Bitmap image = friendsImages.get(uid);
        if (image != null) {
            return image;
        }
        if (!positionRequested.containsKey(uid)) {
            positionRequested.put(uid, "");
            if (runningCount >= MAX_ALLOWED_TASKS) {
                queue.push(new ItemPair(uid, url));
            } else {
                runningCount++;
                new GetProfilePicAsyncTask().execute(uid, url);
            }
        }
        return null;
    }

    public void getNextImage() {
        if (!queue.isEmpty()) {
            ItemPair item = queue.pop();
            new GetProfilePicAsyncTask().execute(item.uid, item.url);
        }
    }

    /*
     * Start a AsyncTask to fetch the request
     */
    private class GetProfilePicAsyncTask extends AsyncTask<Object, Void, Bitmap> {
        String uid;

        @Override
        protected Bitmap doInBackground(Object... params) {
            this.uid = (String) params[0];
            String url = (String) params[1];
            return Utility.getBitmap(url);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            runningCount--;
            if (result != null) {
                friendsImages.put(uid, result);
                listener.notifyDataSetChanged();
                getNextImage();
            }
        }
    }

    class ItemPair {
        String uid;
        String url;

        public ItemPair(String uid, String url) {
            this.uid = uid;
            this.url = url;
        }
    }

}
