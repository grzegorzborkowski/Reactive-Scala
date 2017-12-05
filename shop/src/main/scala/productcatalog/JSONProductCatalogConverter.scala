package productcatalog


import play.api.libs.json._
import shop.Item
object JSONProductCatalogConverter {

  implicit val uriReads = Reads{ js => js match {
      case JsString(s) => JsSuccess(java.net.URI.create(s))
      case _ => JsError("JsString expected to convert to URI")
    } }
    implicit val uriWrites = Writes{ uri: java.net.URI => JsString(uri.toString) }

  implicit val itemFormat = Json.format[Item]
  implicit val itemOccurencesFormat = Json.format[ItemOccurences]


}