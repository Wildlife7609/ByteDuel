// 创建 Bot 时编辑器里默认填入的代码模板
// 约定见 botrunningsystem 的 BotCompiler：public 类 Bot，实现 Function<String, Integer>，有 public 无参构造函数
export const BOT_TEMPLATE = `package com.kob.botrunningsystem.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 每回合调用一次 apply，返回本回合的移动方向：0 上，1 右，2 下，3 左
 *
 * input 格式：地图#我的起点x#我的起点y#(我的操作序列)#对手起点x#对手起点y#(对手操作序列)
 *   - 地图：15 行 14 列，按行拼成的 0/1 字符串，1 表示墙
 *   - 操作序列：每一步的方向拼起来，例如 (0121)
 */
public class Bot implements Function<String, Integer> {
    private static final int ROWS = 15, COLS = 14;
    private static final int[] dx = {-1, 0, 1, 0};
    private static final int[] dy = {0, 1, 0, -1};

    // 和后端规则一致：前 10 步每步都变长，之后每 3 步变长一次
    private boolean checkTailIncreasing(int step) {
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    // 根据起点和操作序列，算出蛇身占据的所有格子，最后一个是蛇头
    private List<int[]> getCells(int sx, int sy, String steps) {
        steps = steps.substring(1, steps.length() - 1); // 去掉两边的括号
        List<int[]> res = new ArrayList<>();
        int x = sx, y = sy, step = 0;
        res.add(new int[]{x, y});
        for (int i = 0; i < steps.length(); i++) {
            int d = steps.charAt(i) - '0';
            x += dx[d];
            y += dy[d];
            res.add(new int[]{x, y});
            if (!checkTailIncreasing(++step)) res.remove(0);
        }
        return res;
    }

    @Override
    public Integer apply(String input) {
        String[] strs = input.split("#");
        int[][] g = new int[ROWS][COLS];
        for (int i = 0, k = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++, k++)
                if (strs[0].charAt(k) == '1') g[i][j] = 1;

        List<int[]> meCells = getCells(Integer.parseInt(strs[1]), Integer.parseInt(strs[2]), strs[3]);
        List<int[]> youCells = getCells(Integer.parseInt(strs[4]), Integer.parseInt(strs[5]), strs[6]);
        for (int[] c : meCells) g[c[0]][c[1]] = 1;
        for (int[] c : youCells) g[c[0]][c[1]] = 1;

        // 示例策略：选第一个不会撞墙、不会撞蛇的方向，在这里写你自己的策略
        int[] head = meCells.get(meCells.size() - 1);
        for (int d = 0; d < 4; d++) {
            int x = head[0] + dx[d], y = head[1] + dy[d];
            if (x >= 0 && x < ROWS && y >= 0 && y < COLS && g[x][y] == 0) return d;
        }
        return 0;
    }
}
`;
