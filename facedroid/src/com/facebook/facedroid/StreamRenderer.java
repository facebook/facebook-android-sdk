package com.facebook.facedroid;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Contains logic for rendering the stream.
 * 
 * @author yariv
 */
class StreamRenderer {
	
	private JSONObject data;
	private StringBuilder sb;
	
	public static String render(JSONObject data) {
		StreamRenderer renderer = new StreamRenderer(data);
		return renderer.render();
	}
	
	private StreamRenderer(JSONObject data) {
		this.data = data;
		this.sb = new StringBuilder();
	}
	
	private String render() {
		
		try {
			JSONArray posts = data.getJSONArray("data");
			String[] chunks = {
				"<html><head>",
				"<link rel=\"stylesheet\" href=\"file:///android_asset/stream.css\" type=\"text/css\">",
				"</head><body>"
			};
			append(chunks);
			for (int i = 6; i < posts.length(); i++) {
				renderPost(posts.getJSONObject(i));
			}
			append("</body></html>");
			return sb.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	private void renderPost(JSONObject post) throws JSONException {
		append("<div class=\"post\">");
		renderFrom(post);
		renderTo(post);
		//sb.append();
		renderMessage(post);
		renderActionLinks(post);
		renderLikes(post);
		renderComments(post);
		append("</div>");
	}
	

	private void renderFrom(JSONObject post) throws JSONException {
		JSONObject from = post.getJSONObject("from");
		String fromName = from.getString("name");
		String fromId = from.getString("id");
		renderAuthor(fromId, fromName);
	}
	
	private void renderTo(JSONObject post) throws JSONException {
		JSONObject to = post.optJSONObject("to");
		if (to != null) {
			JSONObject toData = to.getJSONArray("data").getJSONObject(0);
			String toName = toData.getString("name");
			String toId = toData.getString("id");
			append(" > ");
			renderProfileLink(toId, toName);
		}
	}
	
	private void renderProfileLink(String id, String name) {
		String [] chunks =
			{"<a href=\"fb://",	id, "\">", name, "</a>"};
		append(chunks);
	}

	private void renderAuthor(String id, String name) {
		String[] chunks = {
		"<div class=\"profile_pic_container\">",
		"<a href=\"fb://", id,
		"\"><img class=\"profile_pic\" src=\"http://graph.facebook.com/",
		id, "/picture\"/></a>",
	    "</div>"
		};
		append(chunks);
		renderProfileLink(id, name);
	}
	
	private void renderMessage(JSONObject post) {
		String message = post.optString("message");
		String[] chunks = {
				"&nbsp;<span class=\"msg\">", message, "</span>",
				"<div class=\"clear\"></div>"};
		append(chunks);

	}
	

	private void renderActionLinks(JSONObject post) {
		HashSet<String> actions = getActions(post);
		append("<div class=\"action_links\">");
		append("<div class=\"action_link\">");
		renderTimeStamp(post);
		append("</div>");
		if (actions.contains("Comment")) {
			append("<div class=\"action_link\"><a href=\"\">comment</a></div>");
		}
		if (actions.contains("Like")) {
			append("<div class=\"action_link\"><a href=\"\">like</a></div>");
		}
		append("<div class=\"clear\"></div></div>");
	}
	
	private void renderTimeStamp(JSONObject post) {
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
		append(chunks);
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
	
	private void renderLikes(JSONObject post) {
		int numLikes = post.optInt("likes", 0);
		if (numLikes > 0) {
			String desc = numLikes == 1 ?
						"person likes this" :
						"people like this";
			String[] chunks = new String[] {
				"<div class=\"like_icon\">",
				"<img src=\"file:///android_asset/like_icon.png\"/>",
				"</div>",
				"<div class=\"num_likes\">",
				new Integer(numLikes).toString(),
				" ",
				desc,
				"</div>"
			};
			append(chunks);
		}
	}
	
	private void renderComments(JSONObject post) throws JSONException {
		JSONObject comments = post.optJSONObject("comments");
		if (comments != null) {
			append("<div class=\"comments\">");
			JSONArray data = comments.optJSONArray("data");
			for (int j = 0; j < data.length(); j++) {
				JSONObject comment = data.getJSONObject(j);
				renderComment(comment);
			}
			append("</div>");
		}

	}

	private void renderComment(JSONObject comment) throws JSONException {
		JSONObject from = comment.getJSONObject("from");
		String authorName = from.getString("name");
		String authorId = from.getString("id");
		String message = comment.getString("message");
		append("<div class=\"comment\">");
		renderAuthor(authorId, authorName);
		String[] chunks = {
				"&nbsp;",
				  message,
				  "</div>"
				  };	
		append(chunks);
	}
	
	private void append(String str) {
		sb.append(str);
	}
	private void append(String[] chunks) {
		for (String chunk : chunks) {
			sb.append(chunk);
		}
	}
}