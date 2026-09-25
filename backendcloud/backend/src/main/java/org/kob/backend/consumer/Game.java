package org.kob.backend.consumer;

import java.util.Date;
import java.util.Random;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.kob.backend.mapper.RecordMapper;
import org.kob.backend.pojo.Player;
import org.kob.backend.pojo.Record;
import org.kob.backend.pojo.Cell;

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
    private String loser = ""; // "all": 平局, "A" : A 输, "B" : B 输

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RecordMapper recordMapper; // Game 是 new 出来的，不能 @Autowired，由 StartGameServiceImpl 传入

    public Game(Integer rows, Integer cols, Integer inner_walls_count, Player p1, Player p2,
            RecordMapper recordMapper) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
        this.p1 = p1;
        this.p2 = p2;
        this.recordMapper = recordMapper;
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

    private String getStepsString(Player p) { // [2, 0, 1] → "201"
        StringBuilder sb = new StringBuilder();
        for (int d : p.getSteps())
            sb.append(d);
        return sb.toString();
    }

    private String getMapString() { // 按行拼成一串 0/1
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                sb.append(g[i][j]);
        return sb.toString();
    }

    private void saveToDatabase() {
        Record record = new Record(
                null,
                p1.getUser().getId(), p1.getSx(), p1.getSy(),
                p2.getUser().getId(), p2.getSx(), p2.getSy(),
                getStepsString(p1), getStepsString(p2),
                getMapString(), loser, new Date());
        recordMapper.insert(record);
    }

    private void sendResult() {
        try {
            saveToDatabase(); // 先存库，再通知前端
        } catch (Exception e) {
            e.printStackTrace(); // 存库失败也要把结果发给前端
        }
        ObjectNode resp = objectMapper.createObjectNode();
        resp.put("event", "result");
        resp.put("loser", loser);
        lock.lock();
        try {
            resp.put("a_step", nextStepP1); // 最后一步的方向，前端用来转眼睛
            resp.put("b_step", nextStepP2);
        } finally {
            lock.unlock();
        }
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

    private boolean check_valid(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);
        if (g[cell.getX()][cell.getY()] == 1)
            return false; // 撞墙
        for (int i = 0; i < n - 1; i++) {
            if (cellsA.get(i).getX() == cell.getX() && cellsA.get(i).getY() == cell.getY())
                return false; // 撞到自己
            if (cellsB.get(i).getX() == cell.getX() && cellsB.get(i).getY() == cell.getY())
                return false; // 撞到对方
        }
        return true;
    }

    private void judge() { // 判断两个玩家下一步操作是否合法
        List<Cell> cellsA = p1.getCells();
        List<Cell> cellsB = p2.getCells();
        boolean vaildA = check_valid(cellsA, cellsB);
        boolean vaildB = check_valid(cellsB, cellsA);

        if (!vaildA || !vaildB) {
            if (!vaildA && !vaildB) {
                loser = "all";
            } else if (!vaildA) {
                loser = "A";
            } else {
                loser = "B";
            }
            status = "finished";
        }
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
                        loser = "all";
                    } else if (nextStepP1 == null) {
                        loser = "A";
                    } else {
                        loser = "B";
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
