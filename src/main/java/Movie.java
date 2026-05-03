import java.util.ArrayList;

public class Movie {
    private String movieID;
    private String title;
    private int year;
    private String director;
    private ArrayList<String> genres;
    private boolean isValid;
    private String errInfo;
    public Movie(){
         genres = new ArrayList<>();
         isValid = true;
    }
    public Movie(String movieID, String title, int year, String director){
        genres = new ArrayList<>();
        this.director = director;
        this.movieID = movieID;
        this.title = title;
        this.year = year;
        isValid = true;
    }

    public int getYear() {
        return year;
    }
    public void setYear(int year) {this.year= year;}

    public String getMovieID(){
        return movieID;
    }
    public void setMovieID(String id) {movieID = id;}
    public String getTitle(){
        return title;
    }
    public void setTitle(String title) {this.title = title;}
    public String getDirector(){
        return director;
    }
    public void setDir(String name) {director = name;}
    public ArrayList<String> getGenres(){
        return genres;
    }
    public void setGenres(String genre) {genres.add(genre);}
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Movie: "+ getTitle());
        sb.append(", ");
        sb.append("ID: "+ getMovieID());
        sb.append(", ");
        sb.append("year: "+ getYear());
        sb.append(", ");
        sb.append("director: "+ getDirector());
        sb.append(", ");
        sb.append("Genres: ");
        String result = String.join(", ", genres);
        sb.append(result);
        return sb.toString();
    }
    public void setValid(){
        isValid = true;
    }
    public void setInvalid(){
        isValid = false;
    }
    public boolean isValid(){
        return this.isValid;
    }
    public void setErrInfo(String err){
        errInfo=err;
    }
    public String getErrInfo(){return errInfo;}
}
