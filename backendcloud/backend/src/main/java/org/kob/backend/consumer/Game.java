package org.kob.backend.consumer;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.kob.backend.mapper.RecordMapper;
import org.kob.backend.mapper.UserMapper;

import org.kob.backend.pojo.Player;
import org.kob.backend.pojo.Record;
import org.kob.backend.pojo.Cell;
import org.kob.backend.pojo.User;

import org.springframework.web.client.RestClient;
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

    private volatile String status = "playing"; // "playing" or "finished"; also read by WebSocket threads
    private String loser = ""; // "all": draw, "A": A lost, "B": B lost

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RecordMapper recordMapper; // Game is created with new, not by Spring, so these cannot be @Autowired; StartGameServiceImpl passes them in
    private final RestClient botRunningClient; 
    private final UserMapper userMapper; // 

    public Game(Integer rows, Integer cols, Integer inner_walls_count, Player p1, Player p2,
            RecordMapper recordMapper, RestClient botRunningClient, UserMapper userMapper) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
        this.p1 = p1;
        this.p2 = p2;
        this.recordMapper = recordMapper;
        this.userMapper = userMapper;
        this.botRunningClient = botRunningClient;
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

    public boolean isPlaying() {
        return status.equals("playing");
    }

    public void setNextStepP1(Integer nextStepP1) {
        if (nextStepP1 < 0 || nextStepP1 > 3) return;
        lock.lock();
        try {
            this.nextStepP1 = nextStepP1;
        } finally {
            lock.unlock();
        }
    }

    public void setNextStepP2(Integer nextStepP2) {
        if (nextStepP2 < 0 || nextStepP2 > 3) return;
        lock.lock();
        try {
            this.nextStepP2 = nextStepP2;
        } finally {
            lock.unlock();
        }
    }

    private String getInput(Player player) { // encode the current game state as a string
        Player me = player, you = (player == p1) ? p2 : p1;
    return getMapString() + "#"
            + me.getSx() + "#" + me.getSy() + "#(" + getStepsString(me) + ")#"
            + you.getSx() + "#" + you.getSy() + "#(" + getStepsString(you) + ")";
    }

    private boolean hasBotCode(Player player) {
        return player.getBot() != null && player.getBot().getContent() != null
                && !player.getBot().getContent().isEmpty();
    }

    private void sendBotCode(Player player) {
        if (!hasBotCode(player)) return;
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", player.getUser().getId());
        data.put("bot_language", player.getBot().getLanguage());
        data.put("bot_code", player.getBot().getContent());
        data.put("input", getInput(player));
        try {
            botRunningClient.post().uri("/bot/add/").body(data).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Have botrunningsystem compile both bots before the game starts. The compile cache is lost when the service restarts,
    // and compiling Go takes several seconds, which would blow the 5-second limit if done during the first turn
    private void prepareBots() {
        Thread t1 = Thread.ofVirtual().start(() -> prepareBot(p1)); // compile both bots in parallel
        Thread t2 = Thread.ofVirtual().start(() -> prepareBot(p2));
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void prepareBot(Player player) {
        if (!hasBotCode(player)) return;
        Map<String, String> data = new HashMap<>();
        data.put("bot_language", player.getBot().getLanguage());
        data.put("bot_code", player.getBot().getContent());
        try {
            botRunningClient.post().uri("/bot/compile/").body(data).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace(); // a failed warm-up does not block the game; worst case it compiles on the first turn
        }
    }

    private String getStepsString(Player p) { // [2, 0, 1] → "201"
        StringBuilder sb = new StringBuilder();
        for (int d : p.getSteps())
            sb.append(d);
        return sb.toString();
    }

    private String getMapString() { // the board as a row-by-row string of 0s and 1s
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                sb.append(g[i][j]);
        return sb.toString();
    }

    private void updateUserRating(Player player, Integer rating) {
        player.getUser().setRating(rating);
        User user = new User();
        user.setId(player.getUser().getId());
        user.setRating(rating);
        userMapper.updateById(user);
    }

    // Increment in the database instead of relying on the user data loaded at game start; column is always a fixed name passed by this class
    private void incrementResultCount(Player player, String column) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", player.getUser().getId())
                .setSql(column + " = " + column + " + 1");
        userMapper.update(wrapper);
    }

    private void saveToDatabase() {
        Integer ratingA = p1.getUser().getRating();
        Integer ratingB = p2.getUser().getRating();
        if (!loser.equals("all")) {
            if (loser.equals("A")) {
                ratingA -= 2;
                ratingB += 5;
            } else {
                ratingA += 5;
                ratingB -= 2;
            }
            updateUserRating(p1, ratingA);
            updateUserRating(p2, ratingB);
        }

        if (loser.equals("all")) {
            incrementResultCount(p1, "draw_count");
            incrementResultCount(p2, "draw_count");
        } else if (loser.equals("A")) {
            incrementResultCount(p1, "lose_count");
            incrementResultCount(p2, "win_count");
        } else {
            incrementResultCount(p1, "win_count");
            incrementResultCount(p2, "lose_count");
        }

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
            saveToDatabase(); // save first, then notify the clients
        } catch (Exception e) {
            e.printStackTrace(); // send the result to the clients even if saving fails
        }
        ObjectNode resp = objectMapper.createObjectNode();
        resp.put("event", "result");
        resp.put("loser", loser);
        lock.lock();
        try {
            resp.put("a_step", nextStepP1); // direction of the last move; the frontend uses it to turn the eyes
            resp.put("b_step", nextStepP2);
        } finally {
            lock.unlock();
        }
        sendMessageToAll(resp.toString());

    }

    private void sendMove() { // send both players' next moves
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

    private void sendMessageToAll(String message) { // send a message to both players
        // Skip a player who has already started a new game, so the frontend does not mix up the two games
        if (p1.getGame() == this)
            p1.sendMessage(message);
        if (p2.getGame() == this)
            p2.sendMessage(message);
    }

    private boolean check_valid(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);
        if (g[cell.getX()][cell.getY()] == 1)
            return false; // hit a wall
        for (int i = 0; i < n - 1; i++) {
            if (cellsA.get(i).getX() == cell.getX() && cellsA.get(i).getY() == cell.getY())
                return false; // hit itself
            if (cellsB.get(i).getX() == cell.getX() && cellsB.get(i).getY() == cell.getY())
                return false; // hit the other snake
        }
        return true;
    }

    private void judge() { // check whether both players' next moves are valid
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
        prepareBots();
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
        sendBotCode(p1);
        sendBotCode(p2);
        
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

    // BFS visits each cell at most once. A backtracking DFS (unmarking cells on the way back) would enumerate every path:
    // on a poorly connected map it can run for seconds or longer, stalling game start, so players re-queue and end up in two games at once
    private boolean check_connectivity(int sx, int sy, int tx, int ty) {
        boolean[][] visited = new boolean[rows][cols];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        visited[sx][sy] = true;
        queue.add(new int[]{sx, sy});
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            if (cell[0] == tx && cell[1] == ty)
                return true;
            for (int i = 0; i < 4; i++) {
                int nx = cell[0] + offset[i];
                int ny = cell[1] + offset[i + 1];
                if (nx >= 0 && nx < rows && ny >= 0 && ny < cols && g[nx][ny] == 0 && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
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
