package webCourseRegistrationREST;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("Payment")
public class PaymentResource {

	private boolean isAcceptedMethod(String method) {
		return method.equalsIgnoreCase("Credit") || method.equalsIgnoreCase("Debit")
				|| method.equalsIgnoreCase("Interac");
	}

	// Shared by the HTML and JSON pay methods; applies all payment rules.
	// updated: an accepted payment is now written to Payment.out and the new balance to Student.in
	private Payment processPayment(int studentId, double amount, String method) throws IOException {

		Student st = CourseRegistrationData.findStudent(studentId);

		if (st == null) {
			return new Payment("", studentId, "", 0, method, "REJECTED", "Student " + studentId + " was not found.", 0);
		}
		if (method == null || method.trim().isEmpty()) {
			return new Payment("", studentId, st.fullName(), 0, "", "REJECTED", "A payment method is required.",
					st.getBalance());
		}
		if (!isAcceptedMethod(method.trim())) {
			return new Payment("", studentId, st.fullName(), 0, Validation.clean(method), "REJECTED",
					"Payment method " + Validation.clean(method) + " is not accepted. Use Credit, Debit or Interac.",
					st.getBalance());
		}
		if (amount <= 0) {
			return new Payment("", studentId, st.fullName(), 0, method, "REJECTED",
					"The amount must be a number greater than 0.", st.getBalance());
		}
		if (amount > st.getBalance()) {
			return new Payment("", studentId, st.fullName(), 0, method, "REJECTED",
					"The amount " + Page.money(amount) + " is more than the balance of "
							+ Page.money(st.getBalance()) + ".",
					st.getBalance());
		}

		st.applyPayment(amount);
		// new: keeps the balance at two decimals, so a balance paid in full is exactly 0
		st.setBalance(Math.round(st.getBalance() * 100) / 100.0);

		ArrayList<Payment> paymentList = CourseRegistrationData.paymentList;
		String receiptNo = "R" + (1001 + paymentList.size());

		Payment receipt = new Payment(receiptNo, studentId, st.fullName(), amount, method.trim(), "PAID",
				"Payment received. Thank you.", st.getBalance());
		receipt.setDate(LocalDate.now().toString());
		paymentList.add(receipt);

		CourseRegistrationData.savePayments();
		CourseRegistrationData.saveStudents();

		return receipt;
	}

	// new: the charges and the payments of one student, shared by the balance page and the payment form
	private String buildHTMLStatement(Student st) {

		String result = "<h3>Current balance: " + Page.money(st.getBalance()) + " ("
				+ (st.getBalance() == 0 ? "PAID IN FULL" : "PAYMENT DUE") + ")</h3>";

		result += "<h3>Tuition Charges</h3>";
		result += "<table border=\"1\"><tr><th>Date</th><th class=\"num\">Section</th><th>Course</th><th class=\"num\">Tuition Charged</th>"
				+ "<th class=\"num\">Balance After Charge</th></tr>";
		ArrayList<Enrollment> enrollmentList = CourseRegistrationData.enrollmentList;
		for (int i = 0; i < enrollmentList.size(); i++) {
			Enrollment e = enrollmentList.get(i);
			if (e.getStudentId() == st.getStudentId()) {
				result += "<tr><td>" + e.getDate() + "</td><td class=\"num\">" + e.getSectionId() + "</td><td>"
						+ e.getCourseCode() + "</td><td class=\"num\">" + Page.money(e.getTuitionCharged())
						+ "</td><td class=\"num\">" + Page.money(e.getNewBalance()) + "</td></tr>";
			}
		}
		result += "</table>";

		result += "<h3>Payments</h3>";
		result += "<table border=\"1\"><tr><th>Date</th><th>Receipt No</th><th class=\"num\">Amount Paid</th><th>Method</th>"
				+ "<th class=\"num\">Balance After Payment</th></tr>";
		ArrayList<Payment> paymentList = CourseRegistrationData.paymentList;
		for (int i = 0; i < paymentList.size(); i++) {
			Payment p = paymentList.get(i);
			if (p.getStudentId() == st.getStudentId()) {
				result += "<tr><td>" + p.getDate() + "</td><td>" + p.getReceiptNo() + "</td><td class=\"num\">"
						+ Page.money(p.getAmountPaid()) + "</td><td>" + p.getPaymentMethod()
						+ "</td><td class=\"num\">" + Page.money(p.getNewBalance()) + "</td></tr>";
			}
		}
		result += "</table>";

		return result;
	}

	// new: transaction form of the payment. The student is searched by id, the balance and the statement are
	// read from the tables and shown, and the page carries the payment form already filled with that id.
	@GET
	@Path("/payForm")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLPayForm(@CookieParam("authToken") String token,
			@QueryParam("studentIdParam") String studentIdText) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		int studentId = current.studentIdFor(Validation.toInt(studentIdText));
		if (studentId < 0) {
			return Page.message("Pay Tuition", "Search REJECTED", "The student id must be a whole number.");
		}
		Student st = CourseRegistrationData.findStudent(studentId);
		if (st == null) {
			return Page.message("Pay Tuition", "Student " + studentId + " was not found.", "");
		}

