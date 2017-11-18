package shop

import java.net.URI
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.Logging
import shop.CartManager._
import shop.NewState.NewState
import shop.ShopMessages._

import scala.concurrent.duration.FiniteDuration

case class Item(id: URI, name: String, price: BigDecimal, count: Int)

case class Cart(items: Map[URI, Item]) {

  def addItem(item: Item): Cart = {
      val itemsUpdated = items + (item.id -> Item(item.id, item.name, item.price, item.count+1))
      Cart(itemsUpdated)
    }
    def removeItem(item: Item, count: Int): (Cart, NewState) = {
      val currentCount = if (items contains item.id) items(item.id).count else 0
      if (currentCount != 0) {
          val newCount = item.count - count
          newCount match {
            case 0 => {
              val newItemsMap = items + (item.id -> Item(item.id, item.name, item.price, 0))
              (Cart(newItemsMap), NewState.Empty)
            }
            case newCount if newCount>0 => {
              val newItemsMap = items + (item.id -> Item(item.id, item.name, item.price, newCount))
              (Cart(newItemsMap), NewState.NonEmpty)
            }
            case newCount if newCount < 0 => {
              (Cart(items), NewState.NonEmpty)
            }
          }
      } else (Cart(items), NewState.NonEmpty)
    }
}

object Cart {
  val empty = Cart(Map.empty)
}

class CartManager(var shoppingCart : Cart) extends Actor with Timers {
  def this() = this(Cart.empty)

  val log = Logging(context.system, this)
  val checkout: ActorRef = context.actorOf(Props[Checkout], "checkout")
  var customer: ActorRef = _

  override def receive: Receive = Empty

  def Empty: Receive = {
    case item: ItemAdded => {
      shoppingCart = shoppingCart.addItem(item.item)
      customer = context.sender
      log.info("Cart was empty. Received {}, current state of the cart is: {}", item, shoppingCart)
      context.become(NonEmpty)
    }
    case other => {
      log.info("Received unhandled message: {}", other)
    }
  }

  def NonEmpty: Receive = {
    case itemRemove: ItemRemove => {
      startCartTimer()
      val resultTuple = shoppingCart.removeItem(itemRemove.item, itemRemove.count)
      shoppingCart = resultTuple._1
      val state = resultTuple._2
      log.info("Received ItemRemove message: {}. Current state of the cart:", itemRemove, shoppingCart)
      state match {
        case NewState.Empty => context.become(Empty)
        case NewState.NonEmpty =>
      }
    }
    case itemAdded: ItemAdded => {
      shoppingCart = shoppingCart.addItem(itemAdded.item)
      log.info("Cart was non empty . Received {}, current state of the cart is: {}", shoppingCart)
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
      shoppingCart = Cart.empty
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
      shoppingCart = Cart.empty
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

object CartManager {

  case class ItemRemove(item: Item, count: Int)

  case class CartTimerExpired()

  case object CartTimerExpiredKey

}

object NewState extends Enumeration {
  type NewState = Value
  val Empty, NonEmpty = Value
}
