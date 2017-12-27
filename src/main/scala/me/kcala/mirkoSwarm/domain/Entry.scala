package me.kcala.mirkoSwarm.domain

import java.time.LocalDateTime

import me.kcala.mirkoSwarm.domain.Sex.Sex

case class Entry(
                dateTime: LocalDateTime,
                tags: Seq[Tag],
                authorsSex: Sex
                )