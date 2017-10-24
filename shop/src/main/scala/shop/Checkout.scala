package shop

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Timers}
import akka.event.Logging
import shop.Checkout._
import shop.ShopMessages.{CheckoutCanceled, CheckoutClosed}

import scala.concurrent.duration.FiniteDuration

class Checkout extends Actor with Timers {
  val log = Logging(context.system, this)
  val CartRef: ActorRef = context.parent

  override def receive: Receive = SelectingDelivery

  timers.startSingleTimer(CheckoutTimerKey, CheckoutTimeout, new FiniteDuration(5, TimeUnit.SECONDS))

  def SelectingDelivery: Receive = {
    case ShopMessages.CheckoutCanceled => {
      log.info("Received checkout cancel message!")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case CheckoutTimeout => {
      log.info("Received checkout timeout ")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case DeliverySelected => {
      log.info("Delivery method has been selected")
      context.become(SelectingPaymentMethod)
    }
    case other => {
      log.info("Unhandled message")
    }
  }

  def SelectingPaymentMethod: Receive = {
    case PaymentMethod => {
      log.info("Selecting payment method")
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeout, new FiniteDuration(5, TimeUnit.SECONDS))
      context.become(ProcesingPayment)
    }
    case SelectingPaymentMethodCanceled => {
      log.info("Canceled selecting payment method")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case PaymentTimeout => {
      log.info("Received payment method")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case other => {
      log.info("Unhandled message")
    }
  }

  def ProcesingPayment: Receive = {
    case PaymentReceived => {
      log.info("Payment Received!")
      timers.cancelAll()
      CartRef ! CheckoutClosed
      context.stop(self)
    }
    case PaymentTimeout => {
      log.info("Payment timeout")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case PaymentCanceled => {
      log.info("Payment Canceled")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case other => {
      log.info("Unhandled message!")
    }
  }

}

object Checkout {
  case class CheckoutTimeout()
  case class DeliverySelected()
  case class PaymentMethod(method: String)
  case class SelectingPaymentMethodCanceled()
  case class PaymentReceived()
  case class PaymentTimeout()
  case class PaymentCanceled()

  case object CheckoutTimerKey
  case object PaymentTimerKey
}