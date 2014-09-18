package com.transform.poc;

/**
 * Created by synhershko on 9/17/14.
 */
public class TagMatch {
    private String tag;
    private float score;

    public TagMatch(String tag, float score) {
        this.tag = tag;
        this.score = score;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String toString(){
        if (score != Float.MIN_VALUE)
            return tag + " (" + score + ")";
        else
            return tag;
    }
}
