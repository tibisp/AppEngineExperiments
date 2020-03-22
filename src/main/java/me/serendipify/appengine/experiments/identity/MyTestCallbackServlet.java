package me.serendipify.appengine.experiments.identity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

public class MyTestCallbackServlet extends HttpServlet {

	private static final long serialVersionUID = 2465978614596746653L;
	private static final Logger logger = Logger.getLogger(MyTestCallbackServlet.class.getName());

	private static String linkedinProfileUrl = "https://api.linkedin.com/v2/me";
	private static String redirectURI = "http://localhost:8080/login/oauth/mytest/callback";
	private static String linkedInAccessTokenURI = "https://www.linkedin.com/oauth/v2/accessToken";

	private static String linkedInSerendipifyMeClientId = "<ENTER_YOURS>";
	//FIXME: Keep SUPER SECURE
	private static String linkedInSerendipifyMeClientSecret = "<ENTER_YOURS>";

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		StringBuffer fullUrlBuf;
		String accessCode;
		String accessState;
		TokenResponse tokenResp;
		String accessToken;
		String refreshToken;

		fullUrlBuf = request.getRequestURL();

		if (request.getQueryString() != null) {
			fullUrlBuf.append('?').append(request.getQueryString());
		}

		AuthorizationCodeResponseUrl authResponse =
				new AuthorizationCodeResponseUrl(fullUrlBuf.toString());

		// check for user-denied error
		if (authResponse.getError() != null) {
			logger.warning("ERROR: " + authResponse.getError());
		} else {
			accessCode = authResponse.getCode();
			logger.info("ACCESS CODE: " + accessCode);
			accessState = authResponse.getState();
			logger.info("ACCESS STATE: " + accessState);
			// TODO check accessState against the value sent in Step1

			tokenResp = requestAccessToken(accessCode, "authorization_code");
			accessToken = tokenResp.getAccessToken();
			loginStep3LinkedInRequestUserID(accessToken);
			
			refreshToken = tokenResp.getRefreshToken();
			logger.info("REFRESH TOKEN: " + refreshToken);
			
			if (refreshToken != null) {
				tokenResp = requestAccessToken(refreshToken, "refresh_token");
				accessToken = tokenResp.getAccessToken();
				loginStep3LinkedInRequestUserID(accessToken);
			}
		}

		response.setStatus(200);
		response.sendRedirect("http://localhost:8080/success.html");
		return;
	}

	private TokenResponse requestAccessToken(String code, String grandType) {
		TokenResponse tokenResp = null;

		if((code == null) || (code == null)) {
			logger.warning("Wrong param: " + grandType + ", " + code);
			return tokenResp;
		}

		try {
			if(grandType.equals("authorization_code")) {
				tokenResp = 
						new AuthorizationCodeTokenRequest(new NetHttpTransport(), JSON_FACTORY, 
								new GenericUrl(linkedInAccessTokenURI), code)
						.setRedirectUri(redirectURI)
						.setGrantType(grandType)
						.setClientAuthentication(
								new ClientParametersAuthentication(linkedInSerendipifyMeClientId, linkedInSerendipifyMeClientSecret))
						.execute();
			} else if (grandType.equals("refresh_token")) {
				tokenResp = 
						new RefreshTokenRequest(new NetHttpTransport(), JSON_FACTORY, 
								new GenericUrl(linkedInAccessTokenURI), code)
						.setGrantType(grandType)
						.setClientAuthentication(
								new ClientParametersAuthentication(linkedInSerendipifyMeClientId, linkedInSerendipifyMeClientSecret))
						.execute(); 
			} else {
				logger.warning("Wrong param: " + grandType);
				return tokenResp;
			}
			logger.info("ACCESS TOKEN: " + tokenResp.getAccessToken());
		} catch (TokenResponseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
			logger.warning(ioe.getLocalizedMessage());
		}

		return tokenResp;
	}

	public JsonObject loginStep3LinkedInRequestUserID(String accessToken) {
		URL url;
		try {
			url = new URL(linkedinProfileUrl);
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Bearer " + accessToken);
			con.setRequestProperty("cache-control", "no-cache");
			con.setRequestProperty("X-Restli-Protocol-Version", "2.0.0");

			BufferedReader br = null;
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			}else {
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
			StringBuilder jsonString = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				jsonString.append(line);
			}
			br.close();
			JsonReader jsonReader = Json.createReader(new StringReader(jsonString.toString()));
			JsonObject jsonObject = jsonReader.readObject();

			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				logger.info("USER: "+jsonObject.toString());
			}else {
				logger.warning("ERROR: "+jsonObject.toString());
			}
			return jsonObject;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
			logger.warning(ioe.getLocalizedMessage());
		}
		return null;
	}


}
