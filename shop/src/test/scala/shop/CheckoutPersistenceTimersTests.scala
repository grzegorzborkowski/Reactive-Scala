package shop

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.Checkout.DeliveryMethodSelected
import shop.ShopMessages.CheckoutCanceled

import scala.concurrent.duration._

class CheckoutPersistenceTimersTests extends TestKit(ActorSystem("CheckoutPersistenceTimersTests"))
  with WordSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Checkout" should {
    "Restore CheckoutTimer properly after restart " in {
      val parentCart = TestProbe()
      val childCheckout = parentCart.childActorOf(Props[Checkout])

      Thread.sleep(3000)

      childCheckout ! PoisonPill

      parentCart.childActorOf(Props[Checkout])

      expectMsg(3 seconds, CheckoutCanceled)
    }

    "Restore PaymentTimeout properly after restart " in {
      val parentCart = TestProbe()
      val childCheckouut = parentCart.childActorOf(Props[Checkout])

      childCheckouut ! DeliveryMethodSelected
      Thread.sleep(3000)

      childCheckouut ! PoisonPill

      parentCart.childActorOf(Props[Checkout])

      expectMsg(3 seconds, CheckoutCanceled)
    }

  }

}
