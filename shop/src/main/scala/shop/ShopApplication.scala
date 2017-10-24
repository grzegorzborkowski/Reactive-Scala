package shop

import akka.actor.{ActorRef, ActorSystem, Props}
import shop.Cart._
import shop.ShopMessages.CheckoutStarted
import akka.pattern.ask
import akka.util.Timeout
import shop.Checkout.{DeliverySelected, PaymentMethod, PaymentReceived}

import scala.concurrent.Await
import scala.concurrent.duration._

object ShopApplication extends App {

  override def main(args: Array[String]): Unit = {
    val system = ActorSystem("ShopApplication")
    val cart = system.actorOf(Props[Cart], name = "CartActor")
    implicit val waitForCheckoutRefTimeout = Timeout(5 seconds)

    cart ! ItemAdded("New Item")
    cart ! ItemAdded("New Item 2")
    cart ! ItemRemove("New Item")


    Thread.sleep(10000)
    cart ! ItemAdded("New Item")
    cart ! ItemAdded("New Item 2")
    cart ! ItemRemove("New Item")

    val checkout = Await.result(cart ? CheckoutStarted, waitForCheckoutRefTimeout.duration).asInstanceOf[ActorRef]

    checkout ! DeliverySelected

    checkout ! PaymentMethod

    checkout ! PaymentReceived
  }
}
