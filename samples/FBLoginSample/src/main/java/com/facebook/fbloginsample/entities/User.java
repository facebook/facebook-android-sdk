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

import android.net.Uri;

public class User {
  private final Uri picture;
  private final String name;
  private final String id;
  private final String email;
  private final String permissions;

  public User(Uri picture, String name, String id, String email, String permissions) {
    this.picture = picture;
    this.name = name;
    this.id = id;
    this.email = email;
    this.permissions = permissions;
  }

  public Uri getPicture() {
    return picture;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPermissions() {
    return permissions;
  }
}
