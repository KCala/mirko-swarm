package me.kcala.mirkoSwarm.model

case class Tag(tag: String) extends AnyVal {
  override def toString: String = tag

  def toStringWithHash: String = s"#$tag"
}