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
=======
### SBT

```
resolvers ++= Seq(
  "Scala Conf Repositories" at "http://repo.scalaconf.com/releases"
)
```
Your can see more details in [here](http://www.scala-sbt.org/0.13/docs/Resolvers.html)

and

[here](http://www.scala-sbt.org/0.13/docs/Proxy-Repositories.html)

### Maven

```xml
<project>
...
  <repositories>
    <repository>
      <id>scalaconf-repo</id>
      <name>Scalaconf Repository</name>
      <url>http://repo.scalaconf.com/releases</url>
    </repository>
  </repositories>
...
</project>
```

## Troubleshooting

- List all fails: http://repo.scalaconf.com/fails
- Refresh artifacts: http://repo.scalaconf.com/refresh/:id
