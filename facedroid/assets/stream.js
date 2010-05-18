function $(id) {
  return document.getElementById(id);
}

function show(id) {
  $(id).style.display = "block";
}

function hide(id) {
  $(id).style.display = "none";
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
  hide("comment_box" + post_id);
  var text = $("comment_body_input" + post_id).value;
  app.postComment(post_id, text);
}