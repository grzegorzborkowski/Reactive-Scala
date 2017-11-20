package shop

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.Checkout.DeliveryMethodSelected
import shop.ShopMessages.CheckoutCanceled

import scala.concurrent.duration._
import scala.util.Random

class CheckoutPersistenceTimersTests extends TestKit(ActorSystem("CheckoutPersistenceTimersTests"))
  with WordSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Checkout" should {
    "Restore CheckoutTimer properly after restart " in {
      val parentCart = TestProbe()
      val checkoutID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString

      val childCheckout = parentCart.childActorOf(Props(new Checkout(checkoutID)))

      Thread.sleep(3000)

      childCheckout ! PoisonPill

      parentCart.childActorOf(Props(new Checkout(checkoutID)))

      expectMsg(3 seconds, CheckoutCanceled)
    }

    "Restore PaymentTimeout properly after restart " in {
      val parentCart = TestProbe()
      val checkoutID = new Random(System.currentTimeMillis).alphanumeric.take(10).mkString
      val childCheckout = parentCart.childActorOf(Props(new Checkout(checkoutID)))

      childCheckout ! DeliveryMethodSelected
      Thread.sleep(3000)

      childCheckout ! PoisonPill

      parentCart.childActorOf(Checkout.props(checkoutID))

      expectMsg(3 seconds, CheckoutCanceled)
    }

  }

}
