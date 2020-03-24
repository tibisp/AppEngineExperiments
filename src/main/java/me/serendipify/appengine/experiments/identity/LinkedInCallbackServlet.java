package me.serendipify.appengine.experiments.identity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;

public class LinkedInCallbackServlet extends HttpServlet {

	private static final long serialVersionUID = 2465978614596746655L;
	private static final Logger logger = Logger.getLogger(LinkedInCallbackServlet.class.getName());
	private static Gson gsonEngine=new Gson();

	private static String linkedinProfileUrl = "https://api.linkedin.com/v2/me";
	private static String callbackURI = "http://localhost:8080/login/oauth/linkedin/callback";
	private static String redirectURI = "http://localhost:8080/success.html";

	private static String linkedInSerendipifyMeClientId = "77mnbvdv2hbci8";
	//FIXME: Keep SUPER SECURE
	private static String linkedInSerendipifyMeClientSecret = "<SECRET>";


	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws IOException {	  

		Map<String, Object> respMap = new HashMap<String, Object>();

		String codeParam = request.getParameter("code");
		String stateParam = request.getParameter("state");
		String accessToken=null;

		//check for errors first
		String errorParam = request.getParameter("error");
		if(errorParam!= null && !errorParam.isEmpty()) {
			switch(errorParam) {
			case "user_cancelled_login" :
			case "user_cancelled_authorize":
			default:
				String logErrorMessage = "Failed LinkedIn Authentication: ";
				String errorDescription = request.getParameter("error_description");
				if(errorDescription!=null && !errorDescription.isEmpty()) {
					logErrorMessage+=errorDescription;
				}
				logger.warning(logErrorMessage);
				break;
			}
			response.setStatus(401);
			respMap.put("execStatus", "FAILED");
			respMap.put("errorMessage", "AUTH FAILED - CANCELLED BY USER");
			response.getWriter().write(gsonEngine.toJson(respMap));
			//TODO: maybe redirect back ?
			return;
		}		


		String requestPath = request.getRequestURI();
		logger.info("Request path: "+requestPath);

		if (codeParam!=null && !codeParam.isEmpty()) {
			//session comes directly from appengine cache
			logger.info("Receiving authorization CODE: "+codeParam);

			HttpSession session = request.getSession(false);
			if (session!=null && !session.getId().isEmpty() && session.getAttribute("displayName")!=null){
				logger.info("EXISTING SESSION - NO LOGIN STEPS NECESSARY: "+session.getId()+" FOR "+session.getAttribute("displayName"));
				respMap.put("execStatus", "OK");
				respMap.put("errorMessage", "ALREADY AUTHORIZED - SESSION STORED IN MEMCACHE");
			} else {

				//go to step 2 of OAUTH - request authToken
				JsonObject accessTokenObject = loginStep2RequestAccessToken(request, response, codeParam);
				if (accessTokenObject == null) {
					respMap.put("errorMessage", "LINKEDIN AUTH FAILED");
					session.removeAttribute("displayName");
					session.setAttribute("errorMessage", "ERROR");
					response.sendRedirect("/test/linkedin-login-page.jsp?errorMessage="+URLEncoder.encode("FAILED LINKEDIN AUTHORIZATION", "UTF-8"));
					return;
				}
				accessToken = accessTokenObject.getString("access_token");

				logger.info("ACCESS TOKEN: "+accessToken);


				JsonObject loginUser = loginStep3LinkedInRequestUserID(accessToken);

				if (loginUser.get("status")!=null && loginUser.getInt("status")==401) {
					respMap.put("errorMessage", "LINKEDIN AUTH FAILED");
					session.removeAttribute("displayName");
					session.setAttribute("errorMessage", loginUser.getString("message"));
					response.sendRedirect("/test/linkedin-login-page.jsp?errorMessage="+URLEncoder.encode(loginUser.getString("message"), "UTF-8"));
					return;
				}
				String displayName=loginUser.getString("localizedFirstName")+"."+loginUser.getString("localizedLastName");

				logger.info("USER: "+displayName);

			}
		}

		response.setStatus(200);
		response.sendRedirect(redirectURI);
		return;
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



	public JsonObject loginStep2RequestAccessToken(HttpServletRequest request, HttpServletResponse response, String codeParam) {

		logger.info("HANDLE AUTH CODE CALLBACK");

		Map<String,Object> params = new LinkedHashMap<>();
		params.put("grant_type", "authorization_code");
		params.put("code", codeParam);
		params.put("redirect_uri", callbackURI);
		params.put("client_id", linkedInSerendipifyMeClientId);
		params.put("client_secret", linkedInSerendipifyMeClientSecret);

		try {
			StringBuilder postData = new StringBuilder();
			for (Map.Entry<String,Object> param : params.entrySet()) {
				if (postData.length() != 0) postData.append('&');
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");
			logger.info("POST MESSAGE: "+postData.toString());

			URL url = new URL("https://www.linkedin.com/oauth/v2/accessToken");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			connection.getOutputStream().write(postDataBytes);
			connection.getOutputStream().close();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				StringBuffer sb = new StringBuffer();
				String line;
				// Read input data stream.
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				reader.close();
				logger.warning("RESPONSE: "+sb.toString());

				JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()));
				JsonObject jsonObject = jsonReader.readObject();
				return jsonObject;
			} else {
				logger.warning("HTTP ERROR "+connection.getResponseCode());
				StringBuffer sb = new StringBuffer();
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				reader.close();
				JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()));
				JsonObject jsonObject = jsonReader.readObject();
				logger.warning("ERROR: "+sb.toString());
				return jsonObject;
			}
		} catch (SocketTimeoutException ste) {
			logger.info(ste.getLocalizedMessage());
		} catch (MalformedURLException mue) {
			logger.info(mue.getLocalizedMessage());
		} catch (UnsupportedEncodingException uee) {
			logger.info(uee.getLocalizedMessage());
		} catch (IOException e) {
			logger.info(e.getLocalizedMessage());
		}
		return null;		
	}


}