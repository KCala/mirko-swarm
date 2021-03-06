package kcala.mirkoSwarm.json

import java.time.LocalDateTime

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import kcala.mirkoSwarm.model.{Entry, Sex, SwarmError, Tag}
import kcala.mirkoSwarm.wykop.{MirkoEntry, MirkoError}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsString, JsValue, JsonFormat, NullOptions, RootJsonFormat}


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol with NullOptions {

  /**
    * Custom format for Wykop API.
    * It's needed since Wykop API is retarded and sometimes represents no data
    * as null, and sometimes as empty string
    */
  //noinspection NotImplementedCode
  implicit object MirkoEntryFormat extends RootJsonFormat[MirkoEntry] {
    override def read(json: JsValue): MirkoEntry = json match {
      case JsObject(fields) =>
        MirkoEntry(
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

    override def write(obj: MirkoEntry): JsValue = ???
  }

  implicit object MirkoErrorFormat extends RootJsonFormat[MirkoError] {
    override def read(json: JsValue): MirkoError = json match {
      case JsObject(fields) =>
        val code +: message +: _ = fields("error").asJsObject().getFields("code", "message")
        MirkoError(code.convertTo[Int], message.convertTo[String])
      case other => spray.json.deserializationError("Can't even parse error properly from Wykop")
    }

    override def write(obj: MirkoError): JsValue = ???
  }

  implicit object MirkoResponseFormat extends RootJsonFormat[Either[MirkoError, Seq[MirkoEntry]]] {
    override def read(json: JsValue): Either[MirkoError, Seq[MirkoEntry]] = json match {
      case JsObject(fields) =>
        if (fields.contains("error")) Left(json.convertTo[MirkoError]) else Right(json.convertTo[Seq[MirkoEntry]])
      case other => spray.json.deserializationError("Can't parse response from Wykop API. Must be really weird stuff.")
    }

    override def write(obj: Either[MirkoError, Seq[MirkoEntry]]): JsValue = ???
  }

  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {

    override def write(obj: LocalDateTime): JsValue = JsString(obj.toString)

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(string) => LocalDateTime.parse(string)
      case _ => throw DeserializationException("invalid time format")
    }
  }

  implicit object TagFormat extends JsonFormat[Tag] {
    override def write(obj: Tag): JsValue = JsString(obj.tag)

    override def read(json: JsValue): Tag = json match {
      case JsString(s) => Tag(s)
      case _ => throw DeserializationException("invalid tag format")
    }
  }

  implicit val sexFormat = new EnumJsonConverter(Sex)

  implicit val entryFormat = jsonFormat3(Entry)

  implicit val swarmErrorFormat = jsonFormat1(SwarmError)

}
