package scalaconf.mvn.repo

import akka.actor.{Actor, ActorRef}

/**
 * An Actor that forwards every message to a next Actor
 */
class ForwardingActor(next: ActorRef) extends Actor {
  def receive = {
    case msg => next ! msg
  }
}
