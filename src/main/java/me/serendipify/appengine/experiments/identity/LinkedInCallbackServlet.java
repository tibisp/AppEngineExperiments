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
	private static String redirectURI = "http://localhost:8080/linkedin-test/authorized.jsp";

	private static String linkedInSerendipifyMeClientId = "<ENTER_YOURS>";
	//FIXME: Keep SUPER SECURE
	private static String linkedInSerendipifyMeClientSecret = "<ENTER_YOURS>";

    
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
			
			//AQSo9sh2sA_uRguu95hsEdODZ65X9vXr8cBNyMTMGbBKAsHR3fh29xUqOwsISUvtw4pdvT70pcTLPIPMXk9QYdIB9_sagPuPVnrXF-ShhkFLsO5xDSjvDXKQ08skOEvViqL2dG253Nio8R8a153-p2oohQuVhwZKjyhFQ_6TR21nlMdDdqzMMM6PoKFsGw&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Flogin%2Foauth%2Flinkedin%2Fcallback&client_id=***REMOVED***&client_secret=***REMOVED***
			//AQQWb2ObCuaJPbGn-AK3GyBr53mtixxJlxx-diOuqU3TjVo_UFlljR7OOemRbNQuzKRLQ04kRPHShUIRyfCy5Gbuig2hrNa00BjCHACXnOdJLaoIfEwnK106wlIDXC0yYHh1yh_fjsGZ2WRK3_NsyUN_SXy-kh6Qp_wB0H1vV7LV0ws0k8EqLhuFeQJenA
			
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
				
				//AQWgyPY92wNXqjo85Wfqtcf-8av3cg5-1kTTu_HWFITbV4URBIIO0nbRIsFkRGc6O6Qz_yz-9wFf9dfbhwV2ucPfgR9Q-QdmSmPxmXVo0jeUdm7gdDXt_W5GDJDDIXaLaTB0RQHyDDQlvv2ottmIib2fzexOyDq7WwUB65iaUOI3s7RtjhFr3nWpSJKwEld9TdQ7I8j7dTn5C0Zm32FCW6CxTjzr5bt0s_YYBImnELSCTt_LjNgwnWoXAywwBr5XM_dkvC9HSpUlUCjRaygua8QXWfAqypEQqF38uC9bXLqBxKogc6NH198vMjel-_YKh7WB-dHJKDEfA7TKY2jGor9aPQFX6Q
				//AQVw4h9qKkaEhgbSz-qUCsAvVQonZEYhNztJzOkfUfHNvZflfZEHPUuSFrwm3djhZDlObJzmy0pBY4wx_K0-7RW8qMdzlPWYM2zzkozGc_3jXZDO2miMv-o57CuyDuGWKtf9p8jInMb0_NkznRWL_i3lZ5qjyY6YwqOOjdrTBW_Oh7RXfyMuGioa_HCv77TM7kDxtsHPQyvv3-8smsDZIFz2x5U_MhCxsTMYp7XQbgD4rijzD8Gd3yDHh36RbDRlhM1knUR_bYV7vn-5KPFdEXNyP0D17bJCetoNNIaXquVX43A1SXTEZNXh5GbFWpv1Pl7chkMl0kfTGcaJDPQM1be6Znm18A
				
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
