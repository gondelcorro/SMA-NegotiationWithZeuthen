package Concepts;

import jade.content.Predicate;

public class IsMyZeuthen implements Predicate{
    
    private float value;

    public IsMyZeuthen(){}
    
    /**
     * @return the value
     */
    public float getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(float value) {
        this.value = value;
    }
    
}
