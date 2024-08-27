package com.starise.ipn.MpesaIpn;

import com.starise.ipn.entity.TenantIdEntity;
import com.starise.ipn.repository.TenantIdRepository;
import com.starise.ipn.service.IpnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ipnServiceTest {

    @Mock
    private TenantIdRepository tenantIdRepository;

    @InjectMocks
    private IpnService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

//    @Test
//    void findUserById_UserExists_ReturnsUser() {
//        // Arrange
//        TenantIdEntity mockUser = new TenantIdEntity(1L, "John Doe");
//        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
//
//        // Act
//        TenantIdEntity result = userService.validateIdNo();
//
//        // Assert
//        assertNotNull(result);
//        assertEquals("John Doe", result.getName());
//    }
//
//    @Test
//    void findUserById_UserDoesNotExist_ThrowsException() {
//        // Arrange
//        when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(UserNotFoundException.class, () -> userService.findUserById(1L));
//    }
}
