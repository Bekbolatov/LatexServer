package com.sparkydots.service.latex

import java.io.{StringWriter, PrintWriter}
import javax.inject.{Inject, Singleton}

import com.sparkydots.service.{ServiceInstance, ServiceDiscovery}
import play.api.http.HttpEntity

import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.mvc.{Result, ResponseHeader}
import play.api.mvc.Results.Ok


@Singleton
class LatexService @Inject()(ws: WSClient, serviceDiscovery: ServiceDiscovery) {

  def convertLatex(tex: String, serviceInstance: ServiceInstance,
                   timeoutMillis: Long = 10000,
                   log: Option[(=> String) => Unit] = None
                  )
                  (implicit ec: ExecutionContext): Future[(Boolean, String)] =
    ws.
      url(serviceInstance.httpUrl("/cgi-bin/latex2pdf.sh")).
      withHeaders("Content-Type" -> "application/x-tex").
      withHeaders("Accept" -> "application/json").
      withFollowRedirects(true).
      withRequestTimeout(timeoutMillis.millis).
      post(tex).
      map { response =>
        val jresponse = response.json
        if ((jresponse \ "error").toOption.isDefined)
          (false, serviceInstance.httpUrl((jresponse \ "log").as[String]))
        else
          (true, serviceInstance.httpUrl((jresponse \ "uri").as[String]))
      }

  def convertLatexFile(tex: String,
                       timeoutMillis: Long = 10000,
                       log: Option[(=> String) => Unit] = None)
                      (implicit ec: ExecutionContext): Future[(Boolean, Result)] = {

    val serviceProduct = serviceDiscovery.call[(Boolean, Result)]("latex2pdf", log = log) { serviceInstance =>

      val promise = Promise[Option[(Boolean, Result)]]()

      val futureResponse: Future[(Boolean, WSResponse)] = for {
        (success, uri) <- convertLatex(tex, serviceInstance, timeoutMillis = timeoutMillis, log = log)
        fileResponse <- ws.
          url(uri).
          withFollowRedirects(true).
          withRequestTimeout(timeoutMillis.millis).
          get()
      } yield (success, fileResponse)

      val futureResult: Future[Option[(Boolean, Result)]] = futureResponse.map { case (success, response) =>
        Some((success, if (success) {
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
        )
      }

      futureResult.onSuccess { case res =>
        promise.success(res)
      }

      futureResult.onFailure { case t =>
        log.foreach(_ ("LatexService: Error getting from server"))
        val sw = new StringWriter
        t.printStackTrace(new PrintWriter(sw))
        log.foreach(_ (sw.toString))
        promise.success(None)
      }

      promise.future
    }

    serviceProduct.map {
      case Some(r) => r
      case None => (false, Ok("Server error"))
    }
  }

}
