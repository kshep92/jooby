package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.HttpCredentials;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.http.profile.creator.AuthenticatorProfileCreator;

public class BasicAuthAsClientFeature extends ServerFeature {

  {

    use(new Auth()
        .client(
        new IndirectBasicAuthClient(
            new SimpleTestUsernamePasswordAuthenticator(),
            new AuthenticatorProfileCreator<HttpCredentials, UserProfile>())
        ));

    get("/auth/basic", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .basic("test", "test")
        .get("/auth/basic")
        .expect("/auth/basic");
  }

  @Test
  public void unauthorizedAjax() throws Exception {
    request()
        .get("/auth/basic")
        .header("X-Requested-With", "XMLHttpRequest")
        .expect(401);
  }

  @Test
  public void unauthorized() throws Exception {
    request()
        .get("/auth/basic")
        .expect(401);

  }

  @Test
  public void badCredentials() throws Exception {
    request()
        .basic("test", "")
        .get("/auth/basic")
        .expect(401);
  }

}
