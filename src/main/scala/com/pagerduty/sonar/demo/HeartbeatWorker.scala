/*
 * Copyright (c) 2014, PagerDuty
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.pagerduty.sonar.demo

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import com.pagerduty.sonar.Supervisor.Heartbeat
import scala.concurrent.forkjoin.ThreadLocalRandom


/**
 * @author Aleksey Nikiforov
 */
object HeartbeatWorker {
  case object Tick
}


/**
 * This actor simulates a single client emitting heartbeats.
 */
class HeartbeatWorker(val endpoint: ActorRef, val address: Int) extends Actor with ActorLogging {
  import HeartbeatWorker._
  import context.dispatcher

  def random(): Double = ThreadLocalRandom.current.nextDouble()
  val heartbeatIntervalMs = context.system.settings.config.getInt("com.pagerduty.sonar.heartbeatIntervalMs")

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce((random()*heartbeatIntervalMs).millis)(self ! Tick)
  }
  override def postRestart(reason: Throwable): Unit = {
    // Prevents preStart() being call on restart.
  }


  def receive = {
    case Tick =>
      context.system.scheduler.scheduleOnce(heartbeatIntervalMs.millis)(self ! Tick)

      if (random() > Demo.missingHeartbeatChance) {
        endpoint ! Heartbeat(address, System.currentTimeMillis)
      }
  }
}
