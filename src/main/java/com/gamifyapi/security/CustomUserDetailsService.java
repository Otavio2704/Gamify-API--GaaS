package com.gamifyapi.security;

import com.gamifyapi.entity.Tenant;
import com.gamifyapi.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementa UserDetailsService para autenticação JWT via email do tenant.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final TenantRepository tenantRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Tenant tenant = tenantRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Tenant não encontrado com e-mail: " + email));
        return new TenantUserDetails(tenant);
    }
}
