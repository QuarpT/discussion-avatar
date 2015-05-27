package com.gu.adapters.http

import com.gu.adapters.utils.ISODateFormatter
import com.gu.core.Errors.invalidFilters
import com.gu.core._
import org.joda.time.DateTime
import org.scalatra.Params

import scalaz._

case class Filters(
  status: Status,
  since: Option[DateTime],
  until: Option[DateTime]
)

object Filters {

  def fromParams[A](params: Params): \/[InvalidFilters, Filters] = {
    val status: Validation[NonEmptyList[String], Status] = params.get("status") match {
      case Some(Inactive.asString) => Success(Inactive)
      case Some(Approved.asString) => Success(Approved)
      case Some(Rejected.asString) => Success(Rejected)
      case Some(Pending.asString) => Success(Pending)
      case Some(invalid) => Failure(NonEmptyList(s"'$invalid' is not a valid status type. Must be '${Inactive.asString}', '${Approved.asString}', '${Rejected.asString}', or '${Pending.asString}'."))
      case None => Success(Approved)
    }

    val Iso8601Regex = """(^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$)""".r

    val since: Validation[NonEmptyList[String], Option[DateTime]] = params.get("since") match {
      case Some(Iso8601Regex(s)) => Success(Some(ISODateFormatter.parse(s)))
      case Some(invalid) => Failure(NonEmptyList(s"'$invalid' is not a valid UUID for since."))
      case None => Success(None)
    }

    val until: Validation[NonEmptyList[String], Option[DateTime]] = params.get("until") match {
      case Some(Iso8601Regex(s)) => Success(Some(ISODateFormatter.parse(s)))
      case Some(invalid) => Failure(NonEmptyList(s"'$invalid' is not a valid UUID for until."))
      case None => Success(None)
    }

    // FIXME: can't specify both 'since' and 'until'

    val filters = for {
      s <- status
      f <- since
      b <- until
    } yield Filters(s, f, b)

    filters.leftMap(invalidFilters).disjunction
  }

  def queryString(f: Filters): String = {
    val params = List(
      "status" -> Some(f.status.asString),
      "since" -> f.since.map(t => ISODateFormatter.print(t)),
      "until" -> f.until.map(t => ISODateFormatter.print(t))
    ) collect {
        case (key, Some(value)) => s"$key=$value"
      }

    params.mkString("?", "&", "")
  }
}