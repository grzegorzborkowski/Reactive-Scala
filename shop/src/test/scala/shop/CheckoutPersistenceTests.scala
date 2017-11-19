package shop

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.Checkout.{DeliveryMethodSelected, GetState, PaymentSelected, PaymentServiceStarted}

import scala.util.Random

class CheckoutPersistenceTests extends TestKit(ActorSystem("CheckoutPersistenceTests"))
  with WordSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

    "Checkout" should {
    "Recover properly when in SelectingDelivery state" in {
      val parentCart = TestProbe()
      val checkoutID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      val childCheckout = parentCart.childActorOf(Props(new Checkout(checkoutID)))

      childCheckout ! PoisonPill

      val second = parentCart.childActorOf(Props(new Checkout(checkoutID)))

      second ! GetState
      expectMsg(Checkout.SelectingDeliveryState)
    }

    "Recover properly when in DelieryMethodSelected state" in {
      val parentCart = TestProbe()
      val checkoutID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      val childCheckout = parentCart.childActorOf(Props(new Checkout(checkoutID)))

      childCheckout ! DeliveryMethodSelected
      childCheckout ! GetState
      expectMsg(Checkout.SelectingPaymentMethodState)
      childCheckout ! PoisonPill

      val second = parentCart.childActorOf(Props(new Checkout(checkoutID)))
      second ! GetState
      expectMsg(Checkout.SelectingPaymentMethodState)
    }

      "Recover properly when in ProcessingPayment state" in {
        val checkoutId = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString

        val parentCart = TestProbe()
        val childCheckout = parentCart.childActorOf(Checkout.props(checkoutId))

        childCheckout ! DeliveryMethodSelected
        childCheckout ! PaymentSelected
        expectMsgType[PaymentServiceStarted]

        childCheckout ! GetState
        expectMsg(Checkout.ProcessingPaymentState)

        childCheckout ! PoisonPill

        val second = parentCart.childActorOf(Checkout.props(checkoutId))
        second ! GetState
        expectMsg(Checkout.ProcessingPaymentState)
      }
  }
}
