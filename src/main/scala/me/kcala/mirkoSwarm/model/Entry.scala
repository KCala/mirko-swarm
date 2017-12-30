package me.kcala.mirkoSwarm.model

import java.time.LocalDateTime

import me.kcala.mirkoSwarm.model.Sex.Sex

case class Entry(
                dateTime: LocalDateTime,
                tags: Seq[Tag],
                authorsSex: Sex
                )