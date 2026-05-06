package com.seabattle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class PlayerStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nickname;
    private String city;
    private int wins;
    private int losses;
    private int shotsFired;
    private int hits;
    private double accuracy;

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getShotsFired() { return shotsFired; }
    public void setShotsFired(int shotsFired) { this.shotsFired = shotsFired; }
    public int getHits() { return hits; }
    public void setHits(int hits) { this.hits = hits; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
}
