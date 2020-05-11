/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
