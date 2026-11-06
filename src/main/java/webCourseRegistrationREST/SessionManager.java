package webCourseRegistrationREST;

import java.util.HashMap;
import java.util.UUID;

// pattern (Singleton): the whole application shares ONE SessionManager object.
// The constructor is private, so no other class can write "new SessionManager()";
// the only way to reach the object is getInstance(), which creates it the first time
// and returns that same object every time after. All four resources therefore look
// at the same table of logged in users.
public class SessionManager {

	private static SessionManager instance = null;

	// data structure (HashMap): key = the token sent to the browser as a cookie, value = the user who logged in.
	// A HashMap finds the user from the token directly, without traversing a list.
	private HashMap<String, User> sessions;

	private SessionManager() {
		sessions = new HashMap<String, User>();
	}

	public static synchronized SessionManager getInstance() {
		if (instance == null) {
			instance = new SessionManager();
		}
		return instance;
	}

	// called at login: the token is a random text nobody can guess, it stands for the user until logout
	public String open(User user) {
		String token = UUID.randomUUID().toString();
		sessions.put(token, user);
		return token;
	}

	// returns the logged in user that owns the token, or null when nobody is logged in
	public User currentUser(String token) {
		if (token == null) {
			return null;
		}
		return sessions.get(token);
	}

	public void close(String token) {
		if (token != null) {
			sessions.remove(token);
		}
	}

	// called when an account is deleted: its open sessions are closed as well.
	// lambda expression: u -> ... is a small function with no name; removeIf() runs it on every user
	// of the table and removes the entries for which it answers true.
	public void closeUser(int userId) {
		sessions.values().removeIf(u -> u.getUserId() == userId);
	}
}
