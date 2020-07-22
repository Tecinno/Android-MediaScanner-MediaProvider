package com.czy.jni;

public class ListData {
    final static public int AUDIO = 1;
    final static public int FOLDER = 0;
    final static public int VIDEO = 2;
    private int id;
    private String name;
    private String path;
    public boolean isfolder;
    public int fileTypte;
    public ListData( int fileTypte) {
        this.fileTypte = fileTypte;
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
