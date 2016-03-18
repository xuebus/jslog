name := "jslog"

version := "1.0"

scalaVersion := "2.10.5"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")

val AkkaVersion = "2.3.14"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion,
  "org.scalatest" % "scalatest_2.10" % "3.0.0-M15"
)

doc in Compile <<= target.map(_ / "none")

fork in Test := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2/")
}