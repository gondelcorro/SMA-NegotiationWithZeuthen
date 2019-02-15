package util;

import Concepts.Movie;
import java.util.Objects;

public class Utilidades implements Comparable<Utilidades> {

    private Movie movie;
    private Float utilidad;

    public Utilidades() {
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Float getUtilidad() {
        return utilidad;
    }

    public void setUtilidad(Float utilidad) {
        this.utilidad = utilidad;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.movie);
        hash = 29 * hash + Objects.hashCode(this.utilidad);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Utilidades other = (Utilidades) obj;
        if (!Objects.equals(this.movie, other.movie)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Utilidades u) {
        if (getUtilidad() == null || u.getUtilidad() == null) {
            return 0;
        }
        return getUtilidad().compareTo(u.getUtilidad());
    }

}
