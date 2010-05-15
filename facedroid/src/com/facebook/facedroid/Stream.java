package com.facebook.facedroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

public class Stream extends Controller {

	Facebook fb;
	
	public Stream(WebUI webui, Facebook fb) {
		super(webui);
		this.fb = fb;
	}
	
	public void render() {
		fb.request("me/home", new StremRequestListener());
		//renderResult(renderStream(readFromFile()));
	}
	public void renderResult(String html) {
		this.webui.loadData(html);
	}
	
	private JSONObject readFromFile() {
		try {
			FileInputStream is = getActivity().openFileInput("response.txt");
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			while (r.ready()) {
				String line = r.readLine();
				sb.append(line);
			}
			String response = sb.toString();
			JSONObject obj = new JSONObject(response);
			return obj;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void onUrl(String url) {
		Log.d("facedroid fetch", url);
	}
	
	public class StremRequestListener extends RequestListener {

        @Override
        public void onRequestSucceed(final JSONObject response) {
        	writeToFile(response.toString());
        	final String html = renderStream(response);
            Stream.this.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                	Stream.this.renderResult(html);
                }
            });
        }

        private void writeToFile(String response) {
        	try {
    			FileOutputStream fo = getActivity().openFileOutput("response.txt", 0);
    			BufferedWriter bf = new BufferedWriter(new FileWriter(fo.getFD()));
    			bf.write(response);
    			bf.flush();
    			bf.close();
    		} catch (FileNotFoundException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        }
        @Override
        public void onRequestFail(String error) {
            Log.d("SDK-DEBUG", "Request failed: " + error.toString());
        }
    }
	
	private void appendAll(StringBuilder sb, String[] chunks) {
		for (String chunk : chunks) {
			sb.append(chunk);
		}
	}
	private String renderStream(JSONObject response) {
		
		try {
			JSONArray posts = response.getJSONArray("data");
			StringBuilder sb = new StringBuilder();			
			String[] chunks = {
				"<html><head>",
				"<link rel=\"stylesheet\" href=\"file:///android_asset/stream.css\" type=\"text/css\">",
				"</head><body>"
			};
			appendAll(sb, chunks);
			for (int i = 6; i < posts.length(); i++) {
				renderPost(posts.getJSONObject(i), sb);
			}
			sb.append("</body></html>");
			return sb.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	private void renderProfileLink(StringBuilder sb, String id, String name) {
		String [] chunks =
			{"<a href=\"fb://",	id, "\">", name, "</a>"};
		appendAll(sb, chunks);
	}

	private void renderAuthor(StringBuilder sb, String id, String name) {
		String[] chunks = {
		"<div class=\"profile_pic\">",
		"<a href=\"fb://" + id + "\"><img src=\"http://graph.facebook.com/" + id + "/picture\" width=\"30\" height=\"30\"/></a>",
	    "</div>"
		};
		appendAll(sb, chunks);
		renderProfileLink(sb, id, name);
	}
		
	private void renderPost(JSONObject post, StringBuilder sb) throws JSONException {
		JSONObject from = post.getJSONObject("from");
		String fromName = from.getString("name");
		String fromId = from.getString("id");
		sb.append("<div class=\"post\">");
		renderAuthor(sb, fromId, fromName);

		JSONObject to = post.optJSONObject("to");
		if (to != null) {
			JSONObject toData = to.getJSONArray("data").getJSONObject(0);
			String toName = toData.getString("name");
			String toId = toData.getString("id");
			sb.append(" > ");
			renderProfileLink(sb, toId, toName);
		}
		String message = post.optString("message");
		String[] chunks = {
				"<div class=\"msg\">" + message + "</div>",
				"<div class=\"clear\"></div>"};
		appendAll(sb, chunks);

		HashSet<String> actions = getActions(post);
		boolean hasActionLinks = actions.contains("Comment") || actions.contains("Like");
		if (hasActionLinks) {
			sb.append("<div class=\"action_links\">");
		}
		if (actions.contains("Comment")) {
			sb.append("<a class=\"action_link\" href=\"\">comment</a>");
		}
		if (actions.contains("Like")) {
			sb.append("<a class=\"action_link\" href=\"\">like</a>");
		}
		if (hasActionLinks) {
			sb.append("<div class=\"clear\"></div></div>");
		}

		int numLikes = post.optInt("likes", 0);
		JSONObject comments = post.optJSONObject("comments");
		if (comments != null) {
			sb.append("<div class=\"comments\">");
			JSONArray data = comments.optJSONArray("data");
			for (int j = 0; j < data.length(); j++) {
				JSONObject comment = data.getJSONObject(j);
				renderComment(sb, comment);
			}
			sb.append("</div>");
		}
		sb.append("</div>");
	}

	private HashSet<String> getActions(JSONObject post) {
		HashSet<String> actionsSet = new HashSet<String>();
		JSONArray actions = post.optJSONArray("actions");
		if (actions != null) {
			for (int j = 0; j < actions.length(); j++) {
				JSONObject action = actions.optJSONObject(j);
				String actionName = action.optString("name");
				actionsSet.add(actionName);
			}
		}
		return actionsSet;
	}

	private void renderComment(StringBuilder sb, JSONObject comment) throws JSONException {
		JSONObject from = comment.getJSONObject("from");
		String authorName = from.getString("name");
		String authorId = from.getString("id");
		String message = comment.getString("message");
		sb.append("<div class=\"comment\">");
		renderAuthor(sb, authorId, authorName);
		String[] chunks = {
				  "<div class=\"comment_body\">", message, "</div>",
				  "<div class=\"clear\"></div>",
				"</div>"};	
		appendAll(sb, chunks);
	}
}
