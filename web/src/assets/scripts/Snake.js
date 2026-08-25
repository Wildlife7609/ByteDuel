import AcGameObject from "./ACGameObject.js";
import { Cell } from "./Cell.js";

export class Snake extends AcGameObject {
    constructor(info, gamemap) {
        super();
        this.id = info.id;
        this.color = info.color
        this.gamemap = gamemap;

        this.cells = [new Cell(info.r, info.c)]; // 蛇的身体
        this.next_cell = null; // 下一步要添加的位置
        this.speed = 5;
        this.direction = -1; // -1:未按键, 0:上, 1:右, 2:下, 3:左
        this.status = "idle"; // "idle" : 静止, "move" : 移动, "die" : 死亡

        this.dr = [-1, 0, 1, 0]; // row 变化量，对应 0, 1, 2, 3
        this.dc = [0, 1, 0, -1]; // col 变化量，对应 0, 1, 2, 3

        this.step = 0; // 步数
        this.eps = 1e-2; // 精度误差

    }

    start() {
    }

    set_direction(d) {
        this.direction = d;
    }

    check_tail_increasing() { // 检查蛇尾是否增长
        if (this.step <= 10) return true; // 蛇头每走一步，身体向后移一格，
        if (this.step % 3 === 1) return true; // 蛇每走3步，蛇尾增长一格
        return false;
    }

    next_step() { // 计算下一个蛇头的位置
        const d = this.direction;
        this.next_cell = new Cell(this.cells[0].r + this.dr[d], this.cells[0].c + this.dc[d]);
        this.direction = -1; // 蛇头走一步之后，不再响应按键，直到下一步计算
        this.status = "move";
        this.step++;

        const k = this.cells.length;

        for (let i = k; i > 0; i--) {
            this.cells[i] = JSON.parse(JSON.stringify(this.cells[i - 1]));
        }
    }

    update_move() {
        const dx = this.next_cell.x - this.cells[0].x;
        const dy = this.next_cell.y - this.cells[0].y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < this.eps) { // 走到目标位置了
            this.status = "idle";
            this.cells[0] = this.next_cell; // 添加新的蛇头
            this.next_cell = null;

            if (!this.check_tail_increasing()) { // 如果蛇尾不增长，就删掉蛇尾
                this.cells.pop();
            }
        } else { // 没走到，继续走
            const move_distance = this.speed * this.timedelta / 1000;
            this.cells[0].x += dx * move_distance / distance;
            this.cells[0].y += dy * move_distance / distance;

            if (!this.check_tail_increasing()) {
                const k = this.cells.length;
                const tail = this.cells[k - 1], tail_target = this.cells[k - 2];
                const tail_dx = tail_target.x - tail.x;
                const tail_dy = tail_target.y - tail.y;
                tail.x += move_distance * tail_dx / distance;
                tail.y += move_distance * tail_dy / distance;
            }
        }
    }

    update() { // 每一帧都会调用
        if (this.status === "move") {
            this.update_move();
        }
        this.render();
    }

    render() {
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;

        ctx.fillStyle = this.color;
        for (const cell of this.cells) {
            ctx.beginPath();
            ctx.arc(cell.x * L, cell.y * L, L / 2 * 0.8, 0, 2 * Math.PI);
            ctx.fill();
        }

        for (let i = 1; i < this.cells.length; i++) {
            const a = this.cells[i - 1], b = this.cells[i];
            if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps) continue;
            if (Math.abs(a.x - b.x) < this.eps) {
                ctx.fillRect(a.x * L - 0.4 * L, Math.min(a.y, b.y) * L, 0.8 * L, Math.abs(a.y - b.y) * L);
            }
            else {
                ctx.fillRect(Math.min(a.x, b.x) * L, a.y * L - 0.4 * L, Math.abs(a.x - b.x) * L, 0.8 * L);
            }
        }
    }

}
