package com.seabattle.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.seabattle.dto.GameMessages.CreateGameRequest;
import com.seabattle.dto.GameMessages.GameEvent;
import com.seabattle.dto.GameMessages.JoinGameRequest;
import com.seabattle.dto.GameMessages.PlaceShipsRequest;
import com.seabattle.dto.GameMessages.ShootRequest;
import com.seabattle.engine.GameEngine;

@Controller
public class GameWebSocketController {
    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(GameEngine gameEngine, SimpMessagingTemplate messagingTemplate) {
        this.gameEngine = gameEngine;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game.create")
    public void create(CreateGameRequest request) {
        var session = gameEngine.createSession(request.nickname());
        messagingTemplate.convertAndSend("/topic/game/" + session.inviteCode(),
                new GameEvent("GAME_CREATED", session));
    }

    @MessageMapping("/game.join")
    public void join(JoinGameRequest request) {
        var session = gameEngine.joinSession(request.inviteCode(), request.nickname());
        messagingTemplate.convertAndSend("/topic/game/" + request.inviteCode(),
                new GameEvent("PLAYER_JOINED", session));
    }

    @MessageMapping("/game.place")
    public void place(PlaceShipsRequest request) {
        gameEngine.placeShips(request.inviteCode(), request.nickname(), request.ships());
        var session = gameEngine.getSession(request.inviteCode());
        messagingTemplate.convertAndSend("/topic/game/" + request.inviteCode(),
                new GameEvent("SHIPS_PLACED", session));
    }

    @MessageMapping("/game.shoot")
    public void shoot(ShootRequest request) {
        var outcome = gameEngine.shoot(request.inviteCode(), request.nickname(), request.x(), request.y());
        messagingTemplate.convertAndSend("/topic/game/" + request.inviteCode(),
                new GameEvent("SHOT_RESULT", outcome));
    }
}
