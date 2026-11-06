package webCourseRegistrationREST;

// Input checks shared by the resources. Form fields arrive as text and are converted here,
// so a wrong value gets a message written by the service instead of a 404 from the framework.
public class Validation {

	public static boolean isEmpty(String value) {
		return value == null || value.trim().isEmpty();
	}

	// removes the characters that would break a tab delimited line or an HTML page
	public static String clean(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\t", " ").replace("\r", " ").replace("\n", " ").replace("<", "").replace(">", "")
				.trim();
	}

	// returns -1 when the text is not a whole number
	public static int toInt(String value) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException | NullPointerException e) {
			return -1;
		}
	}

	// returns -1 when the text is not a number
	public static double toDouble(String value) {
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException | NullPointerException e) {
			return -1;
		}
	}

	public static boolean isEmail(String email) {
		return email.contains("@") && email.contains(".") && !email.contains(" ");
	}

	// accepts a phone typed in any format, as long as it holds 10 digits, and returns it as 514-555-0199;
	// returns an empty text when the phone is not valid
	public static String formatPhone(String phone) {
		String digits = "";
		for (int i = 0; i < phone.length(); i++) {
			if (Character.isDigit(phone.charAt(i))) {
				digits += phone.charAt(i);
			}
		}
		if (digits.length() != 10) {
			return "";
		}
		return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
	}

	// an address needs a civic number and a street name
	public static boolean isAddress(String address) {
		boolean hasDigit = false;
		boolean hasLetter = false;
		for (int i = 0; i < address.length(); i++) {
			if (Character.isDigit(address.charAt(i))) {
				hasDigit = true;
			}
			if (Character.isLetter(address.charAt(i))) {
				hasLetter = true;
			}
		}
		return hasDigit && hasLetter && address.trim().length() >= 8;
	}
}
