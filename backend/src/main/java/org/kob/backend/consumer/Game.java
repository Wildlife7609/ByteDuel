package org.kob.backend.consumer;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.kob.backend.pojo.Player;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class Game extends Thread {
    private final Integer rows;
    private final Integer cols;
    private final Integer inner_walls_count;
    private final int[][] g;
    private final static int[] offset = { 0, 1, 0, -1, 0 };

    private final Player p1, p2;
    private Integer nextStepP1 = null;
    private Integer nextStepP2 = null;
    private final ReentrantLock lock = new ReentrantLock();

    private String status = "playing"; // "playing" : 游戏进行中, "finished" : 游戏结束
    private String winner = ""; // "all": 平局, "A" : A 赢, "B" : B 赢

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Game(Integer rows, Integer cols, Integer inner_walls_count, Player p1, Player p2) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
        this.p1 = p1;
        this.p2 = p2;
        createMap();
    }

    public Integer getRows() {
        return rows;
    }

    public Integer getCols() {
        return cols;
    }

    public Player getP1() {
        return p1;
    }

    public Player getP2() {
        return p2;
    }

    public void setNextStepP1(Integer nextStepP1) {
        lock.lock();
        try {
            this.nextStepP1 = nextStepP1;
        } finally {
            lock.unlock();
        }
    }

    public void setNextStepP2(Integer nextStepP2) {
        lock.lock();
        try {
            this.nextStepP2 = nextStepP2;
        } finally {
            lock.unlock();
        }
    }

    private void sendResult() {
        ObjectNode resp = objectMapper.createObjectNode();
        resp.put("event", "result");
        resp.put("winner", winner);
        sendMessageToAll(resp.toString());

    }

    private void sendMove() { // 发送玩家下一步操作
        lock.lock();
        try {
            ObjectNode resp = objectMapper.createObjectNode();
            resp.put("event", "move");
            resp.put("a_step", nextStepP1);
            resp.put("b_step", nextStepP2);
            sendMessageToAll(resp.toString());
            nextStepP1 = nextStepP2 = null;
        } finally {
            lock.unlock();
        }
    }

    private void sendMessageToAll(String message) { // 发送地图信息和玩家操作信息给所有玩家
        p1.sendMessage(message);
        p2.sendMessage(message);
    }

    private void judge() { // 判断两个玩家下一步操作是否合法

    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            if (nextStep()) {
                judge();
                if (status.equals("playing")) {
                    sendMove();
                } else {
                    sendResult();
                    break;
                }
            } else {
                status = "finished";
                lock.lock();
                try {
                    if (nextStepP1 == null && nextStepP2 == null) {
                        winner = "all";
                    } else if (nextStepP1 == null) {
                        winner = "B";
                    } else {
                        winner = "A";
                    }
                } finally {
                    lock.unlock();
                }
                sendResult();
                break;
            }

        }

    }

    private boolean nextStep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 25; i++) {
            try {
                Thread.sleep(200);
                lock.lock();
                try {
                    if (nextStepP1 != null && nextStepP2 != null) {
                        p1.getSteps().add(nextStepP1);
                        p2.getSteps().add(nextStepP2);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public int[][] getG() {
        return g;
    }

    private boolean check_connectivity(int sx, int sy, int tx, int ty) {
        if (sx == tx && sy == ty)
            return true;
        g[sx][sy] = 1;
        for (int i = 0; i < 4; i++) {
            int nx = sx + offset[i];
            int ny = sy + offset[i + 1];
            if (nx >= 0 && nx < this.rows && ny >= 0 && ny < this.cols && g[nx][ny] == 0) {
                if (check_connectivity(nx, ny, tx, ty)) {
                    g[sx][sy] = 0;
                    return true;
                }
            }
        }
        g[sx][sy] = 0;
        return false;
    }

    private boolean draw() {
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                g[i][j] = 0;
            }
        }

        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                if (r == 0 || r == this.rows - 1 || c == 0 || c == this.cols - 1) {
                    g[r][c] = 1;
                }
            }
        }

        Random random = new Random();
        for (int i = 0; i < this.inner_walls_count / 2; i++) {
            for (int j = 0; j < 1000; j++) {
                int r = random.nextInt(this.rows);
                int c = random.nextInt(this.cols);

                if (g[r][c] == 1 || g[this.rows - 1 - r][this.cols - 1 - c] == 1)
                    continue;
                if ((r == this.rows - 2 && c == 1) || (r == 1 && c == this.cols - 2))
                    continue;

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = 1;
                break;
            }
        }

        return check_connectivity(this.rows - 2, 1, 1, this.cols - 2);
    }

    public void createMap() {
        for (int i = 0; i < 1000; i++) {
            if (draw()) {
                break;
            }
        }
    }

}
