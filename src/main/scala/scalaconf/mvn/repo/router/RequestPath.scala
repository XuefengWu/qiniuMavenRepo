package scalaconf.mvn.repo.router

import akka.actor.ActorRef

/**
 * Component:
 * Description:
 * Date: 2014/11/19
 * @author Andy Ai
 */
case class RequestPath(origin: ActorRef, path: String)
