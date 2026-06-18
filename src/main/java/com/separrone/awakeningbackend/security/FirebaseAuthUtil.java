package com.separrone.awakeningbackend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FirebaseAuthUtil {

    public static void requireAuthentication(HttpServletRequest request) {
        Boolean authenticated = (Boolean) request.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    public static String getFirebaseUid(HttpServletRequest request) {
        requireAuthentication(request);
        return (String) request.getAttribute("firebaseUid");
    }

    public static String getFirebaseEmail(HttpServletRequest request) {
        requireAuthentication(request);
        return (String) request.getAttribute("firebaseEmail");
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        Boolean authenticated = (Boolean) request.getAttribute("authenticated");
        return authenticated != null && authenticated;
    }
}