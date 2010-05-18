function $(id) {
  return document.getElementById(id);
}

function show(id) {
  $(id).style.display = "block";
}

function hide(id) {
  $(id).style.display = "none";
}

function onStatusBoxFocus(elt) {
  elt.value = '';
  elt.style.color = "#000";
  show('status_submit');
}

function updateStatus() {
  var message = $('status_input').value;
  if (message == "") {
    return;
  }
  $('status_input').disabled = true;
  $('status_submit').disabled = true;
  app.updateStatus(message);
}

function onStatusUpdated(html) {
  $('status_input').disabled = false;
  $('status_submit').disabled = false;
  $('posts').innerHTML = html + $('posts').innerHTML;
}

function like(post_id) {
  doLike(post_id, true);
 
}

function unlike(post_id) {
  doLike(post_id, false);
}

function doLike(post_id, val) {
  var ids = getLikeLinkIds(post_id, val);
  $(ids[0]).disabled = true;
  app.like(post_id, val);
}

// called when the api request has succeeded
function onLike(post_id, val) {
  var ids = getLikeLinkIds(post_id, val);
  $(ids[0]).disabled = false;
  hide(ids[0]);
  show(ids[1]);
}

function getLikeLinkIds(post_id, val) {
  if (val) {
    var prefix1 = 'like';
    var prefix2 = 'unlike';
  } else {
    var prefix1 = 'unlike';
    var prefix2 = 'like';
  }
  return [prefix1 + post_id, prefix2 + post_id];
}
  

function comment(post_id) {
  show("comment_box" + post_id);
  $("comment_box_input" + post_id).focus();
}

function postComment(post_id) {
  var message = $("comment_box_input" + post_id).value;
  if (message == "") {
    return;
  }
  $("comment_box" + post_id).disabled = true;
  app.postComment(post_id, message);
}

function onComment(post_id, html) {
  $("comments" + post_id).innerHTML += html;
  $("comment_box" + post_id).disabled = false;
  $("comment_box_input" + post_id).value = "";
}