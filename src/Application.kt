@file:Suppress("SameParameterValue", "BlockingMethodInNonBlockingContext")

package com.dankim

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.*
import kotlinx.html.*
import org.apache.commons.text.StringEscapeUtils

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
  checkRequiredConstants()

  accessToken = loadStoredOAuthToken()

  install(DefaultHeaders)
  install(CallLogging)

  install(Authentication) {
    oauth("google-oauth") {
      client = HttpClient(Apache)
      providerLookup = { googleOauthProvider }
      urlProvider = { redirectUrl("/login") }
    }
  }

  install(Routing) {
    handleStaticResources()
    handleGetRoutes()
    handleAuthenticatedRoutes()
  }
}

fun checkRequiredConstants() {
  if (OAUTH_TOKEN_STORAGE_URI.isEmpty() ||
    OAUTH_CLIENT_ID.isEmpty() ||
    OAUTH_CLIENT_SECRET.isEmpty()
  ) {
    throw IllegalStateException("You need to setup some basic constants first. Please see Constants.kt")
  }
}

private fun handleError(error: String) {
  println(error)
}

private fun getGmailData(): GmailThreads? {
  val response = getGoogleServicesInstance().getGmailThreads("dan@basecamp.com").execute()

  return when {
    response.isSuccessful -> response.body()
    else -> {
      handleError(response.errorBody().toString())
      null
    }
  }
}

private fun getTaskList(): GoogleTasks? {
  val taskLists = getGoogleServicesInstance().getTaskLists().execute()
  val firstListId = taskLists.body()?.taskLists?.first()?.id ?: return null

  val response = getGoogleServicesInstance().getTasks(firstListId).execute()
  return when {
    response.isSuccessful -> response.body()
    else -> {
      handleError(response.errorBody().toString())
      null
    }
  }
}

private fun Route.handleGetRoutes() {
  get("/") {
    call.respondHtml {
      head {
        styleLink("/static-resources/styles.css")
        title {
          +"Dashboard"
        }
      }
      body {
        p {
          ul {
            li {
              +"Is authenticated: ${accessToken?.isNotEmpty() ?: false}"
            }
            li {
              a(href = "/login") {
                +"Login"
              }
            }
            li {
              a(href = "/dashboard") {
                +"Dashboard HTML"
              }
            }
            li {
              a(href = "/dashboard.json") {
                +"Dashboard RESTful API"
              }
            }
          }
        }
      }
    }
  }

  get("/dashboard") {
    val gmailData = getGmailData()
    val taskList = getTaskList()

    call.respondHtml {
      head {
        styleLink("/static-resources/styles.css")
        title {
          +"Dashboard"
        }
      }
      body {
        p {
          h1 { +"Dashboard" }
          h2 { +"Tasks:" }
          h3 { a("/tasks/new") { +"Create new" } }
          ul {
            taskList?.tasks?.forEach {
              it.title?.let {
                li { +it }
              }
            }
          }
          h2 { +"Emails:" }
          ul {
            gmailData?.threads?.forEach {
              li { +StringEscapeUtils.unescapeHtml4(it.snippet) }
            }
          }
        }
      }
    }
  }

  get("/dashboard.json") {
    val gmailData = getGmailData()
    val taskList = getTaskList()

    val myGoogleDashboard = MyGoogleDashboard(
      tasks = taskList?.tasks,
      emails = gmailData?.threads
    )

    call.respondText {
      myGoogleDashboard.toJson()
    }
  }

  get("/tasks/new") {
    call.respondHtml {
      body {
        form("/tasks/new", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
          acceptCharset = "utf-8"
          p {
            label { +"Task: " }
            textInput { name = "taskName" }
          }
          p {
            submitInput { value = "send" }
          }
        }
      }
    }
  }

  post("/tasks/new") {
    val taskLists = getGoogleServicesInstance().getTaskLists().execute()
    val firstListId = taskLists.body()?.taskLists?.first()?.id ?: return@post

    val params = call.receiveParameters()
    val taskName = params.entries().find { it.key == "taskName" }?.value?.first()
    val task = GoogleTask(title = taskName)

    getGoogleServicesInstance().createTask(firstListId, task).execute()

    when (call.request.headers.get("Accept")) {
      "application/json" -> call.respond(HttpStatusCode.OK)
      else -> call.respondRedirect("/dashboard")
    }
  }
}

private fun Route.handleAuthenticatedRoutes() {
  authenticate("google-oauth") {
    route("/login") {
      handle {
        val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
          ?: error("No principal")

        saveOAuthToken(principal.accessToken)

        call.respondRedirect("/")
      }
    }
  }
}

private fun Route.handleStaticResources() {
  static("static-resources") {
    resources("css")
  }
}

fun ApplicationCall.redirectUrl(path: String): String {
  val defaultPort = if (request.origin.scheme == "http") 80 else 443
  val hostPort = request.host() + request.port().let { port -> if (port == defaultPort) "" else ":$port" }
  val protocol = request.origin.scheme
  return "$protocol://$hostPort$path"
}
