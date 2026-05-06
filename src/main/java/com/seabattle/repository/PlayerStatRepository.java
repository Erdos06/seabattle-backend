package com.seabattle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seabattle.model.PlayerStat;

public interface PlayerStatRepository extends JpaRepository<PlayerStat, Long> {
    Optional<PlayerStat> findByNicknameAndCity(String nickname, String city);
    List<PlayerStat> findTop10ByCityOrderByWinsDescAccuracyDesc(String city);
}
