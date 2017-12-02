package paymentservers

object PaymentServerExceptions {

  case class InternalServerErrorException(private val message: String = "",
                                          private val cause: Throwable = None.orNull)
    extends Throwable(message, cause)

  case class NetworkException(private val message: String = "",
                              private val cause: Throwable = None.orNull)
    extends Throwable(message, cause)

}