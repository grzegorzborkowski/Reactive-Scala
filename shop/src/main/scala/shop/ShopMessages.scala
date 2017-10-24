package shop

object ShopMessages {

  case class ResponseMessage(log: String)

  case class CheckoutStarted()

  case class CheckoutCanceled()

  case class CheckoutClosed()

}
