package shop

import akka.actor.Actor
import shop.ShopMessages.ItemAdded
import akka.event.Logging


class Cart extends Actor {

  val log = Logging(context.system, this)
  var items = Set[ItemAdded]()

  override def receive = Empty

  def Empty: Receive = {
    case item: ItemAdded => {
      items = items + item
      context.become(NonEmpty)
      log.info("Cart was empty. Received {}, current state of the cart is: {}", item, items)
    }
    case other => log.info("Received unhandled message: {}", other)
  }

  def NonEmpty: Receive = {
    case msg => {
      log.info ("Currently in NonEmpty state! Received {}", msg)
    }
  }
}
