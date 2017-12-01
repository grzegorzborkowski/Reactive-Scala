package paymentservers

import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.{Restart, Stop}

trait PaymentServerSupervision {

  def paymentSupervisionStrategy = OneForOneStrategy(loggingEnabled = true) {
    case serverException: InternalServerErrorException => {
      Restart
    }
    case networkException: NetworkException => {
      Restart
    }
    case e => {
      Stop
    }
  }
}


case class InternalServerErrorException(private val message: String = "",
                                        private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

case class NetworkException(private val message: String = "",
                            private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

