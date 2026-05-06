package com.seabattle.service;

import org.springframework.stereotype.Service;

@Service
public class BotStrategyService {
    public int[] chooseShot(int[][] boardView) {
        int bestX = 0;
        int bestY = 0;
        int bestScore = -1;
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (boardView[x][y] != 0) continue;
                int score = localProbability(boardView, x, y);
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return new int[]{bestX, bestY};
    }

    private int localProbability(int[][] board, int x, int y) {
        int score = ((x + y) % 2 == 0) ? 3 : 1;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx < 0 || ny < 0 || nx >= 10 || ny >= 10) continue;
            if (board[nx][ny] == 2) score += 6;
            if (board[nx][ny] == 1) score -= 2;
        }
        return score;
    }
}
