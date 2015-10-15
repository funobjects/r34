name := "r34"

organization := "org.funobjects"

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

fork := true

libraryDependencies ++= {
  object v {
    val akka        = "2.4.0"
    val akkaHttp    = "1.0"
    val orientDb    = "2.1.3"
    val nimbus      = "3.9.2"
    val scalatest   = "2.2.4"
    val scalactic   = "2.2.4"
    val json4s      = "3.2.11"
    val persist     = "1.0.0-RC2"
    val config      = "1.3.0"
  }
  Seq(
    "com.typesafe.akka"     %% "akka-actor"                         % v.akka withSources(),
    "com.typesafe.akka"     %% "akka-http-core-experimental"        % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-http-experimental"             % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-stream-experimental"           % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-http-spray-json-experimental"  % v.akkaHttp    withSources(),
    "com.typesafe"          %  "config"                             % v.config      withSources(),
    "com.orientechnologies" %  "orientdb-core"                      % v.orientDb    withSources(),
    "com.nimbusds"          %  "nimbus-jose-jwt"                    % v.nimbus      withSources(),
    "org.funobjects"        %%  "akka-persistence-orientdb"         % v.persist     withSources(),
    "org.json4s"            %% "json4s-jackson"                     % v.json4s      withSources(),
    "org.scalactic"         %% "scalactic"                          % v.scalactic   withSources(),
    "com.typesafe.akka"     %% "akka-testkit"                       % v.akka        % "test" withSources(),
    "org.scalatest"         %% "scalatest"                          % v.scalatest   % "test" withSources()
  )
}

