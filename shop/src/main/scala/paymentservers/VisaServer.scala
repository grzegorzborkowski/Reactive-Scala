package paymentservers

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.stream.ActorMaterializer
import org.scalatra.InternalServerError
import paymentservers.PaymentServerExceptions.InternalServerErrorException
import shop.ShopMessages.DoPayment

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Random

class VisaServerSupervisor(systemParam: ActorSystem) extends Actor
  with ActorLogging {
  implicit val system = systemParam
  // here we may duplicate those servers
  val visaServerProps = Props(new VisaServer(systemParam))

  val supervisor = BackoffSupervisor.props(
    Backoff.onFailure(
      childProps = visaServerProps,
      childName = "VisaServer",
      minBackoff = 3 seconds,
      maxBackoff = 30 seconds,
      randomFactor = 0.2
    ).withAutoReset(10.seconds)
      .withSupervisorStrategy(
        OneForOneStrategy() {
          case exception =>  {
            log.info("Unknown exception: {}, occured. Restarting ...", exception)
            SupervisorStrategy.Restart
          }
        }
      )
  )

  val visaServer = system.actorOf(supervisor)

  override def receive: Receive = {
    case DoPayment() => {
      visaServer ! DoPayment()
    }
    case "start" => {
      visaServer ! "start"
      context.sender ! "OK"
    }
    case anyMessage: Any => {
      visaServer ! anyMessage
    }
  }
}

// TODO: if fails escalate to Supervisor
// TODO: in Application and PaymentServices wherever VisaServer was used
// TODO: VisaServerSupervisor should be used
class VisaServer(systemParam: ActorSystem) extends Actor with
  ActorLogging {

  implicit val system = systemParam
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val route: Route = {
  //  handleExceptions(exceptionHandler) {
      path("visa-payment") {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`,
            "Payment OK"))
        }
      }

      path("visa-payment-danger") {
        get {
          failWith(PaymentServerExceptions.
            InternalServerErrorException())
          throw new RuntimeException("blah blah")
        }
      }
    }
//  }

  val port = 8080
  val bindingFuture = Http().bindAndHandle(route, "localhost", port)

  println("Visa server running on port {}", port)
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

  override def receive = {
    case "start" => {
        context.sender ! "OK"
    }
    case exception: Exception => {
      log.info("Received exception : {} in VisaServer. Escalating",
        exception)
      Escalate
    }
  }
}
