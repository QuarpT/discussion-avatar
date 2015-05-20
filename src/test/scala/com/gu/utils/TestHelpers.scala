package com.gu.utils

import java.io.File

import com.gu.adapters.http._
import com.gu.core.{Config, Avatar, Status}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats}
import org.scalatest.FunSuiteLike
import org.scalatra.test.ClientResponse
import org.scalatra.test.scalatest.ScalatraSuite

class TestHelpers extends ScalatraSuite with FunSuiteLike {

  protected implicit val jsonFormats: Formats =
    DefaultFormats +
      new StatusSerializer ++
      JodaTimeSerializers.all

  def getOk(
    uri: String,
    p: ClientResponse => Boolean,
    params: List[(String, String)] = Nil,
    headers: Map[String, String] = Map()): Unit = {

    get(uri, params, headers) {
      status should equal (200)
      p(response)
    }
  }

  def getAvatars(
    uri: String,
    p: AvatarResponse => Boolean,
    params: List[(String, String)] = Nil,
    headers: Map[String, String] = Map()) = {

    get(uri, params, headers) {
      status should equal(200)
      val avatars = read[AvatarsResponse](body).data
      avatars.forall(p) should be (true)
    }
  }

  def getAvatars(uri: String): Unit = getAvatars(uri, _ => true)

  def getAvatar(
    uri: String,
    p: AvatarResponse => Boolean,
    params: List[(String, String)] = Nil,
    headers: Map[String, String] = Map()): Unit = {

    get(uri, params, headers) {
      status should equal(200)
      val avatar = read[AvatarResponse](body)
      avatar.uri should be (Some(Config.apiUrl + "/avatars/" + avatar.data.id))
      p(avatar) should be (true)
    }
  }

  def getAvatar(uri: String): Unit = getAvatar(uri, _ => true)

  def getAvatar(uri: String, guuCookie: String, p: AvatarResponse => Boolean): Unit = {
    getAvatar(uri, p, Nil, Map("Cookie" -> ("GU_U=" + guuCookie)))
  }

  def getError(uri: String, code: Int, p: ErrorResponse => Boolean): Unit = {
    get(uri) {
      status should equal(code)
      val error = read[ErrorResponse](body)
      p(error) should be (true)
    }
  }

  def postAvatar(
    uri: String,
    file: File,
    userId: Int,
    guuCookie: String,
    p: AvatarResponse => Boolean): Unit = {

    post("/avatars", Nil, List("image" -> file), Map("Cookie" -> ("GU_U=" + guuCookie))) {
      status should equal(201)
      val avatar = read[AvatarResponse](body)
      p(avatar) should be (true)
      getAvatar(s"/avatars/${avatar.data.id}", p)
    }
  }

  def put(uri: String, toStatus: Status, p: AvatarResponse => Boolean): Unit = {
    val sr = StatusRequest(toStatus)

    put(uri, write(sr)) {
      status should equal(200)
      val avatar = read[AvatarResponse](body)
      p(avatar) should be (true)
      getAvatar(s"/avatars/${avatar.data.id}", p)
    }
  }
}
