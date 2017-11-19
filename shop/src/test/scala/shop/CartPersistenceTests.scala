package shop

import java.net.URI

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import shop.CartManager.{GetCartState, ItemAdded, ItemRemove}

import scala.util.Random


class CartPersistenceTests extends
  TestKit(ActorSystem("CartPersistenceSystem"))
  with WordSpecLike
  with Matchers
  with ImplicitSender {

  "Cart" should {
    val first_item = Item(new URI("Uri-1"), "First-Item", 10, 1)
    val second_item = Item(new URI("Uri-2"), "Second-Item", 10, 1)

    "Add an item to the cart an preserve it after the restart" in {
      // TODO: how to fix that test, so that the when we run the same
      // TODO: test with the same actorID, the first actor doesn't recover its state
      val cartManagerID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      println(cartManagerID)
      val cartActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      cartActor ! ItemAdded(first_item)
      cartActor ! GetCartState

      expectMsg(Cart(Map(new URI("Uri-1") ->
        Item(new URI("Uri-1"), "First-Item", 10, 1))))

      cartActor ! PoisonPill

      val cartActor_2 = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      cartActor_2 ! GetCartState

      expectMsg(Cart(Map(new URI("Uri-1") ->
        Item(new URI("Uri-1"), "First-Item", 10, 1))))
    }

    "Persistence for more than one item" in {
      val cartManagerID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      val firstActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))
      val expectedCart = Cart(Map(
        new URI("Uri-1") -> Item(new URI("Uri-1"), "First-Item", 10, 1),
        new URI("Uri-2") -> Item(new URI("Uri-2"), "Second-Item", 10, 1)))

      firstActor ! ItemAdded(first_item)
      firstActor ! ItemAdded(second_item)
      firstActor ! GetCartState

      expectMsg(expectedCart)

      firstActor ! PoisonPill

      val secondActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      secondActor ! GetCartState

      expectMsg(expectedCart)
    }

    "Persistence for item removal" in {
      val cartManagerID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      val firstActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))
      val expectedCart = Cart(Map(
        new URI("Uri-2") -> Item(new URI("Uri-2"), "Second-Item", 10, 1)))

      firstActor ! ItemAdded(first_item)
      firstActor ! ItemAdded(second_item)
      firstActor ! ItemRemove(first_item, 1)

      firstActor ! PoisonPill

      val secondActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      secondActor ! GetCartState

      expectMsg(expectedCart)
    }
  }

}
