package me.serendipify.appengine.experiments.identity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
	private static String redirectURI = "http://localhost:8080/linkedin-test/authorized.jsp";

	private static String linkedInSerendipifyMeClientId = "<ENTER YOURS>";
	//FIXME: Keep SUPER SECURE
	private static String linkedInSerendipifyMeClientSecret = "<ENTER YOURS>";

    
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
				//String debugToken = "AQV0mkh3vDqF3-8Z7QCgM4KcqLD6355_PVe1HVfytA7_tgSFS-r3_VobyKC53LpBm8PrvD8x_1vVEmlfelaBYvsmnSZY91rJRY3v49nioD8iuQ0ysuqx2xMO_9PcMDTL7o3dcLabrb8WHipF91vrm4MhiZh8SbY3kpx0IxuzbFZQCNQPRmrNM0_BhPqie1PaJB2VsKauMDFq4nGeM1m2yu3LYxgft_i--z5kOZ5zY2_KJYXSyFWTxJKZTL1393RfsvGRyj6t1xU71S1e5oXWtNhK8hn5tZKZyi_BaGqLXqXkYL0skMNPcba-9ZFiXywB-HbqdnDGpvpozUIWUhdr5Yx9s_myzg";
				
				logger.info("ACCESS TOKEN: "+accessToken);
			}
		}
		
		response.setStatus(200);
		response.sendRedirect(redirectURI);
		return;
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
