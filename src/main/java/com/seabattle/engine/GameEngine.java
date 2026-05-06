package com.seabattle.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class GameEngine {
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession createSession(String hostNickname) {
        String inviteCode = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(inviteCode, hostNickname);
        sessions.put(inviteCode, session);
        return session;
    }

    public GameSession getSession(String inviteCode) {
        return sessions.get(inviteCode);
    }

    public GameSession joinSession(String inviteCode, String nickname) {
        GameSession session = sessions.get(inviteCode);
        if (session == null) throw new IllegalArgumentException("Session not found");
        session.join(nickname);
        return session;
    }

    public void placeShips(String inviteCode, String nickname, List<ShipPlacement> placements) {
        GameSession session = requireSession(inviteCode);
        session.placeShips(nickname, placements);
    }

    public ShotOutcome shoot(String inviteCode, String nickname, int x, int y) {
        GameSession session = requireSession(inviteCode);
        return session.shoot(nickname, x, y);
    }

    private GameSession requireSession(String inviteCode) {
        GameSession session = sessions.get(inviteCode);
        if (session == null) throw new IllegalArgumentException("Invalid invite code");
        return session;
    }

    public record ShipPlacement(ShipType type, int x, int y, boolean horizontal) {}
    public enum ShipType {
        CARRIER(5), BATTLESHIP(4), CRUISER(3), SUBMARINE(3), DESTROYER(2);
        private final int length;
        ShipType(int length) { this.length = length; }
        public int length() { return length; }
    }

    public record ShotOutcome(boolean hit, boolean sunk, boolean win, String nextTurn, String message) {}

    public static class GameSession {
        private final String inviteCode;
        private final String host;
        private String guest;
        private final Map<String, Board> boards = new HashMap<>();
        private String turn;
        private boolean started;

        GameSession(String inviteCode, String host) {
            this.inviteCode = inviteCode;
            this.host = host;
            this.turn = host;
            boards.put(host, new Board());
        }

        void join(String nickname) {
            if (guest != null) throw new IllegalStateException("Room already full");
            guest = nickname;
            boards.put(nickname, new Board());
        }

        void placeShips(String nickname, List<ShipPlacement> placements) {
            Board board = requireBoard(nickname);
            board.placeAll(placements);
            if (guest != null && boards.get(host).isPlaced() && boards.get(guest).isPlaced()) {
                started = true;
            }
        }

        ShotOutcome shoot(String nickname, int x, int y) {
            if (!started) throw new IllegalStateException("Game not started");
            if (!turn.equals(nickname)) throw new IllegalStateException("Not your turn");
            String opponent = opponentOf(nickname);
            Board enemyBoard = requireBoard(opponent);
            ShotResult result = enemyBoard.fire(x, y);
            if (!result.hit()) {
                turn = opponent;
            }
            boolean win = enemyBoard.allShipsSunk();
            return new ShotOutcome(result.hit(), result.sunk(), win, turn, result.message());
        }

        private String opponentOf(String nickname) {
            if (nickname.equals(host)) return guest;
            if (nickname.equals(guest)) return host;
            throw new IllegalArgumentException("Unknown player");
        }

        private Board requireBoard(String nickname) {
            Board board = boards.get(nickname);
            if (board == null) throw new IllegalArgumentException("Player not in game");
            return board;
        }

        public String inviteCode() { return inviteCode; }
        public String host() { return host; }
        public String guest() { return guest; }
        public String turn() { return turn; }
        public boolean started() { return started; }
    }

    record ShotResult(boolean hit, boolean sunk, String message) {}

    static class Board {
        private final int size = 10;
        private final Cell[][] cells = new Cell[size][size];
        private final Map<ShipType, Set<String>> shipCoordinates = new EnumMap<>(ShipType.class);
        private final Map<ShipType, Integer> hitsByShip = new EnumMap<>(ShipType.class);
        private boolean placed;

        Board() {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    cells[i][j] = new Cell();
                }
            }
        }

        void placeAll(List<ShipPlacement> placements) {
            if (placements.size() != ShipType.values().length) {
                throw new IllegalArgumentException("All ships must be placed exactly once");
            }
            List<String> taken = new ArrayList<>();
            for (ShipPlacement placement : placements) {
                if (shipCoordinates.containsKey(placement.type())) {
                    throw new IllegalArgumentException("Duplicate ship placement: " + placement.type());
                }
                Set<String> coords = ConcurrentHashMap.newKeySet();
                for (int i = 0; i < placement.type().length(); i++) {
                    int x = placement.horizontal() ? placement.x() : placement.x() + i;
                    int y = placement.horizontal() ? placement.y() + i : placement.y();
                    if (x < 0 || y < 0 || x >= size || y >= size) {
                        throw new IllegalArgumentException("Ship out of bounds");
                    }
                    String key = x + ":" + y;
                    if (taken.contains(key)) {
                        throw new IllegalArgumentException("Ships cannot overlap");
                    }
                    taken.add(key);
                    coords.add(key);
                    cells[x][y].ship = placement.type();
                }
                shipCoordinates.put(placement.type(), coords);
                hitsByShip.put(placement.type(), 0);
            }
            placed = true;
        }

        ShotResult fire(int x, int y) {
            if (x < 0 || y < 0 || x >= size || y >= size) throw new IllegalArgumentException("Invalid coordinate");
            Cell cell = cells[x][y];
            if (cell.fired) throw new IllegalArgumentException("Cell already targeted");
            cell.fired = true;
            if (cell.ship == null) return new ShotResult(false, false, "Miss");
            ShipType ship = cell.ship;
            int hitCount = hitsByShip.merge(ship, 1, Integer::sum);
            boolean sunk = hitCount == ship.length();
            return new ShotResult(true, sunk, sunk ? ship.name() + " sunk!" : "Hit");
        }

        boolean allShipsSunk() {
            return shipCoordinates.keySet().stream().allMatch(type -> hitsByShip.getOrDefault(type, 0) == type.length());
        }

        boolean isPlaced() { return placed; }
    }

    static class Cell {
        private ShipType ship;
        private boolean fired;
    }
}
