package shop

import akka.actor.Actor
import akka.event.Logging
import shop.Checkout.PaymentReceived
import shop.PaymentService.PaymentConfirmed
import shop.ShopMessages.DoPayment

class PaymentService extends Actor {
  val log = Logging(context.system, this)
  val checkoutRef = context.parent

  override def receive = {
    case DoPayment => {
      log.info("Received DoPayment message!")
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
