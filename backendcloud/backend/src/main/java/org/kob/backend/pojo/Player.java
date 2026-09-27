package org.kob.backend.pojo;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.kob.backend.consumer.Game;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private User user;
    private WebSocketSession session;
    private Bot bot;
    private Game game;

    private Integer sx, sy;
    private List<Integer> steps;

    public Player(User user, WebSocketSession session) {
        this.user = user;
        this.session = session;
    }

    public void sendMessage(String message) {
        try {
            if (session != null && session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean check_tail_increasing(int step) { // 检查当前回合，蛇的长度是否增加
        if (step <= 10)
            return true;
        return step % 3 == 1;
    }

    public List<Cell> getCells() {
        List<Cell> res = new ArrayList<>();
        int[] dx = { -1, 0, 1, 0 }; // 上右下左
        int[] dy = { 0, 1, 0, -1 };
        int x = sx, y = sy;
        int step = 0;
        res.add(new Cell(x, y));
        for (int d : steps) {
            x += dx[d];
            y += dy[d];
            res.add(new Cell(x, y));
            step++;
            if (!check_tail_increasing(step)) {
                res.remove(0);
            }
        }
        return res;
    }
}
