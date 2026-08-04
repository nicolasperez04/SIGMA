package com.SIGMA.USCO.security;

import com.SIGMA.USCO.Users.Entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {


    public static User getCurrentUser (){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Cannot get current user. No authentication found in security context.");
        return (User) auth.getPrincipal();
    }
}
