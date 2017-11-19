package shop

import java.net.URI
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Props, Timers}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import shop.CartManager._
import shop.NewState.NewState
import shop.ShopMessages._

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

case class Item(id: URI, name: String, price: BigDecimal, count: Int)

case class Cart(items: Map[URI, Item]) {

  def addItem(item: Item): Cart = {
    val currentCount = if (items contains item.id) items(item.id).count else 0
    var itemsUpdated: Map[URI, Item] = Map.empty
    if (currentCount == 0) {
      itemsUpdated = items + (item.id -> Item(item.id, item.name, item.price, item.count))
    } else {
      itemsUpdated = items + (item.id -> Item(item.id, item.name, item.price, item.count + 1))
    }
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
        case newCount if newCount > 0 => {
          val newItemsMap = items + (item.id -> Item(item.id, item.name, item.price, newCount))
          (Cart(newItemsMap), NewState.NonEmpty)
        }
        case newCount if newCount < 0 => {
          (Cart(items), NewState.NonEmpty)
        }
      }
    } else (Cart(items), NewState.NonEmpty)
  }

  def size: Int = items.size
}

object Cart {
  val empty = Cart(Map.empty)
}

class CartManager(id: String, var shoppingCart: Cart) extends PersistentActor with Timers {
  def this() = this(new Random(System.currentTimeMillis).alphanumeric.take(10).mkString,
    Cart.empty)

  val log = Logging(context.system, this)
  val checkout: ActorRef = context.actorOf(Props[Checkout], "checkout")
  var customer: ActorRef = _

  override def receive: Receive = Empty

  def Empty: Receive = {
    case item: ItemAdded => {
      persist(item) {
        event =>
          log.info("Currently empty. Current state of the cart:" + shoppingCart)
          shoppingCart = shoppingCart.addItem(item.item)
          customer = context.sender
          saveSnapshot(shoppingCart)
          log.info("\nCart was empty. Received {}, current state of the cart is: {}", item, shoppingCart)
          context.become(NonEmpty)
      }
    }
    case GetCartState => {
      sender ! shoppingCart
    }
    case other => {
      log.info("Received unhandled message in Empty state: {}", other)
    }
  }

  def NonEmpty: Receive = {
    case itemRemove: ItemRemove => {
      persist(itemRemove) {
        event => {
          startCartTimer()
          val resultTuple = shoppingCart.removeItem(itemRemove.item, itemRemove.count)
          shoppingCart = resultTuple._1
          val state = resultTuple._2
          saveSnapshot(shoppingCart)
          log.info("Received ItemRemove message: {}. Current state of the cart:", itemRemove, shoppingCart)
          state match {
            case NewState.Empty => context.become(Empty)
            case NewState.NonEmpty =>
          }
        }
      }
    }
    case itemAdded: ItemAdded => {
      persist(itemAdded) {
        event =>
          log.info("Currently non empty. Current state of the cart:" + shoppingCart)
          shoppingCart = shoppingCart.addItem(itemAdded.item)
          customer = context.sender
          saveSnapshot(shoppingCart)
          log.info("\nCart was not empty. Received {}, current state of the cart is: {}", itemAdded, shoppingCart)
      }
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
    case GetCartState => {
      sender ! shoppingCart
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
    case GetCartState => {
      sender ! shoppingCart
    }
    case other => log.info("Currently in InCheckout state! Received unknown message: {}", other)
  }

  def startCartTimer(): Unit = {
    timers.startSingleTimer(CartTimerExpiredKey, CartTimerExpired, new FiniteDuration(5, TimeUnit.SECONDS))
  }

  def cancelCartTimer(): Unit = {
    timers.cancel(CartTimerExpiredKey)
  }

  override def receiveRecover: Receive = LoggingReceive {
    case RecoveryCompleted => log.info("Recovery completed!")
    case SnapshotOffer(_, snapshot: Cart) => {
      shoppingCart = snapshot
      log.info("Recovered cart state: " + shoppingCart.toString)
    }
    case item: ItemAdded => {
      log.info("Receive Recoever ItemAdde!")
      shoppingCart = shoppingCart.addItem(item.item)
    }
    case item: ItemRemove => {
      log.info("Receive Recover ItemRemove")
      val tuple = shoppingCart.removeItem(item.item, item.count)
      shoppingCart = tuple._1
    }
    case other =>
      log.info("Message received in receiveRecover: " + other)
  }

  override def receiveCommand: Receive = Empty

  override def persistenceId = id
}

object CartManager {

  sealed trait Event

  case class ItemAddedEvent(item: Item) extends Event
  case class ItemRemovedEvent(item: Item) extends Event

  case class ItemAdded(item: Item)

  case class ItemRemove(item: Item, count: Int)

  case class CartTimerExpired()

  case object CartTimerExpiredKey

  case class GetCartState()

}

object NewState extends Enumeration {
  type NewState = Value
  val Empty, NonEmpty = Value
}
