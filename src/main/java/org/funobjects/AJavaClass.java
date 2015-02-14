package org.funobjects;

import org.funobjects.r34.authentication.BearerToken;

/**
 * Created by rgf on 2/12/15.
 */
public class AJavaClass {

    public static void main(String[] args) {
        String s = "aid92";
        BearerToken bt = new BearerToken(s);
        System.out.println("Token: " + bt);
    }
}
