package org.RAO.fundManager.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.RAO.fundManager.dal.FundMQueryManager
import org.RAO.fundManager.utils.{Json, Utils}
import org.slf4j.LoggerFactory

import java.sql.SQLIntegrityConstraintViolationException
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait RestAPIs extends APIRoutes
{
  val log = LoggerFactory.getLogger(this.getClass)
  implicit val system: ActorSystem
  var dateFormat = new SimpleDateFormat("YYYY:MM:dd")
//  var date=dateFormat.format(new Date())
  var timeFormat = new SimpleDateFormat("HH:mm:ss")
//  var time=timeFormat.format(new Date())

  val dataRoute = pathPrefix("fundmgr")
  {
    pathPrefix("funds")
    {
      (path("addFund") & post & entity(as[Multipart.FormData]))
      {
        formData =>
        {
          val inputMapF = getFormDataToMap(formData)
          onSuccess(inputMapF)
          {
            inputMap =>
            {
              try
              {
                val id = Utils.constructRandomKey(30)
                val name = inputMap("name").toString
                val amount = inputMap("amount").toString.toDouble
                val date = inputMap.getOrElse("date", dateFormat.format(new Date())).toString
                val description = inputMap.getOrElse("description", "").toString
                FundMQueryManager.insertFund(id, name, amount, date, description)
                complete(HttpEntity(ContentTypes.`application/json`, Json.Value(Map("id" -> id, "amount" -> amount, "name" -> name, "date" -> date)).write))
              }
              catch
              {
//              TODO: Add proper exception(unique key exception) type
                case ex:Exception=> complete((StatusCodes.BadRequest,"Duplicate name try with new name"))
              }
            }
          }
        }
      } ~
      (path("getFunds") & get)
      {
        val res = FundMQueryManager.getAllFunds
        if(res.isEmpty)
          complete("No Results")
        else
          complete(HttpEntity(ContentTypes.`application/json`, Json.Value(res).write))
      } ~
      (path("getFund" / Segment) & get)
      {
        id=>
//          TODO: add validation to check id exists or not
          val res = FundMQueryManager.getFund(id)
          complete(HttpEntity(ContentTypes.`application/json`, Json.Value(res).write))
      } ~
      (path("deleteFund" / Segment) &get)
      {
        id=>
//          TODO: try to retrive name first then delete the entry and show name is response
          FundMQueryManager.deleteFund(id)
          complete(s"$id Deleted Successfully")
      }
    } ~
    pathPrefix("transactions")
    {
      (path("add") & post & entity(as[Multipart.FormData]))
      {
        formData =>
        {
          val inputMapF = getFormDataToMap(formData)
          onSuccess(inputMapF)
          {
            inputMap=>
            {
              val id = Utils.constructRandomKey(20)
              val amount = inputMap("amount").toString.toDouble
              val who = inputMap.getOrElse("who","").toString
              val category = inputMap("category").toString
              val fundId = inputMap("fundId").toString
              val description = inputMap.getOrElse("description", "").toString
              val date = dateFormat.format(new Date())
              val time = timeFormat.format(new Date())
//              TODO: validate fund id
              var balance = FundMQueryManager.getFundBalance(fundId).getOrElse(0.0)
              if(category == "CRD") balance = balance + amount
              if(category == "DEB") balance = balance - amount
              FundMQueryManager.insertTransaction(id, amount, who, category, fundId, description, balance, date, time)
              FundMQueryManager.updateFundBalance(fundId, balance)
              complete(HttpEntity(ContentTypes.`application/json`,Json.Value(Map("id"->id, "balance"->balance, "who"->who, "date"->date, "time"->time)).write))
            }
          }
        }
      } ~
      (path("getAll") & get)
      {
        val res = FundMQueryManager.getAllTransactions
        if(res.isEmpty)
          complete("No Results")
        else
          complete(HttpEntity(ContentTypes.`application/json`, Json.Value(res).write))
      } ~
      (path("getOne" / Segment) &get)
      {
        fundId =>
        {
          parameters("cat".?) {
            cat =>
              println(cat.toString)
              if(cat.isEmpty)
              {
                val res = FundMQueryManager.getTransactions(fundId)
                if(res.isEmpty)
                  complete("No Results")
                else
                  complete(HttpEntity(ContentTypes.`application/json`, Json.Value(res).write))
              }
              else
              {
                val res = if (cat.getOrElse("CRD") == "CRD")
                  FundMQueryManager.getTransactionWithType(fundId, "CRD")
                else
                  FundMQueryManager.getTransactionWithType(fundId, "DEB")
                log.info(cat.getOrElse("CRD"))
                if (res.isEmpty)
                  complete("No Results")
                else
                  complete(HttpEntity(ContentTypes.`application/json`, Json.Value(res).write))
              }
          }
        }
      } ~
      (path("delete" / Segment) & get)
      {
        id=>
          FundMQueryManager.deleteTransaction(id)
          complete(s"Transaction $id deleted successfully")
      }
    }
  }

  private def getFormDataToMap(formData: Multipart.FormData): Future[Map[String, Any]] =
  {
    // Method to extract byte array from multipart formdata.
    def getBytesFromFilePart(dataBytes: Source[ByteString, Any]) =
    {
      dataBytes.runFold(ArrayBuffer[Byte]())
      { case (accum, value) => accum ++= value.toArray }
    }
    // Process each form data and store it in a Map[String,Array[Byte]]
    formData.parts.mapAsync(1)
    {
      // Case to extract file or schema input
      // Looks like there might be issues in using the method below resulting in buffer overflow
      // See this thread: https://github.com/akka/akka-http/issues/285
      case b: BodyPart if b.name == "file" || b.name == "schema" => getBytesFromFilePart(b.entity.dataBytes).map(bytes => b.name -> bytes.toArray)
      // Case to extract the rest of the POST parameters
      // Not sure why we are using toStrict() below.
      // Reference: https://github.com/knoldus/akka-http-multipart-form-data.g8/blob/master/src/main/g8/src/main/scala/com/knoldus/MultipartFormDataHandler.scala
      case b: BodyPart => b.toStrict(2.seconds).map(strict => b.name -> strict.entity.data.utf8String.getBytes())
    }
      .runFold(mutable.HashMap[String, Any]())
      { case (map, (keyName, keyVal)) =>
        val value = if (keyName != "file") new String(keyVal)
        else keyVal
        map += (keyName -> value)
      }
      .map(_.toMap)
  }
}
