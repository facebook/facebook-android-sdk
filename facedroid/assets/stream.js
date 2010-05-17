function show(id) {
  document.getElementById(id).style.display = "block";
}

function hide(id) {
  document.getElementById(id).style.display = "none";
}

function like(post_id) {
  hide("like" + post_id);
  show("unlike" + post_id);
  app.like(post_id);
  return false;
}

function unlike(post_id) {
  hide("unlike" + post_id);
  show("like" + post_id);
  app.unlike(post_id);
  return false;
}