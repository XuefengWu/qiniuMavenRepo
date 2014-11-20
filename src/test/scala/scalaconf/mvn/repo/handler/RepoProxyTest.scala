package scalaconf.mvn.repo.handler

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import org.mockito.Mockito._

import scalaconf.mvn.repo.router.RequestPath
import scalaconf.mvn.repo.store.FetchStoreDao
import scalaconf.mvn.repo.{ForwardingActor, StopSystemAfterAll}

class RepoProxyTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
                    with WordSpecLike with Matchers with StopSystemAfterAll with MockitoSugar {


  "An RepoProxy actor" must {

    "fetch scala.jar" in {
      val dao = mock[FetchStoreDao]
      when(dao.get("scala.jar")).thenReturn(None)
      val props = Props(classOf[ForwardingActor], testActor)
      val proxy = system.actorOf(Props(new RepoProxy("http://mavenrepo.qiniudn.com", dao, props)))
      proxy ! RequestPath(testActor, "scala.jar")
      expectMsgPF(){
        case ArtifactUri(_, path) if path == "scala.jar" => true
      }
    }

  }
}

