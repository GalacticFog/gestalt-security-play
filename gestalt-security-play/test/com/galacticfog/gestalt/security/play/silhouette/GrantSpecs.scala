package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api.{GestaltAccount, GestaltDirectory, GestaltRightGrant}
import com.galacticfog.gestalt.security.play.silhouette.authorization.{hasGrant, hasValue, matchesGrant, matchesValue}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import play.api.test.PlaySpecification


@RunWith(classOf[JUnitRunner])
class GrantSpecs extends PlaySpecification with Mockito with Tables {

  // Silhouette Authorization.isAuthorized has implicit args for the following
  // Our authorizers don't need them, so mocks will suffice
  implicit val request = mock[Request[AnyContent]]
  implicit val messages = mock[Messages]

  def makeAuth(rights: Seq[GestaltRightGrant]) = AuthAccount(
    account = GestaltAccount(id = UUID.randomUUID, username = "john", "John", "Doe", email = Some("jdoe@gmail.com"), phoneNumber = None, description = None, directory = GestaltDirectory(id = UUID.randomUUID(), "", None, UUID.randomUUID())),
    groups = Seq(),
    rights = rights
  )

  def rightGrant(rightName: String, value: Option[String]) = GestaltRightGrant(id = UUID.randomUUID, rightName, value, appId = UUID.randomUUID)
  def rightGrant(rightName: String, value: String) = GestaltRightGrant(id = UUID.randomUUID, rightName, Some(value), appId = UUID.randomUUID)
  def rightGrant(rightName: String) = GestaltRightGrant(id = UUID.randomUUID, rightName, None, appId = UUID.randomUUID)

  def dummyAuth = mock[DummyAuthenticator]

  "hasGrant" should {

    "match when solely present with no value" in {
      val grantName = "testGrant"
      val oneRight = makeAuth(Seq(rightGrant(grantName, None)))
      await(hasGrant(grantName).isAuthorized(oneRight, dummyAuth)) must beTrue
    }

    "match when solely present with Some value" in {
      val grantName = "testGrant"
      val oneRight = makeAuth(Seq(rightGrant(grantName, Some("value"))))
      await(hasGrant(grantName).isAuthorized(oneRight, dummyAuth)) must beTrue
    }

    "match when present amongst others" in {
      val grantName = "testGrant"
      val manyRights = makeAuth(Seq(
        rightGrant(grantName, None),
        rightGrant("anotherGrant",None),
        rightGrant("thirdGrant",None)
      ))
      await(hasGrant(grantName).isAuthorized(manyRights, dummyAuth)) must beTrue
    }

    "not match when none present" in {
      val noRights = makeAuth(Seq())
      await(hasGrant("testGrant").isAuthorized(noRights, dummyAuth)) must beFalse
    }

    "not match when not present" in {
      val someRights = makeAuth(Seq(
        rightGrant("rightOne", None),
        rightGrant("rightTwo", None)
      ))
      await(hasGrant("rightWrong").isAuthorized(someRights, dummyAuth)) must beFalse
    }

  }

  "hasValue" should {

    "match when has named value" in {
      val right = makeAuth(Seq(rightGrant("foo",Some("bar"))))
      await(hasValue("foo","bar").isAuthorized(right, dummyAuth)) must beTrue
    }

    "not match when named value is None" in {
      val right = makeAuth(Seq(rightGrant("foo",None)))
      await(hasValue("foo","bar").isAuthorized(right, dummyAuth)) must beFalse
    }

  }

  "matchesValues" should {
    "match when values matches w.r.t. matcher" in {
      val n = "foo"
      val right = makeAuth(Seq(rightGrant(n,Some("BAR"))))
      await(matchesValue(n,"bar"){_ equalsIgnoreCase _}.isAuthorized(right, dummyAuth)) must beTrue
    }

    "not match when values don't match w.r.t matcher" in {
      val n = "foo"
      val v = "bar"
      val right = makeAuth(Seq(rightGrant(n,Some(v))))
      await(matchesValue(n,v){(_,_) => false}.isAuthorized(right, dummyAuth)) must beFalse
    }

    "not match when name doesn't match" in {
      val right = makeAuth(Seq(rightGrant("foo1",Some("bar"))))
      await(matchesValue("foo2","bar"){(_,_) => true}.isAuthorized(right, dummyAuth)) must beFalse
    }

    "not match when value doesn't exist" in {
      val right = makeAuth(Seq(rightGrant("foo",None)))
      await(matchesValue("foo","bar"){(_,_) => true}.isAuthorized(right, dummyAuth)) must beFalse
    }
  }


  "matchesGrant" should {

    def testGrant(grantName: String, test: String) = await(matchesGrant(test).isAuthorized(makeAuth(Seq(rightGrant(grantName))), dummyAuth))

    "match when exact" in {
      "grant"         | "test"        |>
        "abc"         ! "abc"         |
        "abc:def"     ! "abc:def"     |
        "abc:def:ghi" ! "abc:def:ghi" | { (grant,test) =>
        testGrant(grant,test) must beTrue
      }
    }

    "not match when exactly not" in {
      "grant"         | "test"        |>
        "abc"         ! "abC"         |
        "abc:def"     ! "ab:def"      |
        "abc:def"     ! "abc:de"      | { (grant,test) =>
        testGrant(grant,test) must beFalse
      }
    }

    "match wildcards on any single in grant or test" in {
      "grant"         | "test"        |>
        "*"           ! "abc"         |
        "abc"         ! "*"           |
        "*:def"       ! "abc:def"     |
        "abc:def"     ! "*:def"       |
        "abc:*:ghi"   ! "abc:def:ghi" |
        "abc:def:ghi" ! "abc:*:ghi"   |
        "*:def:*"     ! "abc:def:ghi" |
        "abc:def:ghi" ! "*:def:*"     |
        "*:def:*"     ! "abc:*:ghi"   | { (grant,test) =>
        testGrant(grant,test) must beTrue
      }
    }

    "fail on unmatched sizes in the absence of a super-wildcard" in {
      "grant"         | "test"        |>
        "*"           ! "abc:def"     |
        "abc:def"     ! "*"           |
        "abc:def"     ! "abc:def:ghi" |
        "abc:def:ghi" ! "abc:def"     | { (grant,test) =>
        testGrant(grant,test) must beFalse
      }
    }

    "pass on rightmost super-wildcard" in {
      "grant"         | "test"        |>
        "**"          ! "a:b:c"       |
        "a:**"        ! "a:b:c"       |
        "a:b:**"      ! "a:b:c"       |
        "a:b:c:**"    ! "a:b:c"       |
        "a:b:c"       ! "**"          |
        "a:b:c"       ! "a:**"        |
        "a:b:c"       ! "a:b:**"      |
        "a:b:c"       ! "a:b:c:**"    | { (grant,test) =>
        testGrant(grant,test) must beTrue
      }
    }

    "throw exception when super-wildcard isn't rightmost" in {
      "grant"         | "test"    |>
        "**:b"        ! "a:b"     |
        "a:b"         ! "**:b"    |
        "a:**:c"      ! "a:b"     |
        "a:b"         ! "a:**:c"  | { (grant,test) =>
        testGrant(grant,test) must throwA[RuntimeException]("invalid matcher; super-wildcard must be in the right-most field")
      }
    }

    "throw exception when grant name is empty" in {
      "grant"  | "test"    |>
        ""     ! "a:b"     |
        "a:b"  ! ""        | { (grant,test) =>
        testGrant(grant,test) must throwA[RuntimeException]("grant name must be non-empty")
      }
    }

  }

}
