name := "r34"

version := "0.9.0-SNAPSHOT"

lazy val commonSettings = Seq(
  organization := "org.funobjects",
  scalaVersion := "2.11.7",
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

fork := true

lazy val commonDependencies = {
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

// Lazy vals aren't lazy enough for aggregation of subprojects that depend on the root.
// With SBT (as of 0.13.9) this common pattern requires use of LocalProject to avoid a circular dependency.
// Note the argument to LocalProject is the project name, not the location.
lazy val multiModuleRef = LocalProject("multiModule")

lazy val invalidModuleRef = LocalProject("invalidModule")

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .aggregate(multiModuleRef, invalidModuleRef)

lazy val multiModule = (project in file("testModules") / "multiModule")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(root)

lazy val invalidModule = (project in file("testModules") / "invalidModule")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(root)
