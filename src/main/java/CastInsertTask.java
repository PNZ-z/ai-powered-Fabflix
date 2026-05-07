public class CastInsertTask {
    private final String starId;
    private final String starName;
    private final String movieId;
    private final boolean newStar;
    private final boolean poisonPill;

    public CastInsertTask(String starId, String starName, String movieId, boolean newStar) {
        this.starId = starId;
        this.starName = starName;
        this.movieId = movieId;
        this.newStar = newStar;
        this.poisonPill = false;
    }

    private CastInsertTask() {
        this.starId = null;
        this.starName = null;
        this.movieId = null;
        this.newStar = false;
        this.poisonPill = true;
    }

    public static CastInsertTask poisonPill() {
        return new CastInsertTask();
    }

    public String getStarId() {
        return starId;
    }

    public String getStarName() {
        return starName;
    }

    public String getMovieId() {
        return movieId;
    }

    public boolean isNewStar() {
        return newStar;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }
}
