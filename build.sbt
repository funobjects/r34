name := "r34"

organization := "org.funobjects"

scalaVersion := "2.11.4"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= {
  val akkaVersion     = "1.0-M2"
  val orientDbVersion = "2.0"
  Seq(
    "com.typesafe.akka" %% "akka-stream-experimental"     % akkaVersion withSources(),
    "com.typesafe.akka" %% "akka-http-experimental"       % akkaVersion withSources(),
    "com.typesafe.akka" %% "akka-http-core-experimental"  % akkaVersion withSources(),
    "com.orientechnologies" % "orientdb-core"             % orientDbVersion withSources(),
    "org.scalatest"     %% "scalatest"                    % "2.2.3" % "test" 
  )
}

