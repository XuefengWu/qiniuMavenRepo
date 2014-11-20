package scalaconf.mvn.repo.router

import akka.actor.{ActorSystem, Props}
import akka.http.model.Uri
import akka.testkit._
import org.scalatest.{Matchers, WordSpecLike}

import scalaconf.mvn.repo.{ForwardingActor, StopSystemAfterAll}

class RequestRouterTest extends TestKit(ActorSystem("testsystem"))  with WordSpecLike with Matchers with StopSystemAfterAll {


  "A Request Router Actor" must {

    "/com/typesafe/akka/akka-agent_2.11/2.3.6/akka-agent_2.11-2.3.6.jar" in {
      val props = Props(classOf[ForwardingActor], testActor)
      val uri = Uri("/com/typesafe/akka/akka-agent_2.11/2.3.6/akka-agent_2.11-2.3.6.jar")
      val router = system.actorOf(Props(new RequestRouter(Map(), props)))
      router ! uri
      expectMsgPF(){
        case RequestPath(_, path) if path == "/com/typesafe/akka/akka-agent_2.11/2.3.6/akka-agent_2.11-2.3.6.jar" => true
      }
    }

    "/search/com/apache" in {
      val props = Props(classOf[ForwardingActor], testActor)
      val uri = Uri("/search/com/apache")
      val routes = Map("/search/" -> props)
      val router = system.actorOf(Props(new RequestRouter(routes, TestActors.echoActorProps)))
      router ! uri
      expectMsgPF(){
        case RequestPath(_, path) if path == "com/apache" => true
      }
    }

  }

}
