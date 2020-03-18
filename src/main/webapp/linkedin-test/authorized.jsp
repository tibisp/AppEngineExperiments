
<div class="separatordiv"></div>
<div class="container">

	Hello, you have been redirected here
	<p></p>
	<%
		HttpSession mySession = request.getSession();
		out.println("<script>console.log(\"SESSION "+(String)mySession.getId()+"\");</script>");
	    String messageAttrib = (String)mySession.getAttribute("debugMessage");
		if (messageAttrib!=null && !messageAttrib.isEmpty()){
			out.println("Got Attribute From Session: "+messageAttrib);
		}else{
	    	String messageParam = (String)request.getParameter("message");
		 	if (messageParam!=null && !messageParam.isEmpty()){
				out.println("<h3>"+messageParam+"</h3>");		
			}else{
				out.println("no message");
			}
		}
	
	%>
	<p></p>
	<a href="/test/linkedin-login-page.jsp">Back to Login Test</a>


</div>
<div class="separatordiv"></div>


