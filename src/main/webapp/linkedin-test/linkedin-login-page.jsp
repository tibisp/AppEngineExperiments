<%@include file="/js-scripts/library-imports.js"%>


<script type="text/javascript">

  $(document).ready(function() {
	  if (sessionStorage["errorMessage"]!=undefined && sessionStorage["errorMessage"].length>0){
		  console.log("Receiving Error from Backend "+sessionStorage["errorMessage"]);
		  $("#linkedinErrorMessage").html(sessionStorage["errorMessage"]);
	  }else{
		  $("#linkedinErrorMessage").html("");
	  } 		  
  });
</script>


<div class="separatordiv"></div>

<div class="text-center">
	<h1 >Welcome Linkedin</h1>
	<div class="separatordivhalf"></div>
	
	<div class="row">
		<div class="col-md-4 col-md-offset-4">
			<button class="btn btn-default" id="clearSessionBtn">Reset Session</button>
		</div>
	</div>
	<div class="row">
		<div class="col-md-4 col-md-offset-4">
		<h3>Invoke for Localhost</h3>
	 	<a href="https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=77mnbvdv2hbci8&redirect_uri=http%3A%2F%2Flocalhost:8080%2Flogin%2Foauth%2Flinkedin%2Fcallback&state=fooobar&scope=r_liteprofile">
			<img src="/img/external/LinkedIn-Sign-In-Small---Default.png">
		</a>
		</div>		
	</div>
	<div id="linkedinErrorMessage">No Error</div>
</div>



<div class="separatordiv"></div>


