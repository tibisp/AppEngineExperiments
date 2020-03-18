package me.serendipify.appengine.experiments.identity;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

public class LinkedInCallbackServlet extends HttpServlet {

	private static final long serialVersionUID = 2465978614596746655L;
	private static final Logger logger = Logger.getLogger(LinkedInCallbackServlet.class.getName());
	private static Gson gsonEngine=new Gson();
	
    private static String linkedinProfileUrl = "https://api.linkedin.com/v2/me";
	private static String callbackURI = "http://localhost:8080/login/oauth/linkedin/callback";
	private static String redirectURI = "http://localhost:8080/test/authorized.jsp";

    
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
		      throws IOException {	  

		response.setStatus(200);
		response.sendRedirect("/test/authorized.jsp");
		return;
	}

}
