package shop

import akka.actor.{ActorRef, ActorSystem, Props}
import shop.CartManager._
import shop.ShopMessages.CheckoutStarted
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import productcatalog.ProductCatalog
import shop.Checkout.{DeliveryMethodSelected, PaymentReceived, PaymentSelected}

import scala.concurrent.Await
import scala.concurrent.duration._

object ShopApplication extends App {

  override def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val shopSystem = ActorSystem("ShopApplication", config.getConfig("shopSystem").withFallback(config))
    val productCatalogSystem = ActorSystem("ProductCatalog", config.getConfig("productCatalogSystem").withFallback(config))

    val cart = shopSystem.actorOf(Props[CartManager], name = "CartActor")
    val productCatalog = productCatalogSystem.actorOf(Props[ProductCatalog])
    implicit val waitForCheckoutRefTimeout = Timeout(5 seconds)


    val futureResponse = productCatalog ? "Hello"
    val result = Await.result(futureResponse, waitForCheckoutRefTimeout.duration).asInstanceOf[String]
    println (result)
    /*
    val uri = new java.net.URI("1")
    cart ! ItemAdded(Item(uri, "New Item", 10.0, 1))

    Thread.sleep(10000)

    val checkout = Await.result(cart ? CheckoutStarted, waitForCheckoutRefTimeout.duration).asInstanceOf[ActorRef]

    checkout ! DeliveryMethodSelected

    checkout ! PaymentSelected

    checkout ! PaymentReceived */
  }
}
