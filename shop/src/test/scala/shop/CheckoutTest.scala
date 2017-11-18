package shop

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import shop.Checkout._
import shop.ShopMessages.{CheckoutCanceled, CheckoutClosed}

import scala.concurrent.duration._

class CheckoutTest extends TestKit(ActorSystem("CheckoutTests"))
  with WordSpecLike
  with BeforeAndAfterAll
{

  override def afterAll(): Unit = {
    system.terminate
  }

  "A Cart " should  {
    "properly get checkoutClose message" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])
      childCheckout ! DeliveryMethodSelected
      childCheckout ! PaymentSelected
      childCheckout ! PaymentReceived
      parentCart.expectMsg(CheckoutClosed)
    }

    "get checkoutCanceled message after DeliveryMethodSelection timeout" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])
      parentCart.expectMsg(10 seconds, CheckoutCanceled)
    }

    "be notified about checkoutCanceled when Checkout got checkoutCanceled" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])
      childCheckout ! CheckoutCanceled
      parentCart.expectMsg(CheckoutCanceled)
    }

    "be notified about checkoutCanceled when PaymentTimeout" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])
      childCheckout ! DeliveryMethodSelected
      parentCart.expectMsg(10 seconds, CheckoutCanceled)
    }

    "get CheckoutCanceled message after SelectingMethodPaymentCanceled" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])
      childCheckout ! DeliveryMethodSelected
      childCheckout ! SelectingPaymentMethodCanceled
      parentCart.expectMsg(CheckoutCanceled)
    }

    "get checkoutCanceled message after Processing Payment timeout" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])
      childCheckout ! DeliveryMethodSelected
      childCheckout ! PaymentSelected
      parentCart.expectMsg(10 seconds, CheckoutCanceled)
    }

    "get CheckoutCanceled message after Processing Payment canceled" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])
      childCheckout ! DeliveryMethodSelected
      childCheckout ! PaymentSelected
      childCheckout ! PaymentCanceled
      parentCart.expectMsg(CheckoutCanceled)
    }

  }

}
