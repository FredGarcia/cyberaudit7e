package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}