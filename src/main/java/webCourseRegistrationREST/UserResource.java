package webCourseRegistrationREST;

import java.io.IOException;
import java.util.ArrayList;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("User")
public class UserResource {

	private int nextUserId() {
		ArrayList<User> userList = CourseRegistrationData.userList;
		int maxId = 0;
		for (int i = 0; i < userList.size(); i++) {
			if (userList.get(i).getUserId() > maxId) {
				maxId = userList.get(i).getUserId();
			}
		}
		return maxId + 1;
	}

	private String buildHTMLRow(User u) {
		// only the first characters of the hash are shown: enough to see that no password is stored
		return "<tr><td class=\"num\">" + u.getUserId() + "</td><td>" + u.getUsername() + "</td><td>" + u.getRole()
				+ "</td><td class=\"num\">" + (u.getStudentId() == 0 ? "" : "" + u.getStudentId()) + "</td><td>"
				+ u.getPasswordHash().substring(0, 16) + "...</td></tr>";
	}

	private String tableHeading() {
		return "<table border=\"1\"><tr><th class=\"num\">User Id</th><th>Username</th><th>Role</th><th class=\"num\">Student Id</th>"
				+ "<th>Password (SHA-256 hash)</th></tr>";
	}

	private String entry(String link, String text) {
		return "<li><a href=\"" + Page.APP + "/" + link + "\">" + text + "</a></li>";
	}

	// The main menu, built for the person who asks for it: an entry is listed only when the access level
	// of that person allows the operation. Nobody logged in sees the public entries only.
	// Hiding an entry is a convenience, not the protection: every resource method checks the level again.
	private String buildHTMLMenu(User u) {

		int level = (u == null) ? -1 : u.accessLevel();

		String result = "<div class=\"menu\">";

		result += "<fieldset><legend>Account</legend><ul>";
		if (u == null) {
			result += entry("login.html", "Login");
			result += entry("signup.html", "Student Registration");
		} else {
			result += "<li>" + u.getUsername() + " (" + u.getRole() + ")</li>";
			result += entry("rest/User/logout", "Logout");
		}
		result += "</ul></fieldset>";

		if (level >= User.CLERK) {
			result += "<fieldset><legend>Input Data</legend><ul>";
			result += entry("studentForm.html", "Student");
			if (level >= User.MANAGER) {
				result += entry("courseForm.html", "Course");
				result += entry("sectionForm.html", "Section");
				result += entry("departmentForm.html", "Department");
				result += entry("instructorForm.html", "Instructor");
			}
			if (level >= User.ADMIN) {
				result += entry("userForm.html", "Staff Account");
			}
			result += "</ul></fieldset>";
		}

		result += "<fieldset><legend>Transactions</legend><ul>";
		result += entry("catalog.html", "Search the Catalog");
		// a student is already identified by the login, so these entries open the student's own
		// reservation and payment pages directly. The staff first searches the student by id.
		if (level == User.STUDENT) {
			result += entry("rest/Section/reserveForm", "Reserve a Seat");
			result += entry("rest/Payment/payForm", "Pay Tuition");
			result += entry("rest/Student/profile/" + u.getStudentId(), "My Profile");
		}
		if (level >= User.CLERK) {
			result += entry("reserveForm.html", "Reserve a Seat for a Student");
			result += entry("payForm.html", "Record a Tuition Payment");
		}
		result += "</ul></fieldset>";

		result += "<fieldset><legend>Output</legend><ul>";
		result += entry("rest/Course", "Course Catalog");
		result += entry("rest/Section", "Sections and Seats");
		result += entry("rest/Department", "Departments");
		result += entry("rest/Instructor", "Instructors");
		if (level >= User.CLERK) {
			result += entry("rest/Student", "Students");
			result += entry("rest/Report/enrollments", "Enrollments");
			result += entry("rest/Report/payments", "Payments");
		}
		if (level >= User.MANAGER) {
			result += entry("rest/Report/enrollmentSummary", "Enrollment Summary");
			result += entry("rest/Report/exceptions", "Exception Report");
		}
		if (level >= User.ADMIN) {
			result += entry("rest/User", "Login Accounts");
		}
		result += "</ul></fieldset>";

		result += "</div>";
		return result;
	}

