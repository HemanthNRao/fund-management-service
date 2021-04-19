package org.RAO.fundManager.routes

import akka.dispatch.affinity.RejectionHandler
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, ValidationRejection}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import org.RAO.fundManager.utils.{ErrorConstants, Json}

trait APIRoutes
{
  val serverExceptionHandler:ExceptionHandler = ExceptionHandler
  {
    case ex:Exception => complete(HttpResponse(StatusCodes.InternalServerError, entity = s"${ex.getMessage}"))
  }

  val serverRejectionHandler = RejectionHandler.newBuilder.handle
  {
    case ValidationRejection(msg,_) =>completWithError(msg)
  }.result()

  def completWithError(error:String) =
  {
    def errorDecoder(error:String)=
    {
      error match
      {
        case ErrorConstants.DATE_ALREADY_EXISTS => ErrorInfo(400, StatusCodes.BadRequest, "InputError")
      }
    }
    val err=errorDecoder(error)
    complete(err.status, Json.Value(Map("error"->Map("internalCodes"->err.internalCode, "type"->err.`type`, "message"->error))).write)
  }
  case class ErrorInfo(internalCode:Int, status:StatusCode, `type`:String)
}
