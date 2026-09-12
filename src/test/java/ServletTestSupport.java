import java.lang.reflect.Field;

final class ServletTestSupport {

    private ServletTestSupport() {
    }

    static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
