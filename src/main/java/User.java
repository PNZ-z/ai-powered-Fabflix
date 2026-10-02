/**
 * This User class only has the username field in this example.
 * You can add more attributes such as the user's shopping cart items.
 */
import com.fabflix.boot.auth.CustomerIdentity;

public class User implements CustomerIdentity {

    private final String username;

    public User(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return username;
    }

}
