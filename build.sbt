name := "r34"

organization := "org.funobjects"

scalaVersion := "2.11.5"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= {
  object v {
    val akka        = "2.3.9"
    val akkaHttp    = "1.0-M4"
    val orientDb    = "2.0"
    val nimbus      = "3.8.2"
    val scalatest   = "2.2.4"
    val scalactic   = "2.2.4"
    val codec       = "1.10"
    val json4s      = "3.2.11"
    val rxMongo     = "0.10.5.0.akka23"
  }
  Seq(
    "com.typesafe.akka"     %% "akka-http-core-experimental"        % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-stream-experimental"           % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-http-experimental"             % v.akkaHttp    withSources(),
    "com.typesafe.akka"     %% "akka-http-spray-json-experimental"  % v.akkaHttp    withSources(),
    "com.orientechnologies" %  "orientdb-core"                      % v.orientDb    withSources(),
    "org.reactivemongo"     %% "reactivemongo"                      % v.rxMongo     withSources(),
    "com.nimbusds"          %  "nimbus-jose-jwt"                    % v.nimbus      withSources(),
    "commons-codec"         %  "commons-codec"                      % v.codec       withSources(),
    "org.json4s"            %% "json4s-jackson"                     % v.json4s      withSources(),
    "org.scalactic"         %% "scalactic"                          % v.scalactic   withSources(),
    "com.typesafe.akka"     %% "akka-testkit"                       % v.akka        % "test" withSources(),
    "org.scalatest"         %% "scalatest"                          % v.scalatest   % "test" withSources()
  )
}

