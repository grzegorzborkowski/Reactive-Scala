package productcatalog

import akka.actor.Actor

class ProductCatalog extends Actor {

  override def receive = {
    case any => {
      context.sender ! "Hello from Product Catalog"
    }
  }

}
