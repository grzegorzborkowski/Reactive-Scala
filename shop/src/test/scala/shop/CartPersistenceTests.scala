package shop

import java.net.URI

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import shop.CartManager.{GetCartState, ItemAdded}

import scala.util.Random


class CartPersistenceTests extends
  TestKit(ActorSystem("CartPersistenceSystem"))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender
  with BeforeAndAfterEach
{
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }



  "Cart" should {
    val item = Item(new URI("Uri-123"), "First-Item", 10, 1)

    "Add an item to the cart an preserve it after the restart" in {
      // TODO: how to fix that test, so that the when we run the same
      // TODO: test with the same actorID, the first actor doesn't recover its state
      val cartManagerID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      println (cartManagerID)
      val cartActor = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      cartActor ! ItemAdded(item)
      cartActor ! GetCartState

      expectMsg(Cart(Map(new URI("Uri-123") ->
        Item(new URI("Uri-123"), "First-Item", 10, 1))))

      cartActor ! PoisonPill

      val cartActor_2 = system.actorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      cartActor_2 ! GetCartState

      expectMsg(Cart(Map(new URI("Uri-123") ->
        Item(new URI("Uri-123"), "First-Item", 10, 1))))
    }
  }

}
