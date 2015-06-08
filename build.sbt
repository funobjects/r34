name := "r34"

organization := "org.funobjects"

scalaVersion := "2.11.6"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

fork := true

libraryDependencies ++= {
  object v {
    val akka        = "2.3.11"
    val akkaHttp    = "1.0-RC3"
    val orientDb    = "2.0.8"
    val nimbus      = "3.9.2"
    val scalatest   = "2.2.4"
    val scalactic   = "2.2.4"
    val json4s      = "3.2.11"
    val rxMongo     = "0.10.5.0.akka23"
    val persist     = "0.9.1"
    val config      = "1.3.0"
  }
  Seq(
    "com.typesafe.akka"     %% "akka-http-core-experimental"        % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-http-experimental"             % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-stream-experimental"           % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-http-spray-json-experimental"  % v.akkaHttp    withSources(),
    "com.typesafe"          %  "config"                             % v.config      withSources(),
    "com.orientechnologies" %  "orientdb-core"                      % v.orientDb    withSources(),
    "com.nimbusds"          %  "nimbus-jose-jwt"                    % v.nimbus      withSources(),
    "org.funobjects"        %%  "akka-persistence-orientdb"         % v.persist     withSources(),
    "org.json4s"            %% "json4s-jackson"                     % v.json4s      withSources(),
    "org.reactivemongo"     %% "reactivemongo"                      % v.rxMongo     withSources(),
    "org.scalactic"         %% "scalactic"                          % v.scalactic   withSources(),
    "com.typesafe.akka"     %% "akka-testkit"                       % v.akka        % "test" withSources(),
    "org.scalatest"         %% "scalatest"                          % v.scalatest   % "test" withSources()
  )
}

