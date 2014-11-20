package scalaconf.mvn.repo.handler

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{Matchers, WordSpecLike}

import scalaconf.mvn.repo.StopSystemAfterAll

class RepoProxyTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
                    with WordSpecLike with Matchers with StopSystemAfterAll {


  "An RepoProxy actor" must {

    "/search/com/apache" in {
     assert(true)
    }

  }
}

