inThisBuild(
  List(
    organization := "com.kubukoz",
    homepage := Some(url("https://github.com/kubukoz/managed")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "kubukoz",
        "Jakub Kozłowski",
        "kubukoz@gmail.com",
        url("https://kubukoz.com")
      )
    )
  )
)

val noPublishPlease = Seq(
  skip in publish := true
)

def crossPlugin(x: sbt.librarymanagement.ModuleID) = compilerPlugin(
  x.cross(CrossVersion.full)
)

val compilerPlugins = List(
  crossPlugin("org.typelevel" % "kind-projector" % "0.11.0"),
  crossPlugin("com.github.cb372" % "scala-typed-holes" % "0.1.5"),
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

val commonSettings = Seq(
  scalaVersion := "2.13.3",
  scalacOptions --= Seq("-Xfatal-warnings"),
  name := "managed",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.2.0",
    "com.codecommit" %% "skolems" % "0.2.1"
  ) ++ compilerPlugins
)

val core = project.settings(commonSettings).settings(name += "-core")

val effect = project
  .settings(
    commonSettings,
    name := "managed-effect",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "2.2.0"
    )
  )
  .dependsOn(core)

val demo =
  project.settings(commonSettings, noPublishPlease).dependsOn(core, effect)

val managed =
  project
    .in(file("."))
    .settings(commonSettings)
    .settings(noPublishPlease)
    .dependsOn(core, effect, demo)
    .aggregate(core, effect, demo)
