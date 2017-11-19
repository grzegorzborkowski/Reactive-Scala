package shop

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import shop.Checkout._
import shop.ShopMessages.{CheckoutCanceled, CheckoutClosed}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

case class CheckoutState(stateName: PossibleCheckoutState)

class Checkout(id: String) extends PersistentActor with Timers {
  def this() = this(new Random(System.currentTimeMillis).alphanumeric.take(10).mkString)

  val log = Logging(context.system, this)
  val CartRef: ActorRef = context.parent
  var PaymentServiceRef: ActorRef = _
  var customerRef: ActorRef = _
  var checkoutState = CheckoutState(SelectingDeliveryState())
  persistState(SelectingDeliveryState())

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
    case DeliveryMethodSelected => {
      customerRef = sender
      log.info("Delivery method has been selected")
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeout, new FiniteDuration(5, TimeUnit.SECONDS))
      persistState(SelectingPaymentMethodState())
      context.become(SelectingPaymentMethod)
    }
    case GetState => {
      sender ! SelectingDeliveryState
    }
    case other => {
      log.info("Unhandled message" + other)
    }
  }

  def SelectingPaymentMethod: Receive = {
    case PaymentSelected => {
      log.info("Selecting payment method")
      this.PaymentServiceRef = context.actorOf(Props[PaymentService], "PaymentService")
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeout, new FiniteDuration(5, TimeUnit.SECONDS))
      customerRef ! PaymentServiceStarted(PaymentServiceRef)
      persistState(ProcessingPaymentState())
      context.become(ProcesingPayment)
    }
    case SelectingPaymentMethodCanceled => {
      log.info("Canceled selecting payment method")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case PaymentTimeout => {
      log.info("Received payment tiemeout!")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case GetState => {
      sender ! SelectingPaymentMethodState
    }
    case other => {
      log.info("Unhandled message " + other)
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
    case GetState => {
      sender ! ProcessingPaymentState
    }
    case other => {
      log.info("Unhandled message!")
    }
  }

  def persistState(state: PossibleCheckoutState): Unit = {
    checkoutState = CheckoutState(state)
    saveSnapshot(checkoutState)
  }

  override def receiveRecover: Receive = LoggingReceive {
    case RecoveryCompleted => log.info("Recovery completed")
    case SnapshotOffer(_, snapshot: CheckoutState) => {
      log.info("Received snapshottOffer {}", snapshot)
      checkoutState = snapshot
      checkoutState.stateName match {
        case SelectingDeliveryState() => context become SelectingDelivery
        case SelectingPaymentMethodState() => context become SelectingPaymentMethod
        case ProcessingPaymentState() => context become ProcesingPayment
      }
    }
  }

  override def receiveCommand: Receive = SelectingDelivery

  override def persistenceId: String = this.id
}

object Checkout {
  case class CheckoutTimeout()
  case class DeliveryMethodSelected()
  case class PaymentSelected(method: String)
  case class SelectingPaymentMethodCanceled()
  case class PaymentReceived()
  case class PaymentTimeout()
  case class PaymentCanceled()

  case class PaymentServiceStarted(paymentServiceRef: ActorRef)

  case object CheckoutTimerKey
  case object PaymentTimerKey

  sealed trait PossibleCheckoutState
  case class SelectingDeliveryState() extends PossibleCheckoutState
  case class SelectingPaymentMethodState() extends PossibleCheckoutState
  case class ProcessingPaymentState() extends PossibleCheckoutState

  case class GetState()

}