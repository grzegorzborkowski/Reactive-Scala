package shop

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Timers}
import akka.event.Logging
import shop.Cart._

import scala.concurrent.duration.FiniteDuration


object Cart {
  case class ItemAdded(item: String)
  case class ItemRemove(item: String)
  case class CheckoutStarted()
  case class CheckoutCanceled()
  case class CheckoutClosed()
  case class CartTimerExpired()

  case object CartTimerExpiredKey

}

class Cart extends Actor with Timers {
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
      startCartTimer()
      items = Set.empty
      log.info("Removing the only one item in cart: {} Cart becomes empty", itemRemove)
      context.become(Empty)
    }
    case itemRemove: ItemRemove if items.size > 1 && items.contains(itemRemove.item) => {
      startCartTimer()
      items = items.filter(item => item.eq(itemRemove.item))
      log.info("Removing item: {}, from Cart. Cart state: {}", itemRemove, items)
    }
    case itemAdded: ItemAdded => {
      startCartTimer()
      items = items + itemAdded.item
      log.info("Received {}, current state of the cart is: {}", itemAdded, items)
    }
    case CheckoutStarted => {
      // TODO: implement starting a CheckoutActor here
      cancelCartTimer()
      log.info("Starting the checkout.")
      context.become(InCheckout)
    }
    case CartTimerExpired => {
      log.info("CartTimeExpired! Your cart is becoming empty.")
      items = Set.empty
      context.become(Empty)
    }
    case other => {
      log.info ("Currently in NonEmpty state! Received unknown message: {}", other)
    }
  }

  def InCheckout: Receive = {
    case CheckoutCanceled => {
      log.info("Received the checkout cancelled message")
      startCartTimer()
      context.become(NonEmpty)
    }
    case CheckoutClosed => {
      log.info("Received the checkout closed message.")
      items = Set.empty
      context.become(Empty)
    }
    case other => log.info("Currently in InCheckout state! Received unknown message: {}", other)
  }

  def startCartTimer(): Unit = {
    timers.startSingleTimer(CartTimerExpiredKey, CartTimerExpired, new FiniteDuration(5, TimeUnit.SECONDS))
  }

  def cancelCartTimer(): Unit = {
    timers.cancel(CartTimerExpiredKey)
  }

}

