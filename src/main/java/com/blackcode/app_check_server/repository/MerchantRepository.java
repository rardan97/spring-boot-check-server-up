package com.blackcode.app_check_server.repository;

import com.blackcode.app_check_server.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}
