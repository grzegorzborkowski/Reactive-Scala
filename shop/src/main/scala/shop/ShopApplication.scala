package shop

import akka.actor.{ActorRef, ActorSystem, Props}
import shop.CartManager._
import shop.ShopMessages.{CheckoutStarted, ItemAdded}
import akka.pattern.ask
import akka.util.Timeout
import shop.Checkout.{DeliveryMethodSelected, PaymentSelected, PaymentReceived}

import scala.concurrent.Await
import scala.concurrent.duration._

object ShopApplication extends App {

  override def main(args: Array[String]): Unit = {
    val system = ActorSystem("ShopApplication")
    val cart = system.actorOf(Props[CartManager], name = "CartActor")
    implicit val waitForCheckoutRefTimeout = Timeout(5 seconds)

    val uri = new java.net.URI("1")
    cart ! ItemAdded(Item(uri, "New Item", 10.0, 1))

    Thread.sleep(10000)

    val checkout = Await.result(cart ? CheckoutStarted, waitForCheckoutRefTimeout.duration).asInstanceOf[ActorRef]

    checkout ! DeliveryMethodSelected

    checkout ! PaymentSelected

    checkout ! PaymentReceived
  }
}
