package shop

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop.CartManager.ItemRemove
import shop.ShopMessages.ItemAdded

class CartTestsSync extends TestKit(ActorSystem("CartTests"))  with WordSpecLike
  with BeforeAndAfterEach with BeforeAndAfterAll {

  val actorRef = TestActorRef[CartManager]
  val actor = actorRef.underlyingActor

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Cart" must {
      "be empty at the begining" in {
        val actorRef = TestActorRef[CartManager]
        val actor = actorRef.underlyingActor

        assert (actor.shoppingCart.items.isEmpty)
    }

    "must not be empty after adding an element" in {
      val actorRef = TestActorRef[CartManager]
      val actor = actorRef.underlyingActor

      actorRef ! ItemAdded(Item(new java.net.URI("First_Item"), "First Item", 10, 1))
      assert (actor.shoppingCart.items.contains(new java.net.URI("First_Item")))
    }

    "remove properly elements" in {
      val actorRef = TestActorRef[CartManager]
      val actor = actorRef.underlyingActor

      actorRef ! ItemAdded(Item(new java.net.URI("Second_Item"), "Second Item", 10, 1))
      actorRef ! ItemRemove(Item(new java.net.URI("First_Item"), "First Item", 10, 1), 1)
      assert (actor.shoppingCart.items.size == 1 &&
        actor.shoppingCart.items.contains(new java.net.URI("Second_Item")) &&
        !actor.shoppingCart.items.contains(new java.net.URI("First_Item")))
    }

}

}
