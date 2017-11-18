package shop

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import shop.ShopMessages.{CheckoutStarted, ItemAdded}

class CartTestAsync extends TestKit(ActorSystem("CartTestAsync"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  "A Cart " should  {
    "Start checkout properly in non empty state" in {
      val cart = system.actorOf(Props[Cart])
      cart ! ItemAdded("First Item")
      cart ! ShopMessages.StartCheckOut
      expectMsgType[CheckoutStarted]
    }

    "No response on Checkout started when in empty state" in {
      val cart = system.actorOf(Props[Cart])
      cart ! ShopMessages.StartCheckOut
      expectNoMsg()
    }

  }
}
