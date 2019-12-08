package com.dankim

import io.ktor.auth.OAuthServerSettings
import io.ktor.http.HttpMethod
import java.io.File

var accessToken: String? = null
val googleOauthProvider = OAuthServerSettings.OAuth2ServerSettings(
  name = "google",
  authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
  accessTokenUrl = "https://www.googleapis.com/oauth2/v3/token",
  requestMethod = HttpMethod.Post,
  clientId = OAUTH_CLIENT_ID,
  clientSecret = OAUTH_CLIENT_SECRET,
  defaultScopes = listOf(
    "profile",
    "email",
    "https://www.googleapis.com/auth/gmail.readonly",
    "https://www.googleapis.com/auth/tasks"
  )
)

fun saveOAuthToken(token: String) {
  accessToken = token
  File(OAUTH_TOKEN_STORAGE_URI).writeText(token)
}

fun loadStoredOAuthToken(): String? {
  return try {
    val fileText = File(OAUTH_TOKEN_STORAGE_URI).readText()
    when {
      fileText.isNotEmpty() -> fileText
      else -> null
    }
  } catch (e: Exception) {
    null
  }
}