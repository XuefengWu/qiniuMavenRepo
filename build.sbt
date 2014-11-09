name := "qiniu Maven Repo"

version := "1.0"

scalaVersion := Version.scala

//resolvers += "Qiniu Snapshots" at "http://localhost:9000/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies ++= Dependencies.mvnRepo
