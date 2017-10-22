package shop

import akka.actor.{ActorSystem, Props}
import shop.ShopMessages.{ItemAdded}

object ShopApplication extends App {

  override def main(args: Array[String]): Unit = {
    val system = ActorSystem("ShopApplication")
    val cart = system.actorOf(Props[Cart])

    cart ! ItemAdded("New Item")

  }
}
