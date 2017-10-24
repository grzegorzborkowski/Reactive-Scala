package shop

import akka.actor.{ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.Cart.{ItemAdded, ItemRemove}
import shop.ShopMessages.{CheckoutStarted, ResponseMessage}

import scala.concurrent.duration._

class CartTests extends TestKit(ActorSystem("MyTests", ConfigFactory.parseString("""akka.loggers = [ "akka.testkit.TestEventListener"]""")))
  with ImplicitSender with
  WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
  /*
  "A Cart Actor" must  {
    "Respond with inserting the new item message when Empty Cart" in {
      val cartActor = system.actorOf(Props[Cart])
      cartActor ! ItemAdded("New Item 1")
      expectMsg(ResponseMessage("Cart was empty. Received ItemAdded(New Item 1)"))
    }
  }
    "Respond with unhandled message when Empty Cart" in {
      val cartActor = system.actorOf(Props[Cart])
      cartActor ! "Blah blah blah"
      expectMsg(ResponseMessage("Cart is empty. Unhandled message"))
    }

    "Should be empty, when inserting and removing item" in {
      val cartActor = system.actorOf(Props[Cart])
      cartActor ! ItemAdded("New Item 1")
      expectMsg(ResponseMessage("Cart was empty. Received ItemAdded(New Item 1)"))
      cartActor ! ItemRemove("New Item 1")
      expectMsg(ResponseMessage("Removed the only item. Cart becomes empty"))
    }

    "Should response with unknown message when inserting and removing item not in a cart" in {
      val cartActor = system.actorOf(Props[Cart])
      cartActor ! ItemAdded("New Item 1")
      expectMsg(ResponseMessage("Cart was empty. Received ItemAdded(New Item 1)"))
      cartActor ! ItemRemove("????????")
      expectMsg(ResponseMessage("Unhandled message"))
    }
    /*
    // TODO: implement this, how to do that?
    "Should response with timeout if there's no response after 5 sec" in {
      val cartActor = system.actorOf(Props[Cart])
      cartActor ! ItemAdded("New Item 5")
      expectMsg(ResponseMessage("Cart was empty. Received ItemAdded(New Item 5)"))
      within(5 seconds) {
        // TODO: fix that
        expectTerminated(cartActor, 5 seconds)
      }
    }
    */

    "Should start checkout properly" in {
      val cartActor = system.actorOf(Props[Cart])
      cartActor ! ItemAdded("New Item 1")
      expectMsg(ResponseMessage("Cart was empty. Received ItemAdded(New Item 1)"))
      cartActor ! CheckoutStarted
      expectMsg(ResponseMessage("CheckoutStarted"))
    }
    */

    /*
    "blah blah blah" in {
      val cartActor = system.actorOf(Props[Cart])
      EventFilter.info(pattern = "Blah blah blah") intercept  {
        cartActor ! ItemAdded("New Item 1")
      }

    }
    */
}
