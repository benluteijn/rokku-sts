package com.ing.wbaa.gargoyle.sts.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.ing.wbaa.gargoyle.sts.service.UserInfo
import com.typesafe.scalalogging.LazyLogging
import spray.json.RootJsonFormat

import scala.concurrent.Future

trait UserApi extends LazyLogging {

  def isCredentialActive(accessKey: String, sessionToken: String): Future[Boolean]

  def getUserInfo(accessKey: String): Future[Option[UserInfo]]

  val userRoutes: Route = verifyUser ~ getUser

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol._

  implicit val userInfoJsonFormat: RootJsonFormat[UserInfo] = jsonFormat4(UserInfo)

  def verifyUser: Route = logRequestResult("debug") {
    path("isCredentialActive") {
      get {
        parameters(("accessKey", "sessionToken")) { (accessKey, sessionToken) =>
          onSuccess(isCredentialActive(accessKey, sessionToken)) { isActive =>
            val result = if (isActive) {
              logger.info("isCredentialActive ok for accessKey={}, sessionToken={}", accessKey, sessionToken)
              StatusCodes.OK
            } else {
              logger.info("isCredentialActive forbidden for accessKey={}, sessionToken={}", accessKey, sessionToken)
              StatusCodes.Forbidden
            }
            complete(result)
          }
        }
      }
    }
  }

  def getUser: Route = logRequestResult("debug") {
    path("userInfo") {
      get {
        parameters('accessKey) {
          accessKey =>
            onSuccess(getUserInfo(accessKey)) {
              case Some(userInfo) =>
                logger.info("user info ok for accessKey={}", accessKey)
                complete(userInfo)
              case _ =>
                logger.info("user info not found for accessKey={}", accessKey)
                complete(StatusCodes.NotFound)
            }
        }
      }
    }
  }

}