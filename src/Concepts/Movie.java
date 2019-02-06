package Concepts;

import jade.content.Concept;
import java.util.Date;
import jade.util.leap.List;

public class Movie implements Concept{
    
    private String name;
    private Date year;
    private String director;
    private List actors;
    
    public Movie(){}

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the year
     */
    public Date getYear() {
        return year;
    }

    /**
     * @param year the year to set
     */
    public void setYear(Date year) {
        this.year = year;
    }

    /**
     * @return the director
     */
    public String getDirector() {
        return director;
    }

    /**
     * @param director the director to set
     */
    public void setDirector(String director) {
        this.director = director;
    }

    /**
     * @return the actors
     */
    public List getActors() {
        return actors;
    }

    /**
     * @param actors the actors to set
     */
    public void setActors(List actors) {
        this.actors = actors;
    }
    
    public String toString(){
        return this.name;
    }
    
    
}
