package me.serendipify.appengine.experiments;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

@WebServlet(
    name = "HelloAppEngine",
    urlPatterns = {"/hello"}
)

//static final serialVerionUID = "10";

public class HelloAppEngine extends HttpServlet {

  /**
   * TODO change this	
   * */
	private static final long serialVersionUID = 1L;

	 /** Global instance of the HTTP transport. */
	  private static HttpTransport httpTransport = null;
	  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	  
	  /** OAuth 2.0 scopes. */
	  private static final List<String> SCOPES = Arrays.asList(
	      "r_basicprofile",
	      "r_emailaddress");
	  
	  /**
	   * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
	   * globally shared instance across your application.
	   */
	  private static FileDataStoreFactory dataStoreFactory;
	  
	  /** Directory to store user credentials. */
	  private static final java.io.File DATA_STORE_DIR =
	      new java.io.File(System.getProperty("user.home"), ".store/oauth2_sample");
	  
@Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) 
      throws IOException {
	try {
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	} catch (GeneralSecurityException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	// set up authorization code flow
/* 
    dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, JSON_FACTORY, "client_id", "secret", SCOPES).setDataStoreFactory(
        dataStoreFactory).build();
	 
	Credential ctoken = flow.loadCredential("lean.sheann");
	
	String authUrl = flow.newAuthorizationUrl().setState("xyz")
	        .setRedirectUri("https://localhost:8080/success.html").build();
	response.getWriter().print("Hello App Engine!\r\n");
	*/
	
//    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(transport, jsonFactory, client_id, client_secret, scopes).build();
//    GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
	
    String url =
            new GoogleAuthorizationCodeRequestUrl("https://www.linkedin.com/oauth/v2/authorization",
                "clientid", "http://localhost:8080/success.html", SCOPES).setState("/profile").build();
    response.sendRedirect(url);
//	GoogleAuthorizationCodeRequestUrl(String authorizationServerEncodedUrl, String clientId, String redirectUri, Collection<String> scopes) 
/*    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
*/
	
	
  }
}