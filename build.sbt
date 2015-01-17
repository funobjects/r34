name := "r34"

organization := "org.funobjects"

scalaVersion := "2.11.4"

libraryDependencies ++= {
  val akkaVersion = "1.0-M2"
  Seq(
    "com.typesafe.akka" %% "akka-stream-experimental"     % akkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental"       % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental"  % akkaVersion,
    "org.scalatest"     %% "scalatest"                    % "2.2.3"       % "test" 
  )
}

