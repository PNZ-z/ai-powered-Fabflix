import java.util.List;

public class MovieInsertTask {
    private final Movie movie;
    private final List<Integer> genreIds;
    private final boolean poisonPill;

    public MovieInsertTask(Movie movie, List<Integer> genreIds) {
        this.movie = movie;
        this.genreIds = genreIds;
        this.poisonPill = false;
    }

    private MovieInsertTask() {
        this.movie = null;
        this.genreIds = null;
        this.poisonPill = true;
    }

    public static MovieInsertTask poisonPill() {
        return new MovieInsertTask();
    }

    public Movie getMovie() {
        return movie;
    }

    public List<Integer> getGenreIds() {
        return genreIds;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }
}