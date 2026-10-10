// 每种语言创建 Bot 时默认填入的代码模板
// 约定：程序每回合运行一次，从标准输入读一行局面，向标准输出打印方向 0~3
// value 和后端存的语言名一致，mode 是 Ace 编辑器的语法高亮模式
export const LANGUAGES = [
    { value: "cpp", label: "C++", mode: "c_cpp" },
    { value: "java", label: "Java", mode: "java" },
    { value: "python", label: "Python", mode: "python" },
    { value: "go", label: "Go", mode: "golang" },
];

export const DEFAULT_LANGUAGE = "java";

// 用 String.raw，模板里的 \n 等转义原样保留
export const BOT_TEMPLATES = {
    cpp: String.raw`#include <deque>
#include <iostream>
#include <sstream>
#include <string>
#include <utility>
#include <vector>
using namespace std;

/*
 * 每回合运行一次：从标准输入读一行局面，向标准输出打印移动方向：0 上，1 右，2 下，3 左
 *
 * 输入格式：地图#我的起点x#我的起点y#(我的操作序列)#对手起点x#对手起点y#(对手操作序列)
 *   - 地图：15 行 14 列，按行拼成的 0/1 字符串，1 表示墙
 *   - 操作序列：每一步的方向拼起来，例如 (0121)
 */
const int ROWS = 15, COLS = 14;
const int dx[4] = {-1, 0, 1, 0}, dy[4] = {0, 1, 0, -1};

// 和后端规则一致：前 10 步每步都变长，之后每 3 步变长一次
bool checkTailIncreasing(int step) {
    if (step <= 10) return true;
    return step % 3 == 1;
}

// 根据起点和操作序列，算出蛇身占据的所有格子，最后一个是蛇头
deque<pair<int, int>> getCells(int sx, int sy, string steps) {
    steps = steps.substr(1, steps.size() - 2); // 去掉两边的括号
    deque<pair<int, int>> res;
    int x = sx, y = sy;
    res.push_back({x, y});
    for (int i = 0; i < (int)steps.size(); i++) {
        int d = steps[i] - '0';
        x += dx[d], y += dy[d];
        res.push_back({x, y});
        if (!checkTailIncreasing(i + 1)) res.pop_front();
    }
    return res;
}

vector<string> split(const string& s, char sep) {
    vector<string> res;
    string item;
    stringstream ss(s);
    while (getline(ss, item, sep)) res.push_back(item);
    return res;
}

int nextMove(const string& input) {
    vector<string> strs = split(input, '#');
    vector<vector<int>> g(ROWS, vector<int>(COLS, 0));
    for (int i = 0, k = 0; i < ROWS; i++)
        for (int j = 0; j < COLS; j++, k++)
            if (strs[0][k] == '1') g[i][j] = 1;

    auto me = getCells(stoi(strs[1]), stoi(strs[2]), strs[3]);
    auto you = getCells(stoi(strs[4]), stoi(strs[5]), strs[6]);
    for (auto& c : me) g[c.first][c.second] = 1;
    for (auto& c : you) g[c.first][c.second] = 1;

    // 示例策略：选第一个不会撞墙、不会撞蛇的方向，在这里写你自己的策略
    auto head = me.back();
    for (int d = 0; d < 4; d++) {
        int x = head.first + dx[d], y = head.second + dy[d];
        if (x >= 0 && x < ROWS && y >= 0 && y < COLS && g[x][y] == 0) return d;
    }
    return 0;
}

int main() {
    string input;
    getline(cin, input);
    cout << nextMove(input) << endl;
    return 0;
}
`,

    java: String.raw`import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 每回合运行一次：从标准输入读一行局面，向标准输出打印移动方向：0 上，1 右，2 下，3 左
 * 类名必须是 Main
 *
 * 输入格式：地图#我的起点x#我的起点y#(我的操作序列)#对手起点x#对手起点y#(对手操作序列)
 *   - 地图：15 行 14 列，按行拼成的 0/1 字符串，1 表示墙
 *   - 操作序列：每一步的方向拼起来，例如 (0121)
 */
public class Main {
    static final int ROWS = 15, COLS = 14;
    static final int[] dx = {-1, 0, 1, 0};
    static final int[] dy = {0, 1, 0, -1};

    // 和后端规则一致：前 10 步每步都变长，之后每 3 步变长一次
    static boolean checkTailIncreasing(int step) {
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    // 根据起点和操作序列，算出蛇身占据的所有格子，最后一个是蛇头
    static List<int[]> getCells(int sx, int sy, String steps) {
        steps = steps.substring(1, steps.length() - 1); // 去掉两边的括号
        List<int[]> res = new ArrayList<>();
        int x = sx, y = sy;
        res.add(new int[]{x, y});
        for (int i = 0; i < steps.length(); i++) {
            int d = steps.charAt(i) - '0';
            x += dx[d];
            y += dy[d];
            res.add(new int[]{x, y});
            if (!checkTailIncreasing(i + 1)) res.remove(0);
        }
        return res;
    }

    static int nextMove(String input) {
        String[] strs = input.split("#");
        int[][] g = new int[ROWS][COLS];
        for (int i = 0, k = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++, k++)
                if (strs[0].charAt(k) == '1') g[i][j] = 1;

        List<int[]> me = getCells(Integer.parseInt(strs[1]), Integer.parseInt(strs[2]), strs[3]);
        List<int[]> you = getCells(Integer.parseInt(strs[4]), Integer.parseInt(strs[5]), strs[6]);
        for (int[] c : me) g[c[0]][c[1]] = 1;
        for (int[] c : you) g[c[0]][c[1]] = 1;

        // 示例策略：选第一个不会撞墙、不会撞蛇的方向，在这里写你自己的策略
        int[] head = me.get(me.size() - 1);
        for (int d = 0; d < 4; d++) {
            int x = head[0] + dx[d], y = head[1] + dy[d];
            if (x >= 0 && x < ROWS && y >= 0 && y < COLS && g[x][y] == 0) return d;
        }
        return 0;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(nextMove(sc.nextLine()));
    }
}
`,

    python: String.raw`import sys

# 每回合运行一次：从标准输入读一行局面，向标准输出打印移动方向：0 上，1 右，2 下，3 左
#
# 输入格式：地图#我的起点x#我的起点y#(我的操作序列)#对手起点x#对手起点y#(对手操作序列)
#   - 地图：15 行 14 列，按行拼成的 0/1 字符串，1 表示墙
#   - 操作序列：每一步的方向拼起来，例如 (0121)

ROWS, COLS = 15, 14
DX = [-1, 0, 1, 0]
DY = [0, 1, 0, -1]


def check_tail_increasing(step):
    # 和后端规则一致：前 10 步每步都变长，之后每 3 步变长一次
    return step <= 10 or step % 3 == 1


def get_cells(sx, sy, steps):
    # 根据起点和操作序列，算出蛇身占据的所有格子，最后一个是蛇头
    steps = steps[1:-1]  # 去掉两边的括号
    cells = [(sx, sy)]
    x, y = sx, sy
    for i, ch in enumerate(steps, start=1):
        d = int(ch)
        x, y = x + DX[d], y + DY[d]
        cells.append((x, y))
        if not check_tail_increasing(i):
            cells.pop(0)
    return cells


def next_move(line):
    strs = line.split("#")
    g = [[strs[0][i * COLS + j] == "1" for j in range(COLS)] for i in range(ROWS)]

    me = get_cells(int(strs[1]), int(strs[2]), strs[3])
    you = get_cells(int(strs[4]), int(strs[5]), strs[6])
    for x, y in me + you:
        g[x][y] = True

    # 示例策略：选第一个不会撞墙、不会撞蛇的方向，在这里写你自己的策略
    hx, hy = me[-1]
    for d in range(4):
        x, y = hx + DX[d], hy + DY[d]
        if 0 <= x < ROWS and 0 <= y < COLS and not g[x][y]:
            return d
    return 0


print(next_move(sys.stdin.readline().strip()))
`,

    go: String.raw`package main

import (
	"bufio"
	"fmt"
	"os"
	"strconv"
	"strings"
)

// 每回合运行一次：从标准输入读一行局面，向标准输出打印移动方向：0 上，1 右，2 下，3 左
//
// 输入格式：地图#我的起点x#我的起点y#(我的操作序列)#对手起点x#对手起点y#(对手操作序列)
//   - 地图：15 行 14 列，按行拼成的 0/1 字符串，1 表示墙
//   - 操作序列：每一步的方向拼起来，例如 (0121)

const ROWS, COLS = 15, 14

var dx = [4]int{-1, 0, 1, 0}
var dy = [4]int{0, 1, 0, -1}

// 和后端规则一致：前 10 步每步都变长，之后每 3 步变长一次
func checkTailIncreasing(step int) bool {
	return step <= 10 || step%3 == 1
}

// 根据起点和操作序列，算出蛇身占据的所有格子，最后一个是蛇头
func getCells(sx, sy int, steps string) [][2]int {
	steps = steps[1 : len(steps)-1] // 去掉两边的括号
	cells := [][2]int{{sx, sy}}
	x, y := sx, sy
	for i := 0; i < len(steps); i++ {
		d := int(steps[i] - '0')
		x, y = x+dx[d], y+dy[d]
		cells = append(cells, [2]int{x, y})
		if !checkTailIncreasing(i + 1) {
			cells = cells[1:]
		}
	}
	return cells
}

func atoi(s string) int {
	n, _ := strconv.Atoi(s)
	return n
}

func nextMove(input string) int {
	strs := strings.Split(input, "#")
	var g [ROWS][COLS]bool
	for i := 0; i < ROWS; i++ {
		for j := 0; j < COLS; j++ {
			g[i][j] = strs[0][i*COLS+j] == '1'
		}
	}

	me := getCells(atoi(strs[1]), atoi(strs[2]), strs[3])
	you := getCells(atoi(strs[4]), atoi(strs[5]), strs[6])
	for _, c := range me {
		g[c[0]][c[1]] = true
	}
	for _, c := range you {
		g[c[0]][c[1]] = true
	}

	// 示例策略：选第一个不会撞墙、不会撞蛇的方向，在这里写你自己的策略
	head := me[len(me)-1]
	for d := 0; d < 4; d++ {
		x, y := head[0]+dx[d], head[1]+dy[d]
		if x >= 0 && x < ROWS && y >= 0 && y < COLS && !g[x][y] {
			return d
		}
	}
	return 0
}

func main() {
	input, _ := bufio.NewReader(os.Stdin).ReadString('\n')
	fmt.Println(nextMove(strings.TrimSpace(input)))
}
`,
};
