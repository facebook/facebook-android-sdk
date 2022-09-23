/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.entities;

public class Post {
  private final String message;
  private final String created_time;
  private final String id;
  private final String picture;
  private final String from_name;
  private final String from_id;

  public Post(
      String message,
      String created_time,
      String id,
      String picture,
      String from_name,
      String from_id) {
    this.message = message;
    this.created_time = created_time;
    this.id = id;
    this.picture = picture;
    this.from_name = from_name;
    this.from_id = from_id;
  }

  public String getMessage() {
    return message;
  }

  public String getCreatedTime() {
    return created_time;
  }

  public String getId() {
    return id;
  }

  public String getPicture() {
    return picture;
  }

  public String getFromName() {
    return from_name;
  }

  public String getFromId() {
    return from_id;
  }
}
