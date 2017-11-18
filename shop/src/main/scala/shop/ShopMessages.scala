package shop

import akka.actor.ActorRef

object ShopMessages {

  case class ItemAdded(item: Item)

  case class ResponseMessage(log: String)

  case class CheckoutStarted(checkoutActorRef: ActorRef)

  case class CheckoutCanceled()

  case class CheckoutClosed()

  case class StartCheckOut()

  case class DoPayment()

}
