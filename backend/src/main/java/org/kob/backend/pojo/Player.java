package org.kob.backend.pojo;

import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.kob.backend.consumer.Game;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private User user;
    private WebSocketSession session;
    private Game game;

    private Integer sx, sy;
    private List<Integer> steps;

    public Player(User user, WebSocketSession session) {
        this.user = user;
        this.session = session;
    }
}
