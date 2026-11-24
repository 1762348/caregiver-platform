package com.youhu.caregiver.auth.repository;

import com.youhu.caregiver.auth.domain.RolePermission;
import com.youhu.caregiver.user.domain.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole(UserRole role);

    boolean existsByRoleAndPermissionId(UserRole role, Long permissionId);

    @Query("""
            select p.code
              from Permission p, RolePermission rp
             where rp.permissionId = p.id
               and rp.role = :role
            """)
    List<String> findPermissionCodesByRole(@Param("role") UserRole role);
}
