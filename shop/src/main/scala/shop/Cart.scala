package shop

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.Logging
import shop.Cart._
import shop.ShopMessages._

import scala.concurrent.duration.FiniteDuration

class Cart extends Actor with Timers {
  val log = Logging(context.system, this)
  var items: Set[String] = Set[String]()
  val checkout: ActorRef = context.actorOf(Props[Checkout], "checkout")
  var customer: ActorRef = _

  override def receive: Receive = Empty

  def Empty: Receive = {
    case item: ItemAdded => {
      customer = context.sender
      items = items + item.item
      log.info("Cart was empty. Received {}, current state of the cart is: {}", item, items)
      context.become(NonEmpty)
    }
    case other => {
      log.info("Received unhandled message: {}", other)
    }
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
      log.info("Removing item: {}, from Cart. Cart state: {}", itemRemove.item, items)
    }
    case itemAdded: ItemAdded => {
      startCartTimer()
      items = items + itemAdded.item
      log.info("Received {}, current state of the cart is: {}", itemAdded, items)
    }
    case StartCheckOut => {
      cancelCartTimer()
      log.info("Starting the checkout[inCart].")
      log.info(customer.toString())
      customer ! ShopMessages.CheckoutStarted(checkout)
      context.become(InCheckout)
    }
    case CartTimerExpired => {
      log.info("CartTimeExpired! Your cart is becoming empty.")
      items = Set.empty
      context.become(Empty)
    }
    case other => {
      log.info("Currently in NonEmpty state! Received unknown message: {}", other)
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

object Cart {

  case class ItemRemove(item: String)

  case class CartTimerExpired()

  case object CartTimerExpiredKey

}
