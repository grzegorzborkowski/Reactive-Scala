package shop

import akka.actor.Actor
import shop.ShopMessages._
import akka.event.Logging


class Cart extends Actor {
  val log = Logging(context.system, this)
  var items: Set[String] = Set[String]()

  override def receive: Receive = Empty

  def Empty: Receive = {
    case item: ItemAdded => {
      items = items + item.item
      context.become(NonEmpty)
      log.info("Cart was empty. Received {}, current state of the cart is: {}", item, items)
    }
    case other => log.info("Received unhandled message: {}", other)
  }

  def NonEmpty: Receive = {
    case itemRemove: ItemRemove if items.size == 1 && items.contains(itemRemove.item) => {
      items = Set.empty
      log.info("Removing the only one item in cart: {} Cart becomes empty", itemRemove)
      context.become(Empty)
    }
    case itemRemove: ItemRemove if items.size > 1 && items.contains(itemRemove.item) => {
      items = items.filter(item => item.eq(itemRemove.item))
      log.info("Removing item: {}, from Cart. Cart state: {}", itemRemove, items)
    }
    case itemAdded: ItemAdded => {
      items = items + itemAdded.item
      log.info("Received {}, current state of the cart is: {}", itemAdded, items)
    }
    case CheckoutStarted => {
      log.info("Starting the checkout.")
      context.become(InCheckout)
    }
    case msg => {
      log.info ("Currently in NonEmpty state! Received unknown message: {}", msg)
    }
  }

  def InCheckout: Receive = {
    case CheckoutCanceled => {
      log.info("Received the checkout cancelled message")
      context.become(NonEmpty)
    }
    case CheckoutClosed => {
      log.info("Received the checkout closed message.")
      context.become(Empty)
    }
  }
}

