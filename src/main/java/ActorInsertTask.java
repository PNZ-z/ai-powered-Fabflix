public class ActorInsertTask {
    public enum TaskType {
        INSERT_STAR,
        UPDATE_BIRTH_YEAR,
        POISON_PILL
    }

    private final TaskType type;
    private final String starId;
    private final String starName;
    private final Integer birthYear;

    private ActorInsertTask(TaskType type, String starId, String starName, Integer birthYear) {
        this.type = type;
        this.starId = starId;
        this.starName = starName;
        this.birthYear = birthYear;
    }

    public static ActorInsertTask insertStar(String starId, String starName, Integer birthYear) {
        return new ActorInsertTask(TaskType.INSERT_STAR, starId, starName, birthYear);
    }

    public static ActorInsertTask updateBirthYear(String starId, Integer birthYear) {
        return new ActorInsertTask(TaskType.UPDATE_BIRTH_YEAR, starId, null, birthYear);
    }

    public static ActorInsertTask poisonPill() {
        return new ActorInsertTask(TaskType.POISON_PILL, null, null, null);
    }

    public TaskType getType() {
        return type;
    }

    public String getStarId() {
        return starId;
    }

    public String getStarName() {
        return starName;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public boolean isPoisonPill() {
        return type == TaskType.POISON_PILL;
    }
}
