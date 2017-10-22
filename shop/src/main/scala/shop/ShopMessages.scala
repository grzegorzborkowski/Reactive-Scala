package shop

object ShopMessages {
  case class ItemAdded(item: String)
  case class ItemRemove(item: String)
  case class CheckoutStarted()
  case class CheckoutCanceled()
  case class CheckoutClosed()


}
