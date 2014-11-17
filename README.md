usage:
===
build.sbt

`
resolvers += "ScalaConf Repository" at "http://repo.scalaconf.com/"
`

or Build.scala

`
resolvers ++= Seq( Resolver.url("ScalaConf Repository", url("http://repo.scalaconf.com")), Resolver.mavenLocal)
`

TODO:
===
repo list page
auto try failed request again after days
