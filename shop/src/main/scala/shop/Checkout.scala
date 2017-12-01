package shop

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Props, Timers}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import shop.CartManager.{CartTimerExpired, CartTimerExpiredKey}
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
  var lastCheckoutTimerSetTime: Long = _
  var lastPaymentTimerSetTime: Long = _

  override def receive: Receive = SelectingDelivery

  timers.startSingleTimer(CheckoutTimerKey, CheckoutTimeout, new FiniteDuration(30, TimeUnit.SECONDS))

  def SelectingDelivery: Receive = {
    case ShopMessages.CheckoutCanceled => {
      log.info("Received checkout cancel message!")
      persistTimer(canceled = true, "checkout")
      persistTimer(canceled = true, "payment")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case CheckoutTimeout => {
      log.info("Received checkout timeout ")
      persistTimer(canceled = true, "checkout")
      persistTimer(canceled = true, "payment")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case DeliveryMethodSelected => {
      customerRef = sender
      log.info("Delivery method has been selected")
      persistTimer(canceled = false, "payment")
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeout, new FiniteDuration(30, TimeUnit.SECONDS))
      persist(SelectingPaymentMethodState())(persistState)
      context.become(SelectingPaymentMethod)
    }
    case GetState => {
      sender ! SelectingDeliveryState
    }
    case other => {
      log.info("Current state: SelectingDelivery. Unhandled message" + other)
    }
  }

  def SelectingPaymentMethod: Receive = {
    case paymentSelected: PaymentSelected => {
      log.info("Selecting payment method {}", paymentSelected.method)
      this.PaymentServiceRef = context.actorOf(Props[PaymentService], "PaymentService")
      persistTimer(canceled = false, "payment")
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeout, new FiniteDuration(30, TimeUnit.SECONDS))
      customerRef ! PaymentServiceStarted(PaymentServiceRef)
      sender ! PaymentServiceStarted(PaymentServiceRef)
      persist(ProcessingPaymentState())(persistState)
      context.become(ProcesingPayment)
    }
    case SelectingPaymentMethodCanceled => {
      log.info("Canceled selecting payment method")
      persistTimer(canceled = true, "checkout")
      persistTimer(canceled = true, "payment")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case PaymentTimeout => {
      log.info("Received payment tiemeout!")
      persistTimer(canceled = true, "checkout")
      persistTimer(canceled = true, "payment")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case GetState => {
      sender ! SelectingPaymentMethodState
    }
    case other => {
      log.info("Current state: SelectingPaymentMethod. Unhandled message " + other)
    }
  }

  def ProcesingPayment: Receive = {
    case PaymentReceived => {
      log.info("Payment Received!")
      persistTimer(canceled = true, "checkout")
      persistTimer(canceled = true, "payment")
      timers.cancelAll()
      CartRef ! CheckoutClosed
      context.stop(self)
    }
    case PaymentTimeout => {
      log.info("Payment timeout")
      persistTimer(canceled = true, "checkout")
      persistTimer(canceled = true, "payment")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case PaymentCanceled => {
      log.info("Payment Canceled")
      persistTimer(canceled = true, "checkout")
      persistTimer(canceled = true, "payment")
      timers.cancelAll()
      CartRef ! CheckoutCanceled
      context.stop(self)
    }
    case GetState => {
      sender ! ProcessingPaymentState
    }
    case other => {
      log.info("Current state ProcessingPayment. Unhandled message! {}" + other)
    }
  }

  def persistState(state: PossibleCheckoutState): Unit = {
    checkoutState = CheckoutState(state)
    saveSnapshot(checkoutState)
    log.info("\n\n Saved state snapshot " + state.getClass + "\n\n")
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
    case checkoutTimePersistence: CheckoutTimePersistence => {
      lastCheckoutTimerSetTime = System.currentTimeMillis()
      timers.startSingleTimer(CartTimerExpiredKey, CartTimerExpired,
        new FiniteDuration(30 * 1000 - checkoutTimePersistence.remainingTime, TimeUnit.MILLISECONDS))
    }
    case paymentTimePersistence: PaymentTimePersistence => {
      lastPaymentTimerSetTime = System.currentTimeMillis()
      timers.startSingleTimer(PaymentTimerKey, PaymentTimeout,
        new FiniteDuration(30 * 1000 - paymentTimePersistence.remainingTime, TimeUnit.MILLISECONDS))
    }
    case SnapshotOffer(_, snapshot: Any) => {
      log.info("Unknown Snapshot!" + snapshot)
    }

  }

  def persistTimer(canceled: Boolean, timer: String): Unit = {
    log.info("Persist timer call for canceled: {}, timer : {}", canceled, timer)
    val currentTime = if (canceled) -1 else System.currentTimeMillis()
    if (currentTime != -1) {
      timer match {
        case "checkout" => {
          persist(CheckoutTimePersistence(currentTime - lastCheckoutTimerSetTime, timer)) { event =>
            log.info("Persisting checkoutTimePersistence {}", event)
          }
        }
        case "payment" => {
          persist(PaymentTimePersistence(currentTime - lastPaymentTimerSetTime, timer)) { event =>
            log.info("Persisitng paymentTimePersistence {}", event)
          }
        }
        case other => log.info("Unknown timer {}", other)
      }
    }
  }


  override def receiveCommand: Receive = SelectingDelivery

  override def persistenceId: String = this.id
}

object Checkout {
  def props(id: String): Props = Props(new Checkout(id))

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

  sealed trait TimePersistence

  case class CheckoutTimePersistence(remainingTime: Long, timer: String) extends TimePersistence

  case class PaymentTimePersistence(remainingTime: Long, timer: String) extends TimePersistence

}