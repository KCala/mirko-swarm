package me.kcala.mirkoSwarm.wykop

import spray.json.{JsValue, RootJsonFormat}

/**
  * Represents JSON response format from Wykop API at
  * http://a.me.kcala.mirkoSwarm.wykop.pl/stream/index endpoint
  *
  * Only relevant fields are listed
  */
case class Entry(
                  id: Long,
                  author: String,
                  author_sex: Option[String],
                  date: String,
                  body: String,
                  url: String,
                  app: Option[String],
                )
