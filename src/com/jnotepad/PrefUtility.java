/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jnotepad;

import java.util.prefs.Preferences;

/**
 *
 * @author Anthony
 */
public class PrefUtility {
    
    private final String SIZE_KEY = "size";
    private final String LOC_KEY = "location";
    
    Preferences preferences;
    private static PrefUtility instance = null;
    
    public static PrefUtility getInstance(){
        if(instance == null)
            instance = new PrefUtility();
        return instance;
    }
    
    public PrefUtility(){
        preferences = Preferences.userRoot().node("JNotepadPreferences");
    }
    
    public String getSize(){
        return preferences.get(SIZE_KEY, "650,400");
    }
    public String getLoc(){
        return preferences.get(LOC_KEY, "default");
    }
    
    public void setSize(String size){
        preferences.put(SIZE_KEY, size);
    }
    public void setLoc(String loc){
        preferences.put(LOC_KEY, loc);
    }
}
