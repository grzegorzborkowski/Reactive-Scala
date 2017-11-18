package shop

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import shop.CartManager.ItemAdded
import shop.ShopMessages.CheckoutStarted

class CartTestAsync extends TestKit(ActorSystem("CartTestAsync"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  "A Cart " should  {
    "Start checkout properly in non empty state" in {
      val cart = system.actorOf(Props[CartManager])
      cart ! ItemAdded(Item(new URI("FirstItem"), "First Item", 10.0, 1))
      cart ! ShopMessages.StartCheckOut
      expectMsgType[CheckoutStarted]
    }

    "No response on Checkout started when in empty state" in {
      val cart = system.actorOf(Props[CartManager])
      cart ! ShopMessages.StartCheckOut
      expectNoMsg()
    }

  }
}
