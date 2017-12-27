package me.kcala.mirkoSwarm.wykop

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import me.kcala.mirkoSwarm.domain.{Entry, Sex, Tag}
import spray.json.{JsValue, RootJsonFormat}

/**
  * Represents JSON response format from Wykop API at
  * http://a.me.kcala.mirkoSwarm.wykop.pl/stream/index endpoint
  *
  * Only relevant fields are listed
  */
case class MirkoEntry(
                       id: Long,
                       author: String,
                       author_sex: Option[String],
                       date: String,
                       body: String,
                       url: String,
                       app: Option[String],
                     )

object MirkoEntry {
  private val TagRegex = "#<a href=\"#(.*?)\"".r

  def convertToEntry(mirkoEntry: MirkoEntry): Entry = {
    Entry(
      LocalDateTime.parse(mirkoEntry.date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
      extractTags(mirkoEntry),
      mirkoEntry.author_sex.map(s => Sex.withName(s)).getOrElse(Sex.Unspecified)
    )
  }

  def extractTags(mirkoEntry: MirkoEntry): Seq[Tag] =
    TagRegex.findAllIn(mirkoEntry.body).matchData.map(_.group(1)).map(Tag).toList
}