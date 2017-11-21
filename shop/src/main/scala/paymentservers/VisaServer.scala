package paymentservers

import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.io.StdIn

class VisaServer(systemParam: ActorSystem) extends Actor {
  implicit val system = systemParam
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val route: Route =
    path("visa-payment") {
      get {
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`,
          "Payment OK"))
      }
    }

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
  }
}
