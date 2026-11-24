package com.youhu.caregiver.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.youhu.caregiver.auth.repository.RolePermissionRepository;
import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.user.domain.UserRole;
import com.youhu.caregiver.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryQueryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaregiverRepository caregiverRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Test
    void findUserByPhone() {
        assertThat(userRepository.findByPhone("13800000002"))
                .get()
                .satisfies(user -> {
                    assertThat(user.getName()).isEqualTo("张三");
                    assertThat(user.getRole()).isEqualTo(UserRole.USER);
                });
    }

    @Test
    void findCaregiverByCodeWithCategory() {
        Caregiver caregiver = caregiverRepository.findByCode("CG0001").orElseThrow();

        assertThat(caregiver.getName()).isEqualTo("李秀兰");
        assertThat(caregiver.getCategoryId()).isEqualTo(1L);
        assertThat(caregiver.getRating()).isEqualByComparingTo("4.9");
    }

    @Test
    void permissionCodesAreBoundToRoles() {
        assertThat(rolePermissionRepository.findPermissionCodesByRole(UserRole.ADMIN))
                .contains("admin:all", "caregiver:edit", "order:edit")
                .hasSize(7);

        assertThat(rolePermissionRepository.findPermissionCodesByRole(UserRole.USER))
                .containsExactlyInAnyOrder("caregiver:view", "order:view");

        assertThat(rolePermissionRepository.findPermissionCodesByRole(UserRole.CAREGIVER))
                .containsExactlyInAnyOrder("caregiver:view", "order:view", "order:edit");
    }
}
