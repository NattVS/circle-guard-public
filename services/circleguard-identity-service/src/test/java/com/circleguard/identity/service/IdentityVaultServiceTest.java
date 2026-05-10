package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVaultServiceTest {

    @Mock
    private IdentityMappingRepository repository;

    @InjectMocks
    private IdentityVaultService vaultService;

    private final String testSalt = "test-salt-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vaultService, "hashSalt", testSalt);
    }

    @Test
    void getOrCreateAnonymousId_WhenIdentityDoesNotExist_CreatesNew() {
        String realIdentity = "STUDENT|12345@uni.edu";
        UUID expectedUuid = UUID.randomUUID();
        
        IdentityMapping savedMapping = IdentityMapping.builder().anonymousId(expectedUuid).build();
        
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenReturn(savedMapping);

        UUID result = vaultService.getOrCreateAnonymousId(realIdentity);

        assertNotNull(result);
        assertEquals(expectedUuid, result);
        verify(repository, times(1)).save(any(IdentityMapping.class));
    }

    @Test
    void getOrCreateAnonymousId_WhenIdentityExists_ReturnsExistingId() {
        String realIdentity = "STUDENT|12345@uni.edu";
        UUID existingUuid = UUID.randomUUID();
        
        IdentityMapping existingMapping = IdentityMapping.builder()
                .anonymousId(existingUuid)
                .realIdentity(realIdentity)
                .build();

        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.of(existingMapping));

        UUID result = vaultService.getOrCreateAnonymousId(realIdentity);

        assertEquals(existingUuid, result);
        verify(repository, never()).save(any(IdentityMapping.class));
    }

    @Test
    void getOrCreateAnonymousId_ConsistentHashing() {
        String realIdentity = "VISITOR|test@test.com";
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenReturn(IdentityMapping.builder().anonymousId(UUID.randomUUID()).build());

        vaultService.getOrCreateAnonymousId(realIdentity);
        vaultService.getOrCreateAnonymousId(realIdentity);

        verify(repository, times(2)).findByIdentityHash(anyString());
    }

    @Test
    void resolveRealIdentity_WhenIdExists_ReturnsRealIdentity() {
        UUID anonymousId = UUID.randomUUID();
        String expectedIdentity = "PROFESSOR|smith@uni.edu";
        
        IdentityMapping mapping = IdentityMapping.builder()
                .realIdentity(expectedIdentity)
                .build();

        when(repository.findById(anonymousId)).thenReturn(Optional.of(mapping));

        String result = vaultService.resolveRealIdentity(anonymousId);

        assertEquals(expectedIdentity, result);
    }

    @Test
    void resolveRealIdentity_WhenIdDoesNotExist_ThrowsException() {
        UUID anonymousId = UUID.randomUUID();
        when(repository.findById(anonymousId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, 
                () -> vaultService.resolveRealIdentity(anonymousId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Identity not found", exception.getReason());
    }
}