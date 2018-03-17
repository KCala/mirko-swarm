package me.kcala.mirkoSwarm.wykop

import me.kcala.mirkoSwarm.model.SwarmError

case class MirkoError(
                       code: Integer,
                       message: String
                     )

object MirkoError {
  def convertToSwarmError(error: MirkoError): SwarmError = SwarmError(error.message)

  val RequestQuotaExceeded = 5
}
