package com.company;

public class Place {
    private double x;
    private double y;

    Place(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double distance(Place anotherPlace) {
        return Math.sqrt(Math.pow(x - anotherPlace.getX(), 2) +
                Math.pow(y - anotherPlace.getY(), 2));
    }

    public static void main(String[] args) {
        String string = new Place(1.34562, 3235435.424653).toString();
        string = ":" + string;
        string = string.substring(string.indexOf(":") + 1);
        double x = Double.parseDouble(string.substring(string.indexOf("=") + 1, string.indexOf(",")));
        double y = Double.parseDouble(string.substring(string.indexOf(",") + 3));
        System.out.println(x);
        System.out.println(y);

    }

    @Override
    // example : "Place:x=12.5,y=4.5"
    public String toString() {
        return "Place:" +
                "x=" + x +
                ",y=" + y;
    }

}
