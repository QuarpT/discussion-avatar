package com.gu.adapters.http

import java.io.File

import com.gu.adapters.http.store.{TestFileStore, TestKVStore}
import com.gu.adapters.store.AvatarStore
import com.gu.core._
import com.gu.utils.TestHelpers
import org.json4s.native.Serialization._
import org.json4s.native.Serialization.write

class AvatarServletTests extends TestHelpers {

  implicit val swagger = new AvatarSwagger

  addServlet(
    new AvatarServlet(
      AvatarStore(new TestFileStore, new TestKVStore),
      Config.cookieDecoder),
    "/*")

  test("Healthcheck should return OK") {
    get("/service/healthcheck") {
      status should equal (200)
      body should include ("OK")
    }
  }

  test("Get avatars") {
    getAvatars("/avatars")
  }

  test("Get avatars by status") {
    val statuses = Set(Inactive, Pending, Approved, Rejected)
    statuses.foreach(s => getAvatars(s"/avatars?status=${s.asString}", _.status == s))
    getAvatars(s"/avatars?status=${All.asString}")
  }

  test("Error response if invalid status") {
    getError("/avatars?status=badStatus", 400, _.message == "Invalid filter parameters")
  }

  test("Get avatar by ID") {
    getAvatar("/avatars/123", _.id == "123")
  }

  test("Get avatars by user ID") {
    getAvatars(s"/avatars/user/123", _.id == "123")
  }

  test("Get active avatar by user ID") {
    getAvatar(s"/avatars/user/123/active", a => a.id == "123" && a.isActive)
  }

  test("Post avatar") {
    val file = new File("src/test/resources/avatar.gif")
    val (userId, cookie) = Config.preProdCookie

    postAvatar(
      "/avatars",
      file,
      userId,
      cookie,
      a => a.userId == userId && a.status == Pending)
  }

  test("Put avatar status") {
    val requestBody = StatusRequest(Approved)
    put("/avatars/345/status", write(requestBody)) {
      status should equal(200)
      val avatar = read[Avatar](body)
      avatar.status should be(Approved)
      getAvatar(s"/avatars/${avatar.id}", _.status == Approved)
    }
  }

  test("Endpoint not found") {
    getError("/avatars/does/not/exist", 404, _.message == "Requested resource not found")
  }
}
