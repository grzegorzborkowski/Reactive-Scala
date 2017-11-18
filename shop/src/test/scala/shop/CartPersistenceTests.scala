package shop

import java.net.URI

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.CartManager.{GetCartState, ItemAdded}

import scala.collection.immutable.Stream.Empty

class CartPersistenceTests extends
  TestKit(ActorSystem("CartPersistenceSystem"))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender
{
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Cart" should {
    val item = Item(new URI("Uri-123"), "First-Item", 10, 1)

    "Add an item to the cart an preserve it after the restart" in {
      val cartManagerID = "1"
      val cartActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))
      cartActor ! ItemAdded(item)

      cartActor ! PoisonPill

      val cartActor_2 = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      cartActor_2 ! GetCartState

      expectMsg(Empty)
    }
  }

}
