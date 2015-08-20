package com.gu.adapters.http

import com.gu.core.models.Errors.invalidFilters
import com.gu.core.models._
import com.gu.core.utils.ErrorHandling.attempt
import com.gu.core.utils.ISODateFormatter
import org.joda.time.DateTime
import org.scalatra.Params

import scalaz.{ Success, _ }

object Filter {

  def fromParams[A](params: Params): InvalidFilters \/ Filters = {
    val status: Validation[NonEmptyList[String], Status] = params.get("status") match {
      case Some(Inactive.asString) => Success(Inactive)
      case Some(Approved.asString) => Success(Approved)
      case Some(Rejected.asString) => Success(Rejected)
      case Some(Pending.asString) => Success(Pending)
      case Some(invalid) => Failure(NonEmptyList(s"'$invalid' is not a valid status type. Must be '${Inactive.asString}', '${Approved.asString}', '${Rejected.asString}', or '${Pending.asString}'."))
      case None => Success(Approved)
    }

    val since: Validation[NonEmptyList[String], Option[DateTime]] = params.get("since") match {
      case Some(s) => attempt(Some(ISODateFormatter.parse(s))).validation
        .leftMap(_ => NonEmptyList(s"'$s' is not a valid datetime format for 'since'. Must be 'YYYY-MM-DDThh:mm:ssZ'"))
      case None => Success(None)
    }

    val until: Validation[NonEmptyList[String], Option[DateTime]] = params.get("until") match {
      case Some(s) => attempt(Some(ISODateFormatter.parse(s))).validation
        .leftMap(_ => NonEmptyList(s"'$s' is not a valid datetime format for 'until'. Must be 'YYYY-MM-DDThh:mm:ssZ'"))
      case None => Success(None)
    }

    val filters = for {
      s <- status
      f <- since
      b <- until
    } yield Filters(s, f, b)

    filters
      .ensure(NonEmptyList("Cannot specify both 'since' and 'until' parameters"))(f => f.since.isEmpty || f.until.isEmpty)
      .leftMap(invalidFilters).disjunction
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