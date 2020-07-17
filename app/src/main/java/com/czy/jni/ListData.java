package com.czy.jni;

public class ListData {

    private int id;
    private String name;
    private String path;
    public boolean isfolder;
    public ListData( boolean isfolder) {
        this.isfolder = isfolder;
    }
    public int getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getPath() {
        return path;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
