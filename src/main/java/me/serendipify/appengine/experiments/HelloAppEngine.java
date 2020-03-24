package me.serendipify.appengine.experiments;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

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

	private static final Logger log = Logger.getLogger(HelloAppEngine.class.getName());
	
	/*
	private static String callbackURI = "http://localhost:8080/login/oauth/mytest/callback";
	private static String linkedInAuthorizationURI = "https://www.linkedin.com/oauth/v2/authorization";
	private static String linkedInSerendipifyMeClientId = "<ENTER_YOURS>";
*/
	
	private static String callbackURI = "http://localhost:8080/login/oauth/google/callback";
	private static String linkedInAuthorizationURI = "https://accounts.google.com/o/oauth2/v2/auth";
	private static String linkedInSerendipifyMeClientId = "77mnbvdv2hbci8";

	/*
	private static HttpTransport httpTransport = null;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	 */
	/** OAuth 2.0 scopes. */
//	private static final List<String> SCOPES = Arrays.asList("r_liteprofile");
	private static final List<String> SCOPES = Arrays.asList("email");

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
		
		
		/* ----- NEW LINKEDIN LOGIN ---- */
		
		//THIS IS CALLED VIA THE BUTTON
		
		if (request.getParameter("newLoginWithLinkedIn") != null) {
			
			String accessState = request.getParameter("state");
			
			String url = new GoogleAuthorizationCodeRequestUrl(linkedInAuthorizationURI,
			linkedInSerendipifyMeClientId, callbackURI, SCOPES).setState(accessState).build();
			log.info("newLoginWithLinkedIn GENERATED REDIRECT: "+url);
				
			//	//resp.setHeader(req.getSession().getId(), "Access-Control-Allow-Origin: *");
			//	resp.addHeader("Origin", "http://localhost:8080");
			//	resp.addHeader("Access-Control-Allow-Origin", "*");
			//	resp.addHeader("Access-Control-Allow-Methods","GET, OPTIONS, HEAD, PUT, POST");
			response.sendRedirect(url);	
			return;			
		}

		
		
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