	@GET
	@Path("/menu")
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLMenu(@CookieParam("authToken") String token) {

		User u = SessionManager.getInstance().currentUser(token);

		String result = Page.open("Course Registration System");
		result += "<h2>Main Menu</h2>";
		if (u == null) {
			result += "<p>The catalog is open to everyone. Log in to reserve a seat or to pay tuition.</p>";
		}
		result += buildHTMLMenu(u);
		result += Page.close();

		return result;
	}

	// The small line at the right of the top bar of every page: who is logged in, with the Logout link,
	// or "Not logged in" with the Login link. Each page embeds this resource in an iframe, so static pages
	// show it too. no-store tells the browser to ask again every time and never reuse an old answer.
	@GET
	@Path("/status")
	@Produces(MediaType.TEXT_HTML)
	public Response displayHTMLStatus(@CookieParam("authToken") String token) {

		User u = SessionManager.getInstance().currentUser(token);

		String result = "<html><head><link rel=\"stylesheet\" href=\"" + Page.APP + "/style.css?v=4\"></head>"
				+ "<body class=\"status\">";
		if (u == null) {
			result += "Not logged in | <a target=\"_top\" href=\"" + Page.APP + "/login.html\">Login</a>";
		} else {
			result += "Logged in: " + u.getUsername() + " (" + u.getRole() + ") | <a target=\"_top\" href=\""
					+ Page.APP + "/rest/User/logout\">Logout</a>";
		}
		result += "</body></html>";

		return Response.ok(result).header("Cache-Control", "no-store").build();
	}

	// Login. The form sends the username and the password in the body of a POST request.
	// When they match an account, a session is opened and its token goes back to the client in a
	// Set-Cookie header. The browser (or the Java client) sends that cookie with every later request,
	// which is how the other resources know who is calling.
	@POST
	@Path("/login")
	@Produces(MediaType.TEXT_HTML)
	public Response loginHTMLUser(@FormParam("usernameParam") String username,
			@FormParam("passwordParam") String password) throws IOException {

		CourseRegistrationData.loadData();

		if (Validation.isEmpty(username) || Validation.isEmpty(password)) {
			return Response.ok(Page.message("Login", "Login REJECTED", "Username and password are both required."))
					.build();
		}

		User u = CourseRegistrationData.findUserByName(username.trim());

		// same message for an unknown username and for a wrong password, so the page does not reveal which accounts exist
		if (u == null || !u.passwordMatches(password)) {
			return Response.ok(Page.message("Login", "Login REJECTED", "Wrong username or password.")).build();
		}

		String token = SessionManager.getInstance().open(u);

		String result = Page.open("Login");
		result += "<h2>Login CONFIRMED</h2>";
		result += "<table border=\"1\">";
		result += "<tr><td>Username</td><td>" + u.getUsername() + "</td></tr>";
		result += "<tr><td>Access level</td><td>" + u.getRole() + "</td></tr>";
		if (u.accessLevel() == User.STUDENT) {
			Student st = CourseRegistrationData.findStudent(u.getStudentId());
			result += "<tr><td>Student</td><td>" + u.getStudentId() + " " + (st == null ? "" : st.fullName())
					+ "</td></tr>";
		}
		result += "</table>";
		result += "<h2>Main Menu</h2>";
		result += buildHTMLMenu(u);
		result += Page.close();

		return Response.ok(result)
				.header("Set-Cookie", "authToken=" + token + "; Path=" + Page.APP + "; HttpOnly")
				.build();
	}

	// Logout closes the session on the server and tells the client to delete the cookie (Max-Age=0).
	@GET
	@Path("/logout")
	@Produces(MediaType.TEXT_HTML)
	public Response logoutHTMLUser(@CookieParam("authToken") String token) {

		SessionManager.getInstance().close(token);

		String result = Page.open("Logout") + "<h2>Logout CONFIRMED</h2><p>The session is closed.</p>"
				+ buildHTMLMenu(null) + Page.close();

		return Response.ok(result)
				.header("Set-Cookie", "authToken=; Path=" + Page.APP + "; Max-Age=0")
				.build();
	}

