package sfu.cmpt371.group7.game.logistics;

public class Player {

    private String team;
    private int x;
    private int y;
    private String name;
    private boolean capturedFlag;
    private boolean capturingStatus;

    // this is causing problem. need to pass agrs for the x and y coordinates
//    public Player(){
//        team = "";
//        x = 0;
//        y = 0;
//        name = "";
//        capturedFlag = false;
//    }


    public Player(String team, int x, int y, String name){
        this.team = team;
        this.x = x;
        this.y = y;
        this.name = name;
        this.capturedFlag = false;
        this.capturingStatus = false;
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

    public void setCapturedFlag(boolean capturedFlag){
        this.capturedFlag = capturedFlag;
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

    public boolean getCapturedFlag(){
        return capturedFlag;
    }

    public void setCapturingStatus(boolean flag){
        this.capturingStatus = flag;
    }

    public boolean getCapturingStatus(){
        return capturingStatus;
    }

}