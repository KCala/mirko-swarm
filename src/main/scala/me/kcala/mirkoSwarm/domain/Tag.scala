package me.kcala.mirkoSwarm.domain

case class Tag(tag: String) extends AnyVal {
  override def toString: String = tag

  def toStringWithHash: String = s"#$tag"
}