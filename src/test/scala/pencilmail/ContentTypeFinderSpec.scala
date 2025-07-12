package pencilmail
import data.*
import protocol.*
import ContentType.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalacheck.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.nio.file.{Path, Paths}

class ContentTypeFinderSpec extends Specification with ScalaCheck {
  import ContentTypeFinderSpec.*

  "ContentTypeFinder" should {

    "find content type" in forAll(pathGen) { case (p, t) =>
      ContentTypeFinder.findType[IO](p).attempt.unsafeRunSync() ==== Right(t)
    }

    "not find file" in {
      val file = Paths.get("files/!!!jpeg-sample.jpg")
      ContentTypeFinder.findType[IO](file)
        .attempt
        .unsafeRunSync() must beLeft(Error.ResourceNotFound(file.toString))
    }
  }

}

object ContentTypeFinderSpec {
  def path(filename: String): Path =
    Paths.get(getClass.getClassLoader.getResource(filename).toURI)

  val files = List(
    ("files/ascii-sample.txt", `text/plain`),
    ("files/html-sample.html", `text/html`),
    ("files/image-sample.png", `image/png`),
    ("files/gif-sample.gif", `image/gif`),
    ("files/jpeg-sample.jpg", `image/jpeg`),
    ("files/rfc2045.pdf", `application/pdf`)
  ).map { case (f, t) => (path(f), t) }

  val pathGen: Gen[(Path, ContentType)] = Gen.oneOf(files)
}
