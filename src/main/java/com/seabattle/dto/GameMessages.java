package com.seabattle.dto;

import java.util.List;

import com.seabattle.engine.GameEngine.ShipPlacement;

public class GameMessages {
    public record CreateGameRequest(String nickname) {}
    public record JoinGameRequest(String inviteCode, String nickname) {}
    public record PlaceShipsRequest(String inviteCode, String nickname, List<ShipPlacement> ships) {}
    public record ShootRequest(String inviteCode, String nickname, int x, int y) {}
    public record GameEvent(String type, Object payload) {}
}
