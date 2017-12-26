package me.kcala.mirkoSwarm.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import me.kcala.mirkoSwarm.wykop.Entry
import spray.json.{DefaultJsonProtocol, JsNull, JsObject, JsString, JsValue, JsonReader, NullOptions, RootJsonFormat, RootJsonReader}


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol with NullOptions {

  /**
    * Custom format for Wykop API.
    * It's needed since Wykop API is retarded and sometimes represents no data
    * as null, and sometimes as empty string
    */
  //noinspection NotImplementedCode
  implicit object entryFormat extends RootJsonFormat[Entry] {
    override def read(json: JsValue): Entry = json match {
      case JsObject(fields) =>
        Entry(
          fields("id").convertTo[Long],
          fields("author").convertTo[String],
          fields.get("author_sex").map(_.convertTo[String]).filter(_.nonEmpty),
          fields("date").convertTo[String],
          fields("body").convertTo[String],
          fields("url").convertTo[String],
          fields("app") match {
            case JsString(s) => Some(s)
            case other => None
          }
        )
      case other => spray.json.deserializationError("Invalid response from Wykop")
    }

    override def write(obj: Entry): JsValue = ???
  }

}