	// Staff accounts are created by the admin only. Student accounts are created by the registration process.
	@POST
	@Path("/add")
	@Produces(MediaType.TEXT_HTML)
	public String addHTMLUser(@CookieParam("authToken") String token, @FormParam("usernameParam") String username,
			@FormParam("passwordParam") String password, @FormParam("roleParam") String role) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.ADMIN) {
			return Page.denied(current, "ADMIN");
		}

		username = Validation.clean(username);

		if (Validation.isEmpty(username) || Validation.isEmpty(password) || Validation.isEmpty(role)) {
			return Page.message("Staff Account", "Account REJECTED", "Username, password and role are all required.");
		}
		if (username.contains(" ")) {
			return Page.message("Staff Account", "Account REJECTED", "The username cannot contain spaces.");
		}
		if (password.length() < 6) {
			return Page.message("Staff Account", "Account REJECTED", "The password needs at least 6 characters.");
		}
		if (!role.equals("CLERK") && !role.equals("MANAGER") && !role.equals("ADMIN")) {
			return Page.message("Staff Account", "Account REJECTED", "The role must be CLERK, MANAGER or ADMIN.");
		}
		if (CourseRegistrationData.findUserByName(username) != null) {
			return Page.message("Staff Account", "Account REJECTED",
					"The username " + username + " is already taken.");
		}

		User u = new User(nextUserId(), username, User.hashPassword(password), role, 0);
		CourseRegistrationData.userList.add(u);
		CourseRegistrationData.insertUser(u); // updated: saved in SQL Server

		return buildHTMLResult("Account CONFIRMED", u);
	}

	// the account form with its fields filled from the record, ready for Update or Delete.
	// The password field stays empty: the table holds the hash only, the password itself cannot be shown.
	private String buildHTMLResult(String heading, User u) {
		return Page.open("Staff Account") + "<h2>" + heading + "</h2>" + tableHeading() + buildHTMLRow(u)
				+ "</table>" + Page.formOpen("Account")
				+ Page.field("Account Id:", "userIdParam", "" + u.getUserId())
				+ Page.field("Username:", "usernameParam", u.getUsername())
				+ "<label class=\"field\">New password (empty = keep):</label>"
				+ "<input type=\"password\" name=\"passwordParam\"><br>"
				+ "<label class=\"field\">Access level:</label>" + roleList(u.getRole())
				+ Page.formClose("Account", "User") + Page.close();
	}

	// updated: the access level is chosen from a list, like on userForm.html, with the level of the record
	// selected. A student account keeps STUDENT only, since its level is not changed from this form.
	private String roleList(String role) {
		String[] roles = { "CLERK", "MANAGER", "ADMIN" };
		if (role.equals("STUDENT")) {
			roles = new String[] { "STUDENT" };
		}
		String result = "<select name=\"roleParam\">";
		for (int i = 0; i < roles.length; i++) {
			result += "<option value=\"" + roles[i] + "\"";
			if (roles[i].equals(role)) {
				result += " selected";
			}
			result += ">" + roles[i].substring(0, 1) + roles[i].substring(1).toLowerCase() + "</option>";
		}
		return result + "</select><br>";
	}

	private int countAdmins() {
		ArrayList<User> userList = CourseRegistrationData.userList;
		int count = 0;
		for (int i = 0; i < userList.size(); i++) {
			if (userList.get(i).accessLevel() == User.ADMIN) {
				count++;
			}
		}
		return count;
	}

	// Search button: reads the record from the User table and returns the form filled with it
	@POST
	@Path("/find")
	@Produces(MediaType.TEXT_HTML)
	public String findHTMLUser(@CookieParam("authToken") String token, @FormParam("userIdParam") String userIdText)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.ADMIN) {
			return Page.denied(current, "ADMIN");
		}

		int userId = Validation.toInt(userIdText);
		if (userId < 0) {
			return Page.message("Staff Account", "Search REJECTED", "The account id must be a whole number.");
		}

		User u = CourseRegistrationData.findUser(userId);
		if (u == null) {
			return Page.message("Staff Account", "Account " + userId + " was not found.", "");
		}
		return buildHTMLResult("Account Found", u);
	}

	// Update button (ADMIN): changes the access level of a staff account, and the password when one is typed.
	// The username stays, and a student account keeps the STUDENT level: only its password can be reset here.
	@POST
	@Path("/update")
	@Produces(MediaType.TEXT_HTML)
	public String updateHTMLUser(@CookieParam("authToken") String token, @FormParam("userIdParam") String userIdText,
			@FormParam("passwordParam") String password, @FormParam("roleParam") String role) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.ADMIN) {
			return Page.denied(current, "ADMIN");
		}

		User u = CourseRegistrationData.findUser(Validation.toInt(userIdText));
		if (u == null) {
			return Page.message("Staff Account", "Update REJECTED",
					"Account " + Validation.clean(userIdText) + " was not found. Search it first.");
		}

		role = Validation.clean(role).toUpperCase();
		if (u.accessLevel() == User.STUDENT) {
			role = "STUDENT";
		} else if (!role.equals("CLERK") && !role.equals("MANAGER") && !role.equals("ADMIN")) {
			return Page.message("Staff Account", "Update REJECTED", "The role must be CLERK, MANAGER or ADMIN.");
		}
		// the system must always keep one account able to manage the others
		if (u.accessLevel() == User.ADMIN && !role.equals("ADMIN") && countAdmins() == 1) {
			return Page.message("Staff Account", "Update REJECTED",
					u.getUsername() + " is the only ADMIN account, its access level cannot be lowered.");
		}
		if (!Validation.isEmpty(password) && password.length() < 6) {
			return Page.message("Staff Account", "Update REJECTED", "The password needs at least 6 characters.");
		}

		u.setRole(role);
		if (!Validation.isEmpty(password)) {
			u.setPasswordHash(User.hashPassword(password));
		}
		CourseRegistrationData.updateUser(u); // updated: saved in SQL Server

		return buildHTMLResult("Account UPDATED", u);
	}

	// Delete button (ADMIN). Refused for the account in use, for the last ADMIN, and for a student account,
	// which is removed together with its student from the student form.
	@POST
	@Path("/delete")
	@Produces(MediaType.TEXT_HTML)
	public String deleteHTMLUser(@CookieParam("authToken") String token, @FormParam("userIdParam") String userIdText)
			throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.ADMIN) {
			return Page.denied(current, "ADMIN");
		}

		User u = CourseRegistrationData.findUser(Validation.toInt(userIdText));
		if (u == null) {
			return Page.message("Staff Account", "Delete REJECTED",
					"Account " + Validation.clean(userIdText) + " was not found.");
		}
		if (u.getUserId() == current.getUserId()) {
			return Page.message("Staff Account", "Delete REJECTED", "The account in use cannot delete itself.");
		}
		if (u.accessLevel() == User.STUDENT) {
			return Page.message("Staff Account", "Delete REJECTED",
					"A student account is removed with its student, from the student form.");
		}
		if (u.accessLevel() == User.ADMIN && countAdmins() == 1) {
			return Page.message("Staff Account", "Delete REJECTED",
					u.getUsername() + " is the only ADMIN account.");
		}

		CourseRegistrationData.userList.remove(u);
		CourseRegistrationData.deleteUser(u.getUserId()); // updated: removed from SQL Server
		SessionManager.getInstance().closeUser(u.getUserId());

		return Page.message("Staff Account", "Account DELETED",
				"Account " + u.getUserId() + " " + u.getUsername() + " was removed.");
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String displayHTMLUserInfo(@CookieParam("authToken") String token) throws IOException {

		CourseRegistrationData.loadData();
		User current = SessionManager.getInstance().currentUser(token);
		if (current == null || current.accessLevel() < User.ADMIN) {
			return Page.denied(current, "ADMIN");
		}

		ArrayList<User> userList = CourseRegistrationData.userList;
		String result = Page.open("Login Accounts") + "<h2>Login Accounts</h2>" + tableHeading();
		for (int i = 0; i < userList.size(); i++) {
			result += buildHTMLRow(userList.get(i));
		}
		result += "</table><h3>Accounts: " + userList.size() + "</h3>" + Page.close();

		return result;
	}
}
