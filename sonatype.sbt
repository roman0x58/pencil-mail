import xerial.sbt.Sonatype.{GitHubHosting, sonatype01}

usePgpKeyHex("0x47E532CC")

sonatypeProfileName := "io.github.roman0x58"

sonatypeCredentialHost := sonatype01

publishTo := sonatypePublishToBundle.value

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/roman0x58/pencil-mail"))

sonatypeProjectHosting := Some(GitHubHosting("roman0x58", "pencil-mail", "roman.sstu@gmail.com"))

scmInfo := Some(ScmInfo(
  url("https://github.com/roman0x58/pencil-mail"),
  "scm:git@github.com:roman0x58/pencil-mail.git"
))

developers := List(
  Developer(
    id = "minosiants",
    name = "kaspar",
    email = "k@minosiants.com",
    url = url("http://minosiants.com")
  ),
  Developer(
    id = "roman0x58",
    name = "Roman Belikin",
    email = "roman.sstu@gmail.com",
    url = url("https://romanbelikin.com")
  )
)
