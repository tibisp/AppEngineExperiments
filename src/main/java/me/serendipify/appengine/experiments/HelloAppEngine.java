package me.serendipify.appengine.experiments;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/*
import java.security.GeneralSecurityException;
*/

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
*/
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;

/*
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
*/

@WebServlet(
		name = "HelloAppEngine",
		urlPatterns = {"/hello"}
		)


public class HelloAppEngine extends HttpServlet {

	/**
	 * TODO change this	
	 * */
	private static final long serialVersionUID = 12L;

	private static String callbackURI = "http://localhost:8080/login/oauth/mytest/callback";
	private static String linkedInAuthorizationURI = "https://www.linkedin.com/oauth/v2/authorization";
	
	private static String linkedInSerendipifyMeClientId = "<ENTER_YOURS>";

	/*
	private static HttpTransport httpTransport = null;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	 */
	/** OAuth 2.0 scopes. */
	private static final List<String> SCOPES = Arrays.asList(
			"r_liteprofile");

	/*
	private static FileDataStoreFactory dataStoreFactory;
	 */

	/*
	private static final java.io.File DATA_STORE_DIR =
			new java.io.File(System.getProperty("user.home"), ".store/oauth2_sample");
	 */

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		/*
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, JSON_FACTORY, "client_id", "secret", SCOPES).setDataStoreFactory(
        dataStoreFactory).build();

	String authUrl = flow.newAuthorizationUrl().setState("xyz")
	        .setRedirectUri("https://localhost:8080/success.html").build();
	response.getWriter().print("Hello App Engine!\r\n");
    //GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
		 */

		String accessState = UUID.randomUUID().toString().replace("-", "");

		String url =
				new GoogleAuthorizationCodeRequestUrl(linkedInAuthorizationURI,
						linkedInSerendipifyMeClientId, callbackURI, SCOPES).setState(accessState).build();

		response.sendRedirect(url);	
	}
}