package com.seabattle.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.seabattle.model.PlayerStat;
import com.seabattle.repository.PlayerStatRepository;

@Service
public class LeaderboardService {
    private final PlayerStatRepository repository;
    private final WebClient webClient;

    @Value("${app.ipapi.base-url:http://ip-api.com/json/}")
    private String ipApiBaseUrl;

    public LeaderboardService(PlayerStatRepository repository, WebClient.Builder builder) {
        this.repository = repository;
        this.webClient = builder.build();
    }

    public String detectCity(String ip) {
        try {
            IpApiResponse response = webClient.get()
                    .uri(ipApiBaseUrl + ip + "?fields=status,city")
                    .retrieve()
                    .bodyToMono(IpApiResponse.class)
                    .block();
            if (response != null && "success".equalsIgnoreCase(response.status()) && response.city() != null) {
                return response.city();
            }
        } catch (Exception ignored) { }
        return "Unknown";
    }

    public PlayerStat upsertResult(String nickname, String city, boolean won, int shots, int hits) {
        PlayerStat stat = repository.findByNicknameAndCity(nickname, city).orElseGet(PlayerStat::new);
        stat.setNickname(nickname);
        stat.setCity(city);
        stat.setWins(stat.getWins() + (won ? 1 : 0));
        stat.setLosses(stat.getLosses() + (won ? 0 : 1));
        stat.setShotsFired(stat.getShotsFired() + shots);
        stat.setHits(stat.getHits() + hits);
        stat.setAccuracy(stat.getShotsFired() == 0 ? 0 : (100.0 * stat.getHits() / stat.getShotsFired()));
        return repository.save(stat);
    }

    public List<PlayerStat> topByCity(String city) {
        return repository.findTop10ByCityOrderByWinsDescAccuracyDesc(city);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IpApiResponse(String status, String city) {}
}
