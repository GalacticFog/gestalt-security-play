class TestSecuredController @Inject() ( mApi: MessagesApi,
                                        env: GestaltFrameworkSecurityEnvironment[DummyAuthenticator] )
  extends GestaltFrameworkSecuredController[DummyAuthenticator](mApi, env) {

  // define a function based on the request header
  def inSitu() = GestaltFrameworkAuthAction({rh: RequestHeader => rh.headers.get("FQON")}) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated with username ${account.username}")
  }

  // like above, but async+json
  def inSituJsonAsync() = GestaltFrameworkAuthAction({rh: RequestHeader => rh.headers.get("FQON")}).async(parse.json) {
    securedRequest => Future {
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated with username ${account.username}")
    }
  }

  // just pass a string
  def fromArgs(fqon: String) = GestaltFrameworkAuthAction(Some(fqon)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated with username ${account.username} on org ${fqon}")
  }

  // how about a UUID? we got that covered! this authenticates against /orgs/:orgId/auth
  def aUUID(orgId: UUID) = GestaltFrameworkAuthAction(Some(orgId)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated with username ${account.username} on org ${orgId}")
  }

  def hello() = GestaltFrameworkAuthAction(Option.empty[String]) { securedRequest =>
    Ok("hello")
  }

  // how about some authenticated methods with a credential-passthrough call to security?
  def createOrgPassthrough(parentOrgId: UUID) = GestaltFrameworkAuthAction(Some(parentOrgId)).async(parse.json) {
    securedRequest =>
      val account = securedRequest.identity.account
      val creds = securedRequest.identity.creds
      GestaltOrg.createSubOrg(parentOrgId = parentOrgId, GestaltOrgCreate("someNewOrgName"))(securityClient.withCreds(creds)) map {
        // do what you were going to do
        newOrg => Created(Json.obj(
          "newAccountId" -> newOrg.id,
          "createdBy" -> account.id
        ))
      }
  }

  def deleteOrgPassthrough(orgId: UUID) = GestaltFrameworkAuthAction(Some(orgId)).async(parse.json) {
    securedRequest =>
      val account = securedRequest.identity.account
      val creds = securedRequest.identity.creds
      GestaltOrg.deleteOrg(orgId)(securityClient.withCreds(creds)) map {
        // do what you were going to do
        newOrg => Ok(Json.obj(
          "deletedOrgId" -> orgId,
          "deletedBy" -> account.id
        ))
      }
  }

  def createAccountPassthrough(parentOrgId: UUID) = GestaltFrameworkAuthAction(Some(parentOrgId)).async(parse.json) {
    securedRequest =>
      val account = securedRequest.identity.account
      val someExistingGroupId = UUID.randomUUID()
      val creds = securedRequest.identity.creds
      GestaltOrg.createAccount(orgId = parentOrgId, GestaltAccountCreateWithRights(
        username = "bsmith",
        firstName = "bob",
        lastName = "smith",
        email = Some("bsmith@myorg"),
        phoneNumber = Some("505-867-5309"),
        credential = GestaltPasswordCredential("bob's password"),
        groups = Some(Seq(someExistingGroupId)),
        rights = Some(Seq(GestaltGrantCreate("freedom")))
      ))(securityClient.withCreds(creds)) map {
        // do what you were going to do
        newOrg => Created(Json.obj(
          "newAccountId" -> newOrg.id,
          "createdBy" -> account.id,
          "authenticatedIn" -> securedRequest.identity.authenticatingOrgId
        ))
      }
  }

}