		String result = Page.open("Pay Tuition");
		result += "<h2>Pay Tuition: " + st.getStudentId() + " " + st.fullName() + "</h2>";
		result += buildHTMLStatement(st);

		result += "<h3>New Payment</h3>";
		result += "<form method=\"post\" action=\"" + Page.APP + "/rest/Payment/pay\">";
		result += "<input type=\"hidden\" name=\"studentIdParam\" value=\"" + st.getStudentId() + "\">";
		result += "<p><label>Amount ($): <input type=\"text\" name=\"amountParam\" size=\"10\"></label></p>";
		result += "<p><label>Payment method: <select name=\"methodParam\">"
				+ "<option value=\"Credit\">Credit card</option><option value=\"Debit\">Debit card</option>"
				+ "<option value=\"Interac\">Interac e-Transfer</option></select></label></p>";
		result += "<input type=\"submit\" value=\"Pay\">";
		result += "</form>";
		result += Page.close();

		return result;
	}

	// Business process 5: pay tuition
	// updated: POST with form parameters instead of GET, and a login is required.
	// The id and the amount arrive as text and are converted here.
	@POST
	@Path("/pay")
	@Produces(MediaType.TEXT_HTML)
	public String payHTMLTuition(@CookieParam("authToken") String token,
			@FormParam("studentIdParam") String studentIdText, @FormParam("amountParam") String amountText,
			@FormParam("methodParam") String method) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		int studentId = current.studentIdFor(Validation.toInt(studentIdText));
		if (studentId < 0) {
			return Page.message("Payment REST Resource", "Payment REJECTED",
					"The student id must be a whole number.");
		}

		Payment p = processPayment(studentId, Validation.toDouble(amountText), method);

		String result = Page.open("Payment REST Resource");
		result += "<h2>Payment " + p.getStatus() + "</h2>";
		result += "<table border=\"1\">";
		result += "<tr><td>Receipt No</td><td>" + p.getReceiptNo() + "</td></tr>";
		result += "<tr><td>Date</td><td>" + p.getDate() + "</td></tr>";
		result += "<tr><td>Student</td><td>" + p.getStudentId() + " " + p.getStudentName() + "</td></tr>";
		result += "<tr><td>Amount Paid</td><td>" + Page.money(p.getAmountPaid()) + "</td></tr>";
		result += "<tr><td>Method</td><td>" + p.getPaymentMethod() + "</td></tr>";
		result += "<tr><td>Message</td><td>" + p.getMessage() + "</td></tr>";
		result += "<tr><td>Balance</td><td>" + Page.money(p.getNewBalance()) + "</td></tr>";
		result += "</table>";
		result += "<p><a href=\"" + Page.APP + "/rest/Payment/balance/" + p.getStudentId()
				+ "\">Tuition balance</a></p>";
		result += Page.close();

		return result;
	}

	// Business process 5: same rules as payHTMLTuition, for JSON clients
	// updated: POST with form parameters, login required
	@POST
	@Path("/pay")
	@Produces(MediaType.APPLICATION_JSON)
	public Payment payJSONTuition(@CookieParam("authToken") String token,
			@FormParam("studentIdParam") String studentIdText, @FormParam("amountParam") String amountText,
			@FormParam("methodParam") String method) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return new Payment("", 0, "", 0, "", "REJECTED", "Login required.", 0);
		}

		int studentId = current.studentIdFor(Validation.toInt(studentIdText));
		if (studentId < 0) {
			return new Payment("", 0, "", 0, "", "REJECTED", "The student id must be a whole number.", 0);
		}

		return processPayment(studentId, Validation.toDouble(amountText), method);
	}

	// Business process 5: check payment status
	// updated: requires a login; a student account receives its own balance only
	@GET
	@Path("/balance/{studentIdParam}")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLBalance(@CookieParam("authToken") String token,
			@PathParam("studentIdParam") int studentId) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null) {
			return Page.denied(current, "STUDENT");
		}

		studentId = current.studentIdFor(studentId);
		Student st = CourseRegistrationData.findStudent(studentId);
		if (st == null) {
			return Page.message("Payment REST Resource", "Student " + studentId + " was not found.", "");
		}

		return Page.open("Payment REST Resource") + "<h2>Tuition Balance: " + st.fullName() + "</h2>"
				+ buildHTMLStatement(st) + Page.close();
	}

	// updated: without a staff login the JSON list comes back empty
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Payment> displayJSONPaymentInfo(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.CLERK) {
			return new ArrayList<Payment>();
		}
		return CourseRegistrationData.paymentList;
	}
}
