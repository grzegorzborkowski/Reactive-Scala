package productcatalog

import akka.actor.{Actor, Props}
import com.github.tototoshi.csv._
import shop.Item
import java.net.URI

import akka.event.Logging
import productcatalog.ProductCatalog.GetElements

class ProductCatalog extends Actor {
  val productCatalogStorage = context.actorOf(Props[ProductCatalogStorage])
  val log = Logging(context.system, this)

  override def receive = {
    case getElements: GetElements => {
      productCatalogStorage forward getElements
    }
    case other => log.info("Received unhandled message {}", other)
  }

}

class ProductCatalogStorage extends Actor {
  var products = collection.mutable.Map[String, Item]()
  val sourceReader = scala.io.Source.fromResource("data/data.csv")
  val reader = CSVReader.open(sourceReader)
  val log = Logging(context.system, this)

  val itemsInCatalog: Stream[Item] = getProductCatalog()
  itemsInCatalog.foreach(item => {
    products += (item.name -> item)
  })

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
      val distance_map = products.toSeq.map(entry =>
        (Util.Util.occurences(entry._1, element),
          entry._2))
     val sorted = distance_map.sortWith(_._1 > _._1)
     val first_10 = sorted.take(10)
     sender ! first_10
    }
    case other => {
      log.info("Unknown message! {}", other)
    }
  }
}

object ProductCatalog {
  case class GetElements(name: String)
}