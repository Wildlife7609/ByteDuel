import AcGameObject from "./ACGameObject.js";
import { Wall } from "./Wall.js";

export class GameMap extends AcGameObject {
    constructor(ctx, parent) {
        super();
        this.ctx = ctx;
        this.parent = parent;
        this.L = 0; //绝对像素

        this.rows = 15;
        this.cols = 15;

        this.inner_walls_count = 20;
        this.walls = [];
    }

    check_connected(g, x1, y1, x2, y2) {
        if (x1 == x2 && y1 == y2) return true;
        g[x1][y1] = true;
        let offset = [0, 1, 0, -1, 0];
        for (let i = 0; i < 4; i++) {
            let nx = x1 + offset[i];
            let ny = y1 + offset[i + 1];
            if (nx >= 0 && nx < this.rows && ny >= 0 && ny < this.cols && !g[nx][ny]) {
                if (this.check_connected(g, nx, ny, x2, y2)) return true;
            }
        }
        return false;
    }

    create_walls() {
        const g = [];
        for (let r = 0; r < this.rows; r++) {
            g[r] = [];
            for (let c = 0; c < this.cols; c++) {
                g[r][c] = false;
            }
        }

        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (r === 0 || r === this.rows - 1 || c === 0 || c === this.cols - 1) {
                    g[r][c] = true;
                }
            }
        }

        // 创建随机障碍物（中心对称）
        for (let i = 0; i < this.inner_walls_count / 2; i++) {
            for (let j = 0; j < 1000; j++) { // 尝试最多 1000 次避免极端死循环
                let r = parseInt(Math.random() * this.rows);
                let c = parseInt(Math.random() * this.cols);

                // 1. 如果自身或对称点已经有墙，跳过重新随机
                if (g[r][c] || g[this.rows - 1 - r][this.cols - 1 - c]) continue;

                // 2. 排除左下角和右上角两条蛇的出生点
                if ((r === this.rows - 2 && c === 1) || (r === 1 && c === this.cols - 2)) continue;

                // 3. 找到合法空位，成对放置障碍物并退出当前循环
                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = true;
                break;
            }
        }

        const copy_g = JSON.parse(JSON.stringify(g));
        if (!this.check_connected(copy_g, this.rows - 2, 1, 1, this.cols - 2)) {
            return false;
        }


        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }
        return true;
    }

    start() {
        this.set_map_size();
        for (let i = 0; i < 10; i++) {
            if (this.create_walls()) break;
        }
    }
    set_map_size() {
    }
    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }
    update() {
        this.update_size();
        this.render()
    }
    render() {
        const color_even = "#AAD751"; // 浅草绿
        const color_odd = "#A2D149"; // 深草绿


        for (let i = 0; i < this.rows; i++) {
            for (let j = 0; j < this.cols; j++) {
                if ((i + j) % 2 === 0) {
                    this.ctx.fillStyle = color_even;
                } else {
                    this.ctx.fillStyle = color_odd;
                }
                this.ctx.fillRect(j * this.L, i * this.L, this.L, this.L);
            }
        }
    }
}