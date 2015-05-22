package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{GestaltRightGrant, GestaltAccount, GestaltAuthResponse, HTTP}
import com.galacticfog.gestalt.security.play.silhouette.authorization.{matchesValue, hasValue, matchesGrant, hasGrant}
import com.galacticfog.gestalt.security.play.silhouette.utils.GestaltSecurityConfig
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.api.test.WithApplication

@RunWith(classOf[JUnitRunner])
class GrantSpecs extends Specification with Mockito with Tables {

  // Silhouette Authorization.isAuthorized has implicit args for the following
  // Our authorizers don't need them, so mocks will suffice
  implicit val requestHeader = mock[RequestHeader]
  implicit val lang = mock[Lang]

  def makeAuth(rights: Seq[GestaltRightGrant]) = AuthAccount(
    account = GestaltAccount("john", "John", "Doe", "jdoe@gmail.com"),
    rights = rights
  )

  def simpleRight(rightName: String) = makeAuth(Seq(GestaltRightGrant(rightName,None)))

  "hasGrant" should {

    "match when solely present with no value" in {
      val grantName = "testGrant"
      val oneRight = makeAuth(Seq(GestaltRightGrant(grantName, None)))
      hasGrant(grantName).isAuthorized(oneRight) must beTrue
    }

    "match when solely present with Some value" in {
      val grantName = "testGrant"
      val oneRight = makeAuth(Seq(GestaltRightGrant(grantName, Some("value"))))
      hasGrant(grantName).isAuthorized(oneRight) must beTrue
    }

    "match when present amongst others" in {
      val grantName = "testGrant"
      val manyRights = makeAuth(Seq(
        GestaltRightGrant(grantName, None),
        GestaltRightGrant("anotherGrant",None),
        GestaltRightGrant("thirdGrant",None)
      ))
      hasGrant(grantName).isAuthorized(manyRights) must beTrue
    }

    "not match when none present" in {
      val noRights = makeAuth(Seq())
      hasGrant("testGrant").isAuthorized(noRights) must beFalse
    }

    "not match when not present" in {
      val someRights = makeAuth(Seq(
        GestaltRightGrant("rightOne", None),
        GestaltRightGrant("rightTwo", None)
      ))
      hasGrant("rightWrong").isAuthorized(someRights) must beFalse
    }

  }

  "hasValue" should {

    "match when has named value" in {
      val right = makeAuth(Seq(GestaltRightGrant("foo",Some("bar"))))
      hasValue("foo","bar").isAuthorized(right) must beTrue
    }

    "not match when named value is None" in {
      val right = makeAuth(Seq(GestaltRightGrant("foo",None)))
      hasValue("foo","bar").isAuthorized(right) must beFalse
    }

  }

  "matchesValues" should {
    "match when values matches w.r.t. matcher" in {
      val n = "foo"
      val right = makeAuth(Seq(GestaltRightGrant(n,Some("BAR"))))
      matchesValue(n,"bar"){_ equalsIgnoreCase _}.isAuthorized(right) must beTrue
    }

    "not match when values don't match w.r.t matcher" in {
      val n = "foo"
      val v = "bar"
      val right = makeAuth(Seq(GestaltRightGrant(n,Some(v))))
      matchesValue(n,v){(_,_) => false}.isAuthorized(right) must beFalse
    }

    "not match when name doesn't match" in {
      val right = makeAuth(Seq(GestaltRightGrant("foo1",Some("bar"))))
      matchesValue("foo2","bar"){(_,_) => true}.isAuthorized(right) must beFalse
    }

    "not match when value doesn't exist" in {
      val right = makeAuth(Seq(GestaltRightGrant("foo",None)))
      matchesValue("foo","bar"){(_,_) => true}.isAuthorized(right) must beFalse
    }
  }


  "matchesGrant" should {

    def testGrant(grantName: String, test: String) = matchesGrant(test).isAuthorized(simpleRight(grantName))

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
