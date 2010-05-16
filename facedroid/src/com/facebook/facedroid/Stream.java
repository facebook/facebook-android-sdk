package com.facebook.facedroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

public class Stream extends Controller {

	Facebook fb;
	private static final String CACHE_FILE = "cache.txt";
	
	public Stream(WebUI webui, Facebook fb) {
		super(webui);
		this.fb = fb;
	}
	
	public void render() {
		// first load the cached result
		JSONObject result = readCache();
		if (result != null) {
			renderResult(renderStream(result));
		}
		
		// TODO figure out why the cached result isn't rendered immediately
		// if the following line is executed.
		fb.request("me/home", new StremRequestListener());
	}
	public void renderResult(String html) {
		this.webui.loadData(html);
	}
	
	private JSONObject readCache() {
		try {
			FileInputStream is = getActivity().openFileInput(CACHE_FILE);
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			while (r.ready()) {
				String line = r.readLine();
				sb.append(line);
			}
			String cache = sb.toString();
			JSONObject obj = new JSONObject(cache);
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			// ignore exceptions
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
    			FileOutputStream fo = getActivity().openFileOutput(CACHE_FILE, 0);
    			BufferedWriter bf = new BufferedWriter(new FileWriter(fo.getFD()));
    			bf.write(response);
    			bf.flush();
    			bf.close();
    		} catch (Exception e) {
    			// ignore exceptions
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
				renderPost(sb, posts.getJSONObject(i));
			}
			sb.append("</body></html>");
			return sb.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	private void renderPost(StringBuilder sb, JSONObject post) throws JSONException {
		sb.append("<div class=\"post\">");
		renderFrom(sb, post);
		renderTo(sb, post);
		//sb.append();
		renderMessage(sb, post);
		renderActionLinks(sb, post);
		renderLikes(sb, post);
		renderComments(sb, post);
		sb.append("</div>");
	}
	

	private void renderFrom(StringBuilder sb, JSONObject post) throws JSONException {
		JSONObject from = post.getJSONObject("from");
		String fromName = from.getString("name");
		String fromId = from.getString("id");
		renderAuthor(sb, fromId, fromName);
	}
	
	private void renderTo(StringBuilder sb, JSONObject post) throws JSONException {
		JSONObject to = post.optJSONObject("to");
		if (to != null) {
			JSONObject toData = to.getJSONArray("data").getJSONObject(0);
			String toName = toData.getString("name");
			String toId = toData.getString("id");
			sb.append(" > ");
			renderProfileLink(sb, toId, toName);
		}
	}
	
	private void renderProfileLink(StringBuilder sb, String id, String name) {
		String [] chunks =
			{"<a href=\"fb://",	id, "\">", name, "</a>"};
		appendAll(sb, chunks);
	}

	private void renderAuthor(StringBuilder sb, String id, String name) {
		String[] chunks = {
		"<div class=\"profile_pic_container\">",
		"<a href=\"fb://", id,
		"\"><img class=\"profile_pic\" src=\"http://graph.facebook.com/",
		id, "/picture\"/></a>",
	    "</div>"
		};
		appendAll(sb, chunks);
		renderProfileLink(sb, id, name);
	}
	
	private void renderMessage(StringBuilder sb, JSONObject post) {
		String message = post.optString("message");
		String[] chunks = {
				"&nbsp;<span class=\"msg\">", message, "</span>",
				"<div class=\"clear\"></div>"};
		appendAll(sb, chunks);

	}
	

	private void renderActionLinks(StringBuilder sb, JSONObject post) {
		HashSet<String> actions = getActions(post);
		sb.append("<div class=\"action_links\">");
		sb.append("<div class=\"action_link\">");
		renderTimeStamp(sb, post);
		sb.append("</div>");
		if (actions.contains("Comment")) {
			sb.append("<div class=\"action_link\"><a href=\"\">comment</a></div>");
		}
		if (actions.contains("Like")) {
			sb.append("<div class=\"action_link\"><a href=\"\">like</a></div>");
		}
		sb.append("<div class=\"clear\"></div></div>");
	}
	
	private void renderTimeStamp(StringBuilder sb, JSONObject post) {
		String dateStr = post.optString("created_time");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
		ParsePosition pos = new ParsePosition(0);
		long then  = formatter.parse(dateStr, pos).getTime();
		long now = new Date().getTime();
		
		long seconds = (now - then)/1000;
		long minutes = seconds/60;
		long hours = minutes/60;
		long days = hours/24;

		String friendly = null;
		long num = 0;
		if (days > 0) {
			num = days;
			friendly = days + " day";
		} else if (hours > 0) {
			num = hours;
			friendly = hours + " hour";
		} else if (minutes > 0) {
			num = minutes;
			friendly = minutes + " minute";
		} else {
			num = seconds;
			friendly = seconds + " second";
		}
		if (num > 1) {
			friendly += "s";
		}
		String[] chunks = new String[] {
			"<div class=\"timestamp\">",
			friendly,
			" ago",
			"</div>"
		};
		appendAll(sb, chunks);
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
	private void renderLikes(StringBuilder sb, JSONObject post) {
		int numLikes = post.optInt("likes", 0);
		if (numLikes > 0) {
			String people = numLikes == 1 ? "person" : "people";
			String[] chunks = new String[] {
				"<div class=\"like_icon\">",
				"<img src=\"file:///android_asset/like_icon.png\"/>",
				"</div>",
				"<div class=\"num_likes\">",
				new Integer(numLikes).toString(),
				" ",
				people,
				" like this</div>"
			};
			appendAll(sb, chunks);
		}
	}
	
	private void renderComments(StringBuilder sb, JSONObject post) throws JSONException {
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
