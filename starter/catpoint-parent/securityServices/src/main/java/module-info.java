module securityServices {

    requires imageServices;

    requires com.google.gson;

    requires com.google.common;

    requires java.desktop;

    requires java.prefs;

    requires com.miglayout.swing;

    exports com.udacity.catpoint.application;

    exports com.udacity.catpoint.data;

    exports com.udacity.catpoint.security.service;
}