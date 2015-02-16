name := "r34"

organization := "org.funobjects"

scalaVersion := "2.11.5"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= {
  object v {
    val akka        = "1.0-M3"
    val orientDb    = "2.0"
    val nimbus      = "3.8.2"
    val scalatest   = "2.2.4"
    val scalactic   = "2.2.4"
    val codec       = "1.10"
  }
  Seq(
    "com.typesafe.akka"     %% "akka-http-core-experimental"        % v.akka        withSources(),
    "com.typesafe.akka"     %% "akka-stream-experimental"           % v.akka        withSources(),
    "com.typesafe.akka"     %% "akka-http-experimental"             % v.akka        withSources(),
    "com.typesafe.akka"     %% "akka-http-spray-json-experimental"  % v.akka        withSources(),
    "com.orientechnologies" %  "orientdb-core"                      % v.orientDb    withSources(),
    "com.nimbusds"          %  "nimbus-jose-jwt"                    % v.nimbus      withSources(),
    "commons-codec"         %  "commons-codec"                      % v.codec       withSources(),
    "org.scalactic"         %% "scalactic"                          % v.scalactic   withSources(),
    "org.scalatest"         %% "scalatest"                          % v.scalatest   % "test" withSources()
  )
}

