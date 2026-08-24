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

    next_step() { // 计算下一个蛇头的位置
        const d = this.direction;
        this.next_cell = new Cell(this.cells[0].r + this.dr[d], this.cells[0].c + this.dc[d], this.gamemap);
        this.direction = -1; // 蛇头走一步之后，不再响应按键，直到下一步计算
        this.status = "move";
        this.step++;

        const k = this.cells.length;

        for (let i = k; i > 0; i--) {
            this.cells[i] = JSON.parse(JSON.stringify(this.cell[i - 1]));
        }

        this.cells[0] = this.next_cell;
        this.next_cell = null;
    }

    update_move() {
        const dx = this.next_cell.x - this.cells[0].x;
        const dy = this.next_cell.y - this.cells[0].y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < this.eps) {
            this.status = "idle";
            this.cells[0] = this.next_cell;
            this.next_cell = null;
        } else {
            const move_distance = this.speed * this.timedelta / 1000;
            this.cells[0].x += dx * move_distance / distance;
            this.cells[0].y += dy * move_distance / distance;
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
            ctx.arc(cell.x * L, cell.y * L, L / 2, 0, 2 * Math.PI);
            ctx.fill();
        }
    }

}
