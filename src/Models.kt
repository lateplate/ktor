package com.dankim

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Gmail
@JsonClass(generateAdapter = true)
data class GmailThreads(
  @Json(name = "threads") val threads: List<GmailThread>
)

@JsonClass(generateAdapter = true)
data class GmailThread(
  @Json(name = "id") val id: String,
  @Json(name = "snippet") val snippet: String
)

// Google Tasks
@JsonClass(generateAdapter = true)
data class GoogleTaskLists(
  @Json(name = "items") val taskLists: List<GoogleTaskList>?
)

@JsonClass(generateAdapter = true)
data class GoogleTaskList(
  @Json(name = "id") val id: String?,
  @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class GoogleTasks(
  @Json(name = "items") val tasks: List<GoogleTask>?
)

@JsonClass(generateAdapter = true)
data class GoogleTask(
  @Json(name = "id") val id: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "status") val status: String? = null
)

// Combined object
@JsonClass(generateAdapter = true)
data class MyGoogleDashboard(
  @Json(name = "tasks") val tasks: List<GoogleTask>?,
  @Json(name = "emails") val emails: List<GmailThread>?
)