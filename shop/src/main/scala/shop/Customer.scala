package shop

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import shop.Checkout.PaymentServiceStarted
import shop.ShopMessages.{CheckoutStarted, ItemAdded}

class Customer extends Actor {
  val log = Logging(context.system, this)
  val cartActorRef: ActorRef = context.actorOf(Props[CartManager], name = "Cart")
  var checkoutRef: ActorRef = _
  var paymentServiceRef: ActorRef = _

  override def receive = {
    case item: ItemAdded => {
      cartActorRef ! item
    }
    case checkout: CheckoutStarted => {
      checkoutRef = checkout.checkoutActorRef
    }
    case paymentServiceStarted: PaymentServiceStarted => {
      paymentServiceRef = paymentServiceStarted.paymentServiceRef
    }
    case other => {
      log.info("Received unhandled message {}", other)
    }
  }
}

object Customer {
  case class StartCheckOut()
}