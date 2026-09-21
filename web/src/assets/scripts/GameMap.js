import AcGameObject from "./ACGameObject.js";
import { Wall } from "./Wall.js";
import { Snake } from "./Snake.js";

export class GameMap extends AcGameObject {
    constructor(ctx, parent, gameObject) {
        super();
        this.ctx = ctx;
        this.parent = parent;
        this.gameObject = gameObject;
        this.L = 0; //绝对像素

        this.rows = 15;
        this.cols = 14;

        this.inner_walls_count = 20;
        this.walls = [];

        this.snakes = [
            new Snake({ id: 0, color: "#EC4899", r: gameObject.a_sx, c: gameObject.a_sy }, this),
            new Snake({ id: 1, color: "#06B6D4", r: gameObject.b_sx, c: gameObject.b_sy }, this),
        ]
    }

    create_walls() {
        const g = this.gameObject?.map;
        if (!g) return;

        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }
    }

    add_listening_event() {
        this.ctx.canvas.focus();

        const [snake0, snake1] = this.snakes;

        this.ctx.canvas.addEventListener("keydown", e => {
            if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", " "].includes(e.key)) {
                e.preventDefault();
            }
            if (e.key === "w") {
                snake0.set_direction(0);
            } else if (e.key === "d") {
                snake0.set_direction(1);
            } else if (e.key === "s") {
                snake0.set_direction(2);
            } else if (e.key === "a") {
                snake0.set_direction(3);
            }
            else if (e.key === "ArrowUp") {
                snake1.set_direction(0);
            } else if (e.key === "ArrowRight") {
                snake1.set_direction(1);
            } else if (e.key === "ArrowDown") {
                snake1.set_direction(2);
            } else if (e.key === "ArrowLeft") {
                snake1.set_direction(3);
            }
        });
    }

    start() {
        this.create_walls();
        this.add_listening_event();
    }

    on_destroy() {
        for (let wall of this.walls) {
            wall.destroy();
        }
        for (let snake of this.snakes) {
            snake.destroy();
        }
    }

    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    check_valid(cell) { // 检查蛇是否撞墙或撞到自己或撞到对方
        for (const wall of this.walls) {
            if (wall.r === cell.r && wall.c === cell.c) return false;
        }
        for (const snake of this.snakes) {
            let k = snake.cells.length;
            if (!snake.check_tail_increasing()) { // 蛇尾巴不增长的话，就不用检查蛇尾巴了
                k--;
            }
            for (let i = 0; i < k; i++) {
                if (snake.cells[i].r === cell.r && snake.cells[i].c === cell.c) return false;
            }
        }
        return true;

    }

    next_step() {
        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    check_ready() { // 检查是否已经准备好开始游戏（所有蛇都已静止且方向已确定）
        for (const snake of this.snakes) {
            if (snake.status !== "idle") return false;
            if (snake.direction === -1) return false;
        }
        return true;
    }

    update() {
        this.update_size();
        if (this.check_ready()) {
            this.next_step()
        }
        this.render()
    }

    render() {
        const color_even = "#AAD751"; // 浅草绿
        const color_odd = "#A2D149";  // 深草绿

        for (let i = 0; i < this.rows; i++) {
            for (let j = 0; j < this.cols; j++) {
                // 1. 绘制经典棋盘格底色
                this.ctx.fillStyle = (i + j) % 2 === 0 ? color_even : color_odd;
                this.ctx.fillRect(j * this.L, i * this.L, this.L, this.L);

                // 2. 随机小草点缀（利用固定坐标哈希，保证位置永远稳定）
                if ((i * 7 + j * 13) % 9 === 0) {
                    const cx = (j + 0.5) * this.L;
                    const cy = (i + 0.5) * this.L;

                    // 画两根可爱的小嫩草苗
                    this.ctx.strokeStyle = "rgba(74, 120, 30, 0.4)";
                    this.ctx.lineWidth = this.L * 0.04;
                    this.ctx.lineCap = "round";

                    this.ctx.beginPath();
                    this.ctx.moveTo(cx - this.L * 0.08, cy + this.L * 0.08);
                    this.ctx.lineTo(cx - this.L * 0.12, cy - this.L * 0.08);

                    this.ctx.moveTo(cx, cy + this.L * 0.1);
                    this.ctx.lineTo(cx + this.L * 0.08, cy - this.L * 0.1);
                    this.ctx.stroke();
                }
            }
        }
    }
}