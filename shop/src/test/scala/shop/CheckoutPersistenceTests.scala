package shop

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.Checkout.{DeliveryMethodSelected, GetState, PaymentSelected}

class CheckoutPersistenceTests extends TestKit(ActorSystem("CheckoutPersistenceTests"))
  with WordSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

    "Checkout" should {
    "Recover properly when in SelectingDelivery state" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])

      childCheckout ! PoisonPill

      val second = parentCart.childActorOf(Props[Checkout])

      second ! GetState
      expectMsg(Checkout.SelectingDeliveryState)
    }

    "Recover properly when in DelieryMethodSelected state" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])

      childCheckout ! DeliveryMethodSelected

      childCheckout ! PoisonPill

      val second = parentCart.childActorOf(Props[Checkout])
      second ! GetState
      expectMsg(Checkout.SelectingPaymentMethodState)
    }

    "Recover properly when in ProcessingPayment state" in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])

      childCheckout ! DeliveryMethodSelected
      childCheckout ! PaymentSelected

      val second = parentCart.childActorOf(Props[Checkout])
      second ! GetState
      expectMsg(Checkout.ProcessingPaymentState)
    }
  }
}
