package com.seabattle.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.seabattle.model.PlayerStat;
import com.seabattle.engine.GameEngine;
import com.seabattle.service.AiCoachService;
import com.seabattle.service.BotStrategyService;
import com.seabattle.service.LeaderboardService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class PlatformController {
    private final LeaderboardService leaderboardService;
    private final AiCoachService aiCoachService;
    private final BotStrategyService botStrategyService;
    private final GameEngine gameEngine;

    public PlatformController(LeaderboardService leaderboardService, AiCoachService aiCoachService,
                              BotStrategyService botStrategyService, GameEngine gameEngine) {
        this.leaderboardService = leaderboardService;
        this.aiCoachService = aiCoachService;
        this.botStrategyService = botStrategyService;
        this.gameEngine = gameEngine;
    }

    @GetMapping("/location")
    public Map<String, String> location(HttpServletRequest request,
                                        @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        String ip = forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        return Map.of("city", leaderboardService.detectCity(ip));
    }

    @PostMapping("/stats/result")
    public PlayerStat upsert(@RequestBody ResultRequest request) {
        return leaderboardService.upsertResult(
                request.nickname(),
                request.city(),
                request.won(),
                request.shotsFired(),
                request.hits()
        );
    }

    @GetMapping("/leaderboard/{city}")
    public List<PlayerStat> leaderboard(@PathVariable String city) {
        return leaderboardService.topByCity(city);
    }

    @PostMapping("/coach/review")
    public Map<String, String> review(@RequestBody CoachRequest request) {
        return Map.of("review", aiCoachService.strategicReview(request.log()));
    }

    @PostMapping("/bot/shot")
    public Map<String, Integer> botShot(@RequestBody BotShotRequest request) {
        int[] move = botStrategyService.chooseShot(request.boardView());
        return Map.of("x", move[0], "y", move[1]);
    }

    @PostMapping("/games/create")
    public GameEngine.SessionView createGame(@RequestBody CreateGameRequest request) {
        var session = gameEngine.createSession(request.nickname());
        return session.asView();
    }

    @PostMapping("/games/join")
    public GameEngine.SessionView joinGame(@RequestBody JoinGameRequest request) {
        var session = gameEngine.joinSession(request.inviteCode(), request.nickname());
        return session.asView();
    }

    @GetMapping("/location")
    public Map<String, String> getLocation() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject("https://ipapi.co/json/", Map.class);

        return Map.of("city", response != null ? (String) response.get("city") : "Unknown");
    }

    public record ResultRequest(String nickname, String city, boolean won, int shotsFired, int hits) {}
    public record CoachRequest(String log) {}
    public record BotShotRequest(int[][] boardView) {}
    public record CreateGameRequest(String nickname) {}
    public record JoinGameRequest(String inviteCode, String nickname) {}
}
