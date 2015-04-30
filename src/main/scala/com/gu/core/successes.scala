package com.gu.core

sealed trait Success {
  def body: Any
}

case class CreatedAvatar(body: Avatar) extends Success
case class UpdatedAvatar(body: Avatar) extends Success
case class FoundAvatar(body: Avatar) extends Success
case class FoundAvatars(body: List[Avatar]) extends Success