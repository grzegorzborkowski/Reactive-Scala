package shop

import akka.actor.{ActorRef, ActorSystem, Props}
import shop.CartManager._
import shop.ShopMessages.{CheckoutStarted, DoDangerousPayment, DoPayment, StartCheckOut}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import paymentservers.{VisaServer, VisaServerSupervisor}
import productcatalog.ProductCatalog
import productcatalog.ProductCatalog.GetElements
import shop.Checkout.{DeliveryMethodSelected, PaymentReceived, PaymentSelected, PaymentServiceStarted}
import shop.PaymentService.PaymentConfirmed

import scala.collection.immutable.ListMap
import scala.concurrent.Await
import scala.concurrent.duration._

object ShopApplication extends App {

  override def main(args: Array[String]): Unit = {
    val debug = true
    val config = ConfigFactory.load()

    val shopSystem = ActorSystem("ShopApplication", config.getConfig("shopSystem").withFallback(config))
    val productCatalogSystem = ActorSystem("ProductCatalog", config.getConfig("productCatalogSystem").withFallback(config))

    val paymentSystem = ActorSystem("Visa", config.getConfig("paymentSystem").withFallback(config))
    //val visaServer = paymentSystem.actorOf(
    //  Props(new VisaServer(paymentSystem)))

    val visaServer = paymentSystem.actorOf(
      Props(new VisaServerSupervisor(paymentSystem))
    )
    val cart = shopSystem.actorOf(Props[CartManager], name = "CartActor")
    val productCatalog = productCatalogSystem.actorOf(Props[ProductCatalog])
    implicit val waitTime = Timeout(30 seconds)

    val futureResponse = productCatalog ? GetElements("Beef")
    val result = Await.result(futureResponse, waitTime.duration).asInstanceOf[Seq[(Int, Item)]]
    println (result.toString())

    val uri = new java.net.URI("1")
    if (debug) {
      System.out.println("[DEBUG] Sending ItemAdded request to CartManager")
    }
    cart ! ItemAdded(Item(uri, "New Item", 10.0, 1))
    if (debug) {
      System.out.println("[DEBUG] Sent ItemAdded request to CartManager")
    }

    if(debug) {
      System.out.println("[DEBUG] Sending StartCheckout ask to cart")
    }
    val checkoutStarted = Await.result(cart ? StartCheckOut, waitTime.duration).asInstanceOf[CheckoutStarted]
    val checkout = checkoutStarted.checkoutActorRef

    checkout ! DeliveryMethodSelected

    val paymentSelectedMessage = PaymentSelected("Visa")
    val paymentService = Await.result(checkout ? paymentSelectedMessage, waitTime.duration).asInstanceOf[PaymentServiceStarted]

    if (debug) {
      System.out.println("[DEBUG] Payment service response: {}", paymentService)
    }

    //val response = Await.result(paymentService.paymentServiceRef ? DoPayment(), waitTime.duration).asInstanceOf[PaymentConfirmed]

    val response = Await.result(paymentService.paymentServiceRef ? DoDangerousPayment(), waitTime.duration)

    System.out.println(response)
  }
}
