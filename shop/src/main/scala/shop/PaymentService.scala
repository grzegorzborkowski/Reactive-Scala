package shop

import akka.actor.Actor
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import shop.Checkout.PaymentReceived
import shop.PaymentService.PaymentConfirmed
import shop.ShopMessages.DoPayment
import scalaj.http.{Http => HttpQuery}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PaymentService extends Actor {
  val log = Logging(context.system, this)
  val checkoutRef = context.parent
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  import context.dispatcher
  val http = Http(context.system)

  override def preStart() = {
//    http.singleRequest()
  }

  def shutdown() = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }

  override def receive = {
    case resp @ HttpResponse(StatusCodes.OK, headers, entity, _) => {
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        println("Got response, body: " + body.utf8String)
        resp.discardEntityBytes()
        shutdown()
      }
    }
    case resp @ HttpResponse(code, _, _, _) => {
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      shutdown()
    }


    case DoPayment => {
      log.info("Received DoPayment message!")
      val content = HttpQuery("http://localhost:8080/visa-payment").asString
      log.info("Received response from Visa Payment service {}", content)

      sender ! PaymentConfirmed
      checkoutRef ! PaymentReceived
    }
    case other => {
      log.info("Received unknown message {}", other)
    }
  }

}

object PaymentService {
  case class PaymentConfirmed()
}
