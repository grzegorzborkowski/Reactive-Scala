package shop

import java.net.URI

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{Matchers, WordSpecLike}
import shop.CartManager.{CartTimerExpired, ItemAdded}
import shop.ShopMessages.{CheckoutStarted, StartCheckOut}

import scala.util.Random
import scala.concurrent.duration._

class CartPersistenceTimersTests extends
  TestKit(ActorSystem("CartPersistenceTimersTests"))
  with WordSpecLike
  with Matchers
  with ImplicitSender {

  "Cart" should {
    val first_item = Item(new URI("Uri-1"), "First-Item", 10, 1)

    "Checkout timer retains its time after actor restart" in {
        val cartManagerID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
        val firstActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

        firstActor ! ItemAdded(first_item)

        Thread.sleep(3000)

        firstActor ! PoisonPill

        val secondActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))
        expectMsg(3 seconds, CartTimerExpired)
      }
  }

}
