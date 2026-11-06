package webCourseRegistrationREST;

// Opening and closing HTML shared by every page the resources produce,
// so all pages carry the same style sheet and the link back to the main menu.
public class Page {

	// context root of the application, needed because the pages are produced at different URL depths
	public static final String APP = "/WebCourseRegistrationRESTProject";

	public static String open(String title) {
		return "<html><head><title>" + title + "</title>"
				+ "<link rel=\"stylesheet\" href=\"" + APP + "/style.css?v=4\"></head><body>"
				+ "<div class=\"header\">Course Registration System</div>"
				+ "<p class=\"topbar\"><a href=\"" + APP + "/rest/User/menu\">Main Menu</a>"
				+ "<iframe class=\"status\" style=\"border:0; height:22px; width:520px\" src=\"" + APP
				+ "/rest/User/status\"></iframe></p>";
	}

	public static String close() {
		return "</body></html>";
	}

	// A refusal (REJECTED, not found) is shown in a framed box with a button back to the form.
	// history.back() is the JavaScript call behind the Back arrow of the browser: it returns to the
	// previous page, where the form still holds what was typed. Other messages are shown plainly.
	public static String message(String title, String heading, String text) {
		if (heading.contains("REJECTED") || heading.contains("not found")) {
			return open(title) + "<div class=\"rejected\"><h2>" + heading + "</h2><p>" + text + "</p>"
					+ "<button type=\"button\" onclick=\"history.back()\">Back to the form</button></div>"
					+ close();
		}
		return open(title) + "<h2>" + heading + "</h2><p>" + text + "</p>" + close();
	}

	// page returned when nobody is logged in, or when the logged in role is below the one required
	public static String denied(User user, String requiredRole) {
		String result = open("Access Denied") + "<h2>ACCESS DENIED</h2>";
		if (user == null) {
			result += "<p>Nobody is logged in. This operation requires the " + requiredRole + " access level.</p>"
					+ "<p><a href=\"" + APP + "/login.html\">Login</a></p>";
		} else {
			result += "<p>" + user.getUsername() + " is logged in as " + user.getRole()
					+ ". This operation requires the " + requiredRole + " access level.</p>";
		}
		return result + close();
	}

	// one labeled text field of a form produced by a resource, already holding its value
	public static String field(String label, String name, String value) {
		return "<label class=\"field\">" + label + "</label><input type=\"text\" name=\"" + name
				+ "\" value=\"" + value.replace("\"", "&quot;") + "\"><br>";
	}

	public static String formOpen(String entity) {
		return "<fieldset><legend>" + entity + " Form</legend><form method=\"post\">";
	}

	// the four buttons of an entity form. One form serves the four operations: the formaction
	// attribute of the button pressed decides which resource method receives the fields.
	public static String formClose(String entity, String resource) {
		String base = APP + "/rest/" + resource;
		return "<input type=\"submit\" value=\"Add " + entity + "\" formaction=\"" + base + "/add\">"
				+ "<input type=\"submit\" value=\"Update " + entity + "\" formaction=\"" + base + "/update\">"
				+ "<input type=\"submit\" value=\"Search " + entity + "\" formaction=\"" + base + "/find\">"
				+ "<input type=\"submit\" value=\"Delete " + entity + "\" formaction=\"" + base + "/delete\">"
				+ "</form></fieldset>";
	}

	public static String money(double amount) {
		return String.format("%.2f", amount) + "$";
	}
}
