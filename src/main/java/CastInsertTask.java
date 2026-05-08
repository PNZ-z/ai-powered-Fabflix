public class CastInsertTask {
    private final String starId;
    private final String movieId;
    private final boolean poisonPill;

    public CastInsertTask(String starId,String movieId) {
        this.starId = starId;
        this.movieId = movieId;
        this.poisonPill = false;
    }

    private CastInsertTask() {
        this.starId = null;
        this.movieId = null;
        this.poisonPill = true;
    }

    public static CastInsertTask poisonPill() {
        return new CastInsertTask();
    }

    public String getStarId() {
        return starId;
    }


    public String getMovieId() {
        return movieId;
    }


    public boolean isPoisonPill() {
        return poisonPill;
    }
}
