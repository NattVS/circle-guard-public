package com.circleguard.auth.service;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.model.Role;
import com.circleguard.auth.model.Permission;
import com.circleguard.auth.repository.LocalUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    @Mock
    private LocalUserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        LocalUser user = new LocalUser();
        user.setUsername("admin");
        user.setPassword("hash");
        user.setIsActive(true);
        Role role = new Role();
        role.setName("ADMIN");
        role.setPermissions(java.util.Collections.emptySet());
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertNotNull(details);
        assertEquals("admin", details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("unknown");
        });
    }

    @Test
    void loadUserByUsername_UserDisabled_ThrowsDisabledException() {
        LocalUser user = new LocalUser();
        user.setUsername("disabled");
        user.setPassword("hash");
        user.setIsActive(false);

        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> {
            userDetailsService.loadUserByUsername("disabled");
        });
    }

    @Test
    void loadUserByUsername_UserWithPermissions_ReturnsAuthorities() {
        LocalUser user = new LocalUser();
        user.setUsername("user_perms");
        user.setPassword("hash");
        user.setIsActive(true);
        
        Role role = new Role();
        role.setName("USER");
        Permission p = new Permission();
        p.setName("READ_DATA");
        role.setPermissions(Set.of(p));
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("user_perms")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("user_perms");

        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("READ_DATA")));
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_MultipleRoles_ReturnsAllAuthorities() {
        LocalUser user = new LocalUser();
        user.setUsername("multi_role");
        user.setPassword("hash");
        user.setIsActive(true);
        
        Role r1 = new Role(); r1.setName("USER"); r1.setPermissions(Set.of());
        Role r2 = new Role(); r2.setName("ADMIN"); r2.setPermissions(Set.of());
        user.setRoles(Set.of(r1, r2));

        when(userRepository.findByUsername("multi_role")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("multi_role");

        assertEquals(2, details.getAuthorities().size());
    }
}
