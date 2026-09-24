public class RecaptchaConstants {
    private RecaptchaConstants() {
    }

    public static String getSecretKey() {
        String secretKey = System.getenv("RECAPTCHA_SECRET_KEY");
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("RECAPTCHA_SECRET_KEY is not configured");
        }
        return secretKey;
    }
}
