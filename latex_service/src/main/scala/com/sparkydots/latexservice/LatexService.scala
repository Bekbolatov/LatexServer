package com.sparkydots.latexservice

import javax.inject.{Inject, Singleton}

import play.api.http.HttpEntity

import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.mvc.{Result, ResponseHeader}

import scala.concurrent.ExecutionContext.Implicits.global
import com.sparkydots.utils.{ServiceInstance, ServiceDiscovery}
import play.api.mvc.Results.Ok


@Singleton
class LatexService @Inject()(ws: WSClient, serviceDiscovery: ServiceDiscovery) {

  def convertLatex(tex: String, serviceInstance: ServiceInstance): Future[(Boolean, String)] = {

    val host = serviceInstance.host
    val port = serviceInstance.port

    val url = s"http://$host:$port/cgi-bin/latex2pdf.sh"

    val request: WSRequest = ws.url(url).
      withHeaders("Content-Type" -> "application/x-tex").
      withHeaders("Accept" -> "application/json").
      withRequestTimeout(10000.millis)

    val futureResponse: Future[WSResponse] = request.post(tex)

    val futureResult: Future[(Boolean, String)] = futureResponse.map {
      response =>
        val jresponse = response.json
        val error = jresponse \ "error"

        if (error.toOption.isDefined) {
          (false, s"http://$host:$port${(response.json \ "log").as[String]}")
        } else {
          (true, s"http://$host:$port${(response.json \ "uri").as[String]}")
        }
    }

    futureResult
  }

  def convertLatexFile(tex: String): Future[(Boolean, Result)] = {

    serviceDiscovery.call[Future[(Boolean, Result)]]("latex2pdf") { serviceInstance=>

      val futureResponse: Future[(Boolean, WSResponse)] = for {
        (success, uri) <- convertLatex(tex, serviceInstance)
        fileResponse <- ws.url(uri).get()
      } yield (success, fileResponse)

      val futureResult: Future[(Boolean, Result)] = futureResponse.map { case (success, response) =>
        (success, if (success) {
          Result(
            header = ResponseHeader(200),
            body = HttpEntity.Strict(response.bodyAsBytes, Some("application/pdf"))
          ).withHeaders("Content-Disposition" -> "inline; filename=\"result.pdf\"")
        } else {
          Result(
            header = ResponseHeader(200),
            body = HttpEntity.Strict(response.bodyAsBytes, Some("plain/text"))
          )
        })
      }
      Some(futureResult)
    }
  }.getOrElse {
    Future.successful {
      (false, Ok("Server error"))
    }
  }
}
