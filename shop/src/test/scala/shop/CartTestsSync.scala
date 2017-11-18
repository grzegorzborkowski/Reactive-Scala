package shop

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}
import shop.Cart.ItemRemove
import shop.ShopMessages.ItemAdded

class CartTestsSync extends TestKit(ActorSystem("CartTests"))  with WordSpecLike
  with BeforeAndAfterEach with BeforeAndAfterAll {

  val actorRef = TestActorRef[Cart]
  val actor = actorRef.underlyingActor

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Cart" must {
      "be empty at the begining" in {
        val actorRef = TestActorRef[Cart]
        val actor = actorRef.underlyingActor

        assert (actor.items.isEmpty)
    }

    "must not be empty after adding an element" in {
      val actorRef = TestActorRef[Cart]
      val actor = actorRef.underlyingActor

      actorRef ! ItemAdded("First Item")
      assert (actor.items.contains("First Item"))
    }

    "remove properly elements" in {
      val actorRef = TestActorRef[Cart]
      val actor = actorRef.underlyingActor

      actorRef ! ItemAdded("Second Item")
      actorRef ! ItemRemove("First Item")
      assert (actor.items.size == 1 && actor.items.contains("Second Item") &&
        !actor.items.contains("First Item"))
    }

}

}
