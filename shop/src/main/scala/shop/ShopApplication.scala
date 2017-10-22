package shop

import akka.actor.{ActorSystem, Props}
import shop.ShopMessages._

object ShopApplication extends App {

  override def main(args: Array[String]): Unit = {
    val system = ActorSystem("ShopApplication")
    val cart = system.actorOf(Props[Cart])

    cart ! ItemAdded("New Item")
    cart ! ItemAdded("New Item 2")
    cart ! ItemRemove("New Item")
    cart ! CheckoutStarted
    cart ! CheckoutCanceled
    cart ! CheckoutStarted
    cart ! CheckoutClosed

  }
}
