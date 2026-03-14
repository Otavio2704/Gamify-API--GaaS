package com.gamifyapi.security;

import com.gamifyapi.entity.Tenant;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador de Tenant para UserDetails do Spring Security.
 */
public class TenantUserDetails implements UserDetails {

    private final Tenant tenant;

    public TenantUserDetails(Tenant tenant) {
        this.tenant = tenant;
    }

    public Tenant getTenant() {
        return tenant;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_TENANT"));
    }

    @Override
    public String getPassword() {
        return tenant.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return tenant.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
