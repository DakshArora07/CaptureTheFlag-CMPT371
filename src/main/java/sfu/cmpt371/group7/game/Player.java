package sfu.cmpt371.group7.game;

public class Player {

    private String team;
    private int x;
    private int y;
    private String name;

    public Player(){
        team = "";
        x = 0;
        y = 0;
        name = "";
    }


    public Player(String team, int x, int y, String name){
        this.team = team;
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public void setTeam(String team){
        this.team = team;
    }

    public void setX(int x){
        this.x = x;
    }

    public void setY(int y){
        this.y = y;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getTeam(){
        return team;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public String getName(){
        return name;
    }





}