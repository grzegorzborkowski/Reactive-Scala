package productcatalog

import java.net.URI

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import com.github.tototoshi.csv._
import productcatalog.ProductCatalog.GetElements
import shop.Item

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn





import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

import productcatalog.JSONProductCatalogConverter._
import play.api.libs.json._




class ProductCatalog(systemParam: ActorSystem) extends Actor {

  implicit val system = systemParam
  val log = Logging(context.system, this)
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val executionContext = system.dispatcher
  val catalog = system.actorOf(Props(new ProductCatalogRouter(10)))
  implicit val timeout = Timeout(120 seconds)

  val route = {
    path("query" / """\w+""".r) {
      str => {
        log.info("Received request for : {}", str)
        val futureItems = catalog ? GetElements(str)
        val response = Await.result(futureItems, timeout.duration).asInstanceOf[List[ItemOccurences]]
        complete(HttpEntity(ContentTypes.`application/json`, Json.obj("items" -> response).toString()))
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 7777)



  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

  override def receive = {
    case str: String => {
      log.info(str)
    }
    case other => log.info("Received unhandled message {}", other)
  }

}

class ProductCatalogRouter(numberOfWorkers: Int) extends Actor {
  val routees = Vector.fill(numberOfWorkers) {
    val r = context.actorOf(Props(new ProductCatalogStorage(self)))
    context watch r
    ActorRefRoutee(r)
  }

  var router = {
    Router(RoundRobinRoutingLogic(), routees)
  }

  override  def receive = {
    case getElements: GetElements => {
      router.route(getElements, sender())
    }
  }
}

class ProductCatalogStorage(returnSource: ActorRef) extends Actor {
  var products = collection.mutable.Map[String, Item]()
  val sourceReader = scala.io.Source.fromResource("data/data.csv")
  val reader = CSVReader.open(sourceReader)
  val log = Logging(context.system, this)

  val itemsInCatalog: Stream[Item] = getProductCatalog()
  itemsInCatalog.foreach(item => {
    products += (item.name -> item)
  })

  print(itemsInCatalog.toList.map(p => p.name.split(" ") + ","))
  def getProductCatalog(): Stream[Item] = {
    val lines = scala.io.Source.fromResource("data/data.csv").getLines
    lines.toStream.tail
      .map(
        line => line.split(",").map(_.replace("\"", ""))
      )
      .filter(x => x.length > 2)
      .map(
        x => {
          val name = x(1) + " " + x(2)
          val uri = URI.create(x(0))
          val price = x(2).length
          Item(uri, name.trim, price, name.length)
        }
      )
      .filterNot(x => x.name contains "NULL")
  }

  override def receive = {
    case GetElements(element) => {
//      log.info("SENDER :", parent)
      log.info("Received GetElemenets query")
      print(itemsInCatalog.map(p => p.name))
      val distance_map = products.toSeq.map(entry =>
        (Util.Util.occurences(entry._1, element),
          entry._2))
     val sorted = distance_map.sortWith(_._1 > _._1)
     val first_10 = sorted.take(10).map(p => ItemOccurences(p._1, p._2)).toList
     log.info("Response {}", first_10)
     returnSource ! first_10
     sender ! first_10
     context.parent ! first_10
    }
    case other => {
      log.info("Unknown message! {}", other)
    }
  }
}

object ProductCatalog {
  case class GetElements(name: String)
}

case class ItemOccurences(occurence: Int, item: Item)

case class ItemOccurencesList(itemsOcurrences: List[ItemOccurences])