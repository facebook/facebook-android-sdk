/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object AnalyticsEvents {
  const val EVENT_NATIVE_LOGIN_DIALOG_COMPLETE = "fb_dialogs_native_login_dialog_complete"
  const val EVENT_NATIVE_LOGIN_DIALOG_START = "fb_dialogs_native_login_dialog_start"
  const val EVENT_WEB_LOGIN_COMPLETE = "fb_dialogs_web_login_dialog_complete"
  const val EVENT_FRIEND_PICKER_USAGE = "fb_friend_picker_usage"
  const val EVENT_PLACE_PICKER_USAGE = "fb_place_picker_usage"
  const val EVENT_LOGIN_VIEW_USAGE = "fb_login_view_usage"
  const val EVENT_USER_SETTINGS_USAGE = "fb_user_settings_vc_usage"
  const val EVENT_NATIVE_DIALOG_START = "fb_native_dialog_start"
  const val EVENT_NATIVE_DIALOG_COMPLETE = "fb_native_dialog_complete"
  const val PARAMETER_WEB_LOGIN_E2E = "fb_web_login_e2e"
  const val PARAMETER_WEB_LOGIN_SWITCHBACK_TIME = "fb_web_login_switchback_time"
  const val PARAMETER_APP_ID = "app_id"
  const val PARAMETER_CALL_ID = "call_id"
  const val PARAMETER_ACTION_ID = "action_id"
  const val PARAMETER_NATIVE_LOGIN_DIALOG_START_TIME = "fb_native_login_dialog_start_time"
  const val PARAMETER_NATIVE_LOGIN_DIALOG_COMPLETE_TIME = "fb_native_login_dialog_complete_time"
  const val PARAMETER_DIALOG_OUTCOME = "fb_dialog_outcome"
  const val PARAMETER_DIALOG_OUTCOME_VALUE_COMPLETED = "Completed"
  const val PARAMETER_DIALOG_OUTCOME_VALUE_UNKNOWN = "Unknown"
  const val PARAMETER_DIALOG_OUTCOME_VALUE_CANCELLED = "Cancelled"
  const val PARAMETER_DIALOG_OUTCOME_VALUE_FAILED = "Failed"
  const val EVENT_NATIVE_DIALOG_TYPE_SHARE = "fb_dialogs_present_share"
  const val EVENT_NATIVE_DIALOG_TYPE_MESSAGE = "fb_dialogs_present_message"
  const val EVENT_NATIVE_DIALOG_TYPE_OG_SHARE = "fb_dialogs_present_share_og"
  const val EVENT_NATIVE_DIALOG_TYPE_OG_MESSAGE = "fb_dialogs_present_message_og"
  const val EVENT_NATIVE_DIALOG_TYPE_PHOTO_SHARE = "fb_dialogs_present_share_photo"
  const val EVENT_NATIVE_DIALOG_TYPE_PHOTO_MESSAGE = "fb_dialogs_present_message_photo"
  const val EVENT_NATIVE_DIALOG_TYPE_VIDEO_SHARE = "fb_dialogs_present_share_video"
  const val EVENT_NATIVE_DIALOG_TYPE_LIKE = "fb_dialogs_present_like"
  const val EVENT_LIKE_VIEW_CANNOT_PRESENT_DIALOG = "fb_like_control_cannot_present_dialog"
  const val EVENT_LIKE_VIEW_DID_LIKE = "fb_like_control_did_like"
  const val EVENT_LIKE_VIEW_DID_PRESENT_DIALOG = "fb_like_control_did_present_dialog"
  const val EVENT_LIKE_VIEW_DID_PRESENT_FALLBACK = "fb_like_control_did_present_fallback_dialog"
  const val EVENT_LIKE_VIEW_DID_UNLIKE = "fb_like_control_did_unlike"
  const val EVENT_LIKE_VIEW_DID_UNDO_QUICKLY = "fb_like_control_did_undo_quickly"
  const val EVENT_LIKE_VIEW_DIALOG_DID_SUCCEED = "fb_like_control_dialog_did_succeed"
  const val EVENT_LIKE_VIEW_ERROR = "fb_like_control_error"
  const val PARAMETER_LIKE_VIEW_STYLE = "style"
  const val PARAMETER_LIKE_VIEW_AUXILIARY_POSITION = "auxiliary_position"
  const val PARAMETER_LIKE_VIEW_HORIZONTAL_ALIGNMENT = "horizontal_alignment"
  const val PARAMETER_LIKE_VIEW_OBJECT_ID = "object_id"
  const val PARAMETER_LIKE_VIEW_OBJECT_TYPE = "object_type"
  const val PARAMETER_LIKE_VIEW_CURRENT_ACTION = "current_action"
  const val PARAMETER_LIKE_VIEW_ERROR_JSON = "error"
  const val PARAMETER_SHARE_OUTCOME = "fb_share_dialog_outcome"
  const val PARAMETER_SHARE_OUTCOME_SUCCEEDED = "succeeded"
  const val PARAMETER_SHARE_OUTCOME_CANCELLED = "cancelled"
  const val PARAMETER_SHARE_OUTCOME_ERROR = "error"
  const val PARAMETER_SHARE_OUTCOME_UNKNOWN = "unknown"
  const val PARAMETER_SHARE_ERROR_MESSAGE = "error_message"
  const val PARAMETER_SHARE_DIALOG_SHOW = "fb_share_dialog_show"
  const val PARAMETER_SHARE_DIALOG_SHOW_WEB = "web"
  const val PARAMETER_SHARE_DIALOG_SHOW_NATIVE = "native"
  const val PARAMETER_SHARE_DIALOG_SHOW_AUTOMATIC = "automatic"
  const val PARAMETER_SHARE_DIALOG_SHOW_UNKNOWN = "unknown"
  const val PARAMETER_SHARE_DIALOG_CONTENT_TYPE = "fb_share_dialog_content_type"
  const val PARAMETER_SHARE_DIALOG_CONTENT_UUID = "fb_share_dialog_content_uuid"
  const val PARAMETER_SHARE_DIALOG_CONTENT_PAGE_ID = "fb_share_dialog_content_page_id"
  const val PARAMETER_SHARE_DIALOG_CONTENT_VIDEO = "video"
  const val PARAMETER_SHARE_DIALOG_CONTENT_PHOTO = "photo"
  const val PARAMETER_SHARE_DIALOG_CONTENT_STATUS = "status"
  const val PARAMETER_SHARE_DIALOG_CONTENT_OPENGRAPH = "open_graph"
  const val PARAMETER_SHARE_DIALOG_CONTENT_UNKNOWN = "unknown"
  const val EVENT_SHARE_RESULT = "fb_share_dialog_result"
  const val EVENT_SHARE_DIALOG_SHOW = "fb_share_dialog_show"
  const val EVENT_SHARE_MESSENGER_DIALOG_SHOW = "fb_messenger_share_dialog_show"
  const val EVENT_LIKE_BUTTON_CREATE = "fb_like_button_create"
  const val EVENT_LOGIN_BUTTON_CREATE = "fb_login_button_create"
  const val EVENT_SHARE_BUTTON_CREATE = "fb_share_button_create"
  const val EVENT_SEND_BUTTON_CREATE = "fb_send_button_create"
  const val EVENT_SHARE_BUTTON_DID_TAP = "fb_share_button_did_tap"
  const val EVENT_SEND_BUTTON_DID_TAP = "fb_send_button_did_tap"
  const val EVENT_LIKE_BUTTON_DID_TAP = "fb_like_button_did_tap"
  const val EVENT_LOGIN_BUTTON_DID_TAP = "fb_login_button_did_tap"
  const val EVENT_DEVICE_SHARE_BUTTON_CREATE = "fb_device_share_button_create"
  const val EVENT_DEVICE_SHARE_BUTTON_DID_TAP = "fb_device_share_button_did_tap"
  const val EVENT_SMART_LOGIN_SERVICE = "fb_smart_login_service"
  const val EVENT_SDK_INITIALIZE = "fb_sdk_initialize"
  const val PARAMETER_SHARE_MESSENGER_GENERIC_TEMPLATE = "GenericTemplate"
  const val PARAMETER_SHARE_MESSENGER_MEDIA_TEMPLATE = "MediaTemplate"
  const val PARAMETER_SHARE_MESSENGER_OPEN_GRAPH_MUSIC_TEMPLATE = "OpenGraphMusicTemplate"
  const val EVENT_FOA_LOGIN_BUTTON_CREATE = "foa_login_button_create"
  const val EVENT_FOA_LOGIN_BUTTON_DID_TAP = "foa_login_button_did_tap"
  const val EVENT_FOA_DISAMBIGUATION_DIALOG_FB_DID_TAP = "foa_disambiguation_dialog_fb_did_tap"
  const val EVENT_FOA_DISAMBIGUATION_DIALOG_IG_DID_TAP = "foa_disambiguation_dialog_ig_did_tap"
  const val EVENT_FOA_DISAMBIGUATION_DIALOG_CANCELLED = "foa_disambiguation_dialog_cancelled"
  const val EVENT_FOA_FB_LOGIN_BUTTON_CREATE = "foa_fb_login_button_create"
  const val EVENT_FOA_FB_LOGIN_BUTTON_DID_TAP = "foa_fb_login_button_did_tap"
  const val EVENT_FOA_IG_LOGIN_BUTTON_CREATE = "foa_ig_login_button_create"
  const val EVENT_FOA_IG_LOGIN_BUTTON_DID_TAP = "foa_ig_login_button_did_tap"
}
