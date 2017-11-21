package shop

import akka.actor.{ActorRef, ActorSystem, Props}
import shop.CartManager._
import shop.ShopMessages.{CheckoutStarted, DoPayment, StartCheckOut}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import paymentservers.VisaServer
import productcatalog.ProductCatalog
import productcatalog.ProductCatalog.GetElements
import shop.Checkout.{DeliveryMethodSelected, PaymentReceived, PaymentSelected}

import scala.collection.immutable.ListMap
import scala.concurrent.Await
import scala.concurrent.duration._

object ShopApplication extends App {

  override def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val shopSystem = ActorSystem("ShopApplication", config.getConfig("shopSystem").withFallback(config))
    val productCatalogSystem = ActorSystem("ProductCatalog", config.getConfig("productCatalogSystem").withFallback(config))

    val paymentSystem = ActorSystem("Visa", config.getConfig("paymentSystem").withFallback(config))
    val visaServer = paymentSystem.actorOf(Props(new VisaServer(paymentSystem)))

    val cart = shopSystem.actorOf(Props[CartManager], name = "CartActor")
    val productCatalog = productCatalogSystem.actorOf(Props[ProductCatalog])
    implicit val waitTime = Timeout(5 seconds)
    // implicit val waitForPaymentSelected = Timeout(10 seconds)
    val futureResponse = productCatalog ? GetElements("Beef")
    val result = Await.result(futureResponse, waitTime.duration).asInstanceOf[Seq[(Int, Item)]]
    println (result.toString())

    val uri = new java.net.URI("1")
    cart ! ItemAdded(Item(uri, "New Item", 10.0, 1))


    val checkout = Await.result(cart ? StartCheckOut, waitTime.duration).asInstanceOf[ActorRef]

    checkout ! DeliveryMethodSelected

    checkout ! PaymentSelected

    val paymentService = Await.result(checkout ? PaymentSelected, waitTime.duration).asInstanceOf[ActorRef]

    paymentService ! DoPayment


    checkout ! PaymentReceived
  }
}
