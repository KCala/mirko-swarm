package me.kcala.mirkoSwarm.domain

object Sex extends Enumeration {
  type Sex = Value
  val Male = Value("male")
  val Female = Value("female")
  val Unspecified = Value("unspecified")
}
