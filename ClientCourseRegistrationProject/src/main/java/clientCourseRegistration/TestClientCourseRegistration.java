package clientCourseRegistration;

import java.util.Scanner;

import org.glassfish.jersey.client.ClientConfig;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

// Walks through one registration: login, browse, search, pick a section, reserve, pay, check balance.
public class TestClientCourseRegistration {

	public static void main(String[] args) {

		Scanner console = new Scanner(System.in);

		String url = "http://localhost:8080/WebCourseRegistrationRESTProject/rest";
		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);

		// new: Step 1 - login. The username and the password travel in the body of a POST request,
		// as form parameters, exactly as the HTML login form sends them.
		System.out.println("Step 1. Please enter the username (e.g. clerk, or a student email): ");
		String vusername = console.nextLine();
		System.out.println("Please enter the password: ");
		String vpassword = console.nextLine();
		Form loginForm = new Form();
		loginForm.param("usernameParam", vusername);
		loginForm.param("passwordParam", vpassword);
		WebTarget target = client.target(url).path("/User/login");
		Response loginResponse = target.request()
								 .accept(MediaType.TEXT_HTML)
								 .post(Entity.form(loginForm));

		// new: an accepted login answers with a cookie named authToken. The client keeps its value
		// and sends it back with every request that needs a login. No cookie means the login was refused.
		NewCookie cookie = loginResponse.getCookies().get("authToken");
		if (cookie == null) {
			System.out.println("Login rejected: wrong username or password.");
			console.close();
			return;
		}
		String vtoken = cookie.getValue();
		System.out.println("Login confirmed for " + vusername + ".");

		// Step 2 - business process 1: courses of one department (category)
		System.out.println("Step 2. Please enter the department (e.g. Software Engineering): ");
		String vdepartment = console.nextLine();
		target = client.target(url).path("/Course/byDepartment/" + vdepartment);
		String response = target.request()
						  .accept(MediaType.APPLICATION_JSON)
						  .get(String.class);
		System.out.println(response);

		// Step 3 - business process 2: narrow down by code, title or instructor
		System.out.println("Step 3. Please enter a keyword to search (e.g. Calculus): ");
		String vkeyword = console.nextLine();
		target = client.target(url).path("/Course/search")
				.queryParam("keywordParam", vkeyword);
		response = target.request()
				  .accept(MediaType.APPLICATION_JSON)
				  .get(String.class);
		System.out.println(response);

		// Step 4: sections offered for the chosen course, one readable line per section
		System.out.println("Step 4. Please enter the course code you chose (e.g. SE101): ");
		String vcourseCode = console.nextLine();
		target = client.target(url).path("/Section/byCourse/" + vcourseCode);
		response = target.request()
				  .accept(MediaType.TEXT_PLAIN)
				  .get(String.class);
		System.out.println(response);

		// Step 5 - business process 4: reserve a seat in that section
		// updated: POST with form parameters instead of GET with query parameters, and the cookie of step 1
		System.out.println("Step 5. Please enter the student id (ignored for a student account): ");
		while (!console.hasNextInt()) {
			System.out.println("The student id is a number. Please enter the student id: ");
			console.next();
		}
		int vstudentId = console.nextInt();
		System.out.println("Please enter the section number from step 4 (e.g. 11 in \"Section 11\"): ");
		while (!console.hasNextInt()) {
			System.out.println("The section is a number, not a course code. Please enter the section number: ");
			console.next();
		}
		int vsectionId = console.nextInt();
		Form reserveForm = new Form();
		reserveForm.param("studentIdParam", "" + vstudentId);
		reserveForm.param("sectionIdParam", "" + vsectionId);
		target = client.target(url).path("/Section/reserve");
		response = target.request()
				  .accept(MediaType.APPLICATION_JSON)
				  .cookie("authToken", vtoken)
				  .post(Entity.form(reserveForm), String.class);
		System.out.println(response);

		// new: Step 6 - business process 5: pay tuition, POST with form parameters
		// the amount is sent as typed: the service checks it and answers with its own message when it is not valid
		console.nextLine();
		System.out.println("Step 6. Please enter the amount to pay (e.g. 100): ");
		String vamount = console.nextLine();
		System.out.println("Please enter the payment method (Credit, Debit or Interac): ");
		String vmethod = console.nextLine();
		Form payForm = new Form();
		payForm.param("studentIdParam", "" + vstudentId);
		payForm.param("amountParam", vamount);
		payForm.param("methodParam", vmethod);
		target = client.target(url).path("/Payment/pay");
		response = target.request()
				  .accept(MediaType.APPLICATION_JSON)
				  .cookie("authToken", vtoken)
				  .post(Entity.form(payForm), String.class);
		System.out.println(response);

		// Step 7 - business process 5: balance after the reservation and the payment
		// updated: the cookie is sent, since the balance now requires a login
		target = client.target(url).path("/Payment/balance/" + vstudentId);
		response = target.request()
				  .accept(MediaType.TEXT_HTML)
				  .cookie("authToken", vtoken)
				  .get(String.class);
		System.out.println(response);

		// new: logout closes the session on the server
		target = client.target(url).path("/User/logout");
		response = target.request()
				  .accept(MediaType.TEXT_HTML)
				  .cookie("authToken", vtoken)
				  .get(String.class);
		System.out.println("Logout done.");

		console.close();
	}

}
