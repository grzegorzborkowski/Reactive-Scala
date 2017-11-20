package shop

import java.net.URI

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.CartManager.{CartTimerExpired, ItemAdded}

import scala.util.Random
import scala.concurrent.duration._

class CartPersistenceTimersTests extends
  TestKit(ActorSystem("CartPersistenceTimersTests"))
  with WordSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Cart" should {
    val first_item = Item(new URI("Uri-1"), "First-Item", 10, 1)

    "Checkout timer retains its time after actor restart" in {
      val parentCart = TestProbe()

      val cartManagerID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      val firstActor = parentCart.childActorOf(Props(new CartManager(cartManagerID, Cart.empty)))

      firstActor ! ItemAdded(first_item)

      Thread.sleep(3000)

      firstActor ! PoisonPill

      parentCart.childActorOf(Props(new CartManager(cartManagerID, Cart.empty)))
      parentCart.expectMsg(3 seconds, CartTimerExpired)
    }
  }

}
