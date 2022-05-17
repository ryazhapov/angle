scalacOptions += "-Ymacro-annotations"

lazy val root = (project in file("."))
  .settings(
    name := "angle",
    version := "0.1",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Dependencies.zio,
    libraryDependencies ++= Dependencies.zioConfig,
    libraryDependencies ++= Dependencies.doobie,
    libraryDependencies ++= Dependencies.http4s,
    libraryDependencies ++= Dependencies.circe,
    libraryDependencies ++= Dependencies.security,
    libraryDependencies ++= Seq(
      Dependencies.liquibase,
      Dependencies.postgres,
      Dependencies.logback,
    ))

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))