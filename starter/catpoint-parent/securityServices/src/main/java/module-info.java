module securityServices {

    requires imageServices;

    requires java.desktop;

    requires java.prefs;

    requires com.google.gson;

    requires com.google.common;

    requires com.miglayout.swing;

    opens com.udacity.catpoint.data to com.google.gson;

}