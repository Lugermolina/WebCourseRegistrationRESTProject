// Show or hide the password of the field next to the button.
// The eye carries a diagonal line while the password is hidden.
function togglePassword(button) {
	var field = button.previousElementSibling;
	var line = button.querySelector("line");
	if (field.type === "password") {
		field.type = "text";
		line.style.display = "none";
	} else {
		field.type = "password";
		line.style.display = "";
	}
}
