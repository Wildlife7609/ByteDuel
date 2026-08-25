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

        this.eye_direction = 0; // 默认朝上
        if (this.id === 1) {
            this.eye_direction = 2; // 右上角蛇，眼睛朝下
        }
        // 眼睛相对蛇头的偏移量, 0.15
        this.eye_dx = [
            [-1, 1], // 眼睛相对蛇头的偏移量
            [1, 1],
            [1, -1],
            [-1, -1]
        ];
        this.eye_dy = [
            [-1, -1],
            [-1, 1],
            [1, 1],
            [1, -1]
        ];
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
        this.eye_direction = d; // 蛇头走一步之后，眼睛朝向变成当前方向
        this.next_cell = new Cell(this.cells[0].r + this.dr[d], this.cells[0].c + this.dc[d]);
        this.direction = -1; // 蛇头走一步之后，不再响应按键，直到下一步计算
        this.status = "move";
        this.step++;

        const k = this.cells.length;

        for (let i = k; i > 0; i--) {
            this.cells[i] = JSON.parse(JSON.stringify(this.cells[i - 1]));
        }

        if (!this.gamemap.check_valid(this.next_cell)) { // next_cell 位置无效，蛇死亡
            this.status = "die";
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

        const r = L * 0.38; // 蛇身半径 
        const d = r * 2;   // 蛇身厚度 

        // 折纸蛇的感觉
        ctx.save();
        ctx.shadowColor = "rgba(0, 0, 0, 0.25)";
        ctx.shadowBlur = 6;
        ctx.shadowOffsetY = 3;

        ctx.fillStyle = this.color;
        if (this.status === "die") {
            ctx.fillStyle = "white";
        }

        for (const cell of this.cells) {
            ctx.beginPath();
            ctx.arc(cell.x * L, cell.y * L, d / 2, 0, 2 * Math.PI);
            ctx.fill();
        }

        for (let i = 1; i < this.cells.length; i++) {
            const a = this.cells[i - 1], b = this.cells[i];
            if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps) continue;
            if (Math.abs(a.x - b.x) < this.eps) {
                ctx.fillRect(a.x * L - r, Math.min(a.y, b.y) * L, d, Math.abs(a.y - b.y) * L);
            }
            else {
                ctx.fillRect(Math.min(a.x, b.x) * L, a.y * L - r, Math.abs(a.x - b.x) * L, d);
            }
        }

        // 恢复原始状态（移除阴影）
        ctx.restore();

        // 绘制蛇眼
        if (this.status === "die") {
            // 绘制死亡晕厥 X X 眼
            ctx.strokeStyle = "#475569";
            ctx.lineWidth = L * 0.025;
            ctx.lineCap = "round";
            for (let i = 0; i < 2; i++) {
                const eye_x = (this.cells[0].x + this.eye_dx[this.eye_direction][i] * 0.12) * L;
                const eye_y = (this.cells[0].y + this.eye_dy[this.eye_direction][i] * 0.12) * L;
                const sz = L * 0.04;
                ctx.beginPath();
                ctx.moveTo(eye_x - sz, eye_y - sz);
                ctx.lineTo(eye_x + sz, eye_y + sz);
                ctx.moveTo(eye_x + sz, eye_y - sz);
                ctx.lineTo(eye_x - sz, eye_y + sz);
                ctx.stroke();
            }
        } else {
            // 正常状态：绘制白眼仁 + 黑色瞳孔 + 反光
            for (let i = 0; i < 2; i++) {
                const eye_x = (this.cells[0].x + this.eye_dx[this.eye_direction][i] * 0.12) * L;
                const eye_y = (this.cells[0].y + this.eye_dy[this.eye_direction][i] * 0.12) * L;
                // 黑眼珠
                ctx.fillStyle = "#1E293B";
                ctx.beginPath();
                ctx.arc(eye_x, eye_y, L * 0.065, 0, 2 * Math.PI);
                ctx.fill();
                // 眼睛反光高光小白点
                ctx.fillStyle = "white";
                ctx.beginPath();
                ctx.arc(eye_x - L * 0.02, eye_y - L * 0.02, L * 0.025, 0, 2 * Math.PI);
                ctx.fill();
            }
        }
    }
    // render() {
    //     const L = this.gamemap.L;
    //     const ctx = this.gamemap.ctx;

    //     const r = L * 0.36; // 彩色蛇身半径 
    //     const d = r * 2;   // 彩色蛇身厚度 
    //     const border = L * 0.045; // 🌟 纸片马力欧标志性：白色贴纸切边宽度

    //     // 🌟 1. 纸片马力欧专属：薄卡纸硬阴影（清晰干净的纸片投影）
    //     ctx.save();
    //     ctx.shadowColor = "rgba(0, 0, 0, 0.22)";
    //     ctx.shadowBlur = 0; // 👈 纸片不需要模糊，硬投影才有纸板感！
    //     ctx.shadowOffsetX = 3;
    //     ctx.shadowOffsetY = 4;

    //     // 🌟 2. 【第一层：纯白卡纸剪边（Die-cut White Border）】
    //     ctx.fillStyle = "#FFFFFF";
    //     for (const cell of this.cells) {
    //         ctx.beginPath();
    //         ctx.arc(cell.x * L, cell.y * L, r + border, 0, 2 * Math.PI);
    //         ctx.fill();
    //     }
    //     for (let i = 1; i < this.cells.length; i++) {
    //         const a = this.cells[i - 1], b = this.cells[i];
    //         if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps) continue;
    //         if (Math.abs(a.x - b.x) < this.eps) {
    //             ctx.fillRect(a.x * L - (r + border), Math.min(a.y, b.y) * L, d + border * 2, Math.abs(a.y - b.y) * L);
    //         } else {
    //             ctx.fillRect(Math.min(a.x, b.x) * L, a.y * L - (r + border), Math.abs(a.x - b.x) * L, d + border * 2);
    //         }
    //     }
    //     ctx.restore(); // 阴影已随白卡纸投射完毕

    //     // 🌟 3. 【第二层：彩色蛇身印花贴纸】
    //     ctx.fillStyle = this.color;
    //     if (this.status === "die") {
    //         ctx.fillStyle = "#CBD5E1";
    //     }
    //     for (const cell of this.cells) {
    //         ctx.beginPath();
    //         ctx.arc(cell.x * L, cell.y * L, r, 0, 2 * Math.PI);
    //         ctx.fill();
    //     }
    //     for (let i = 1; i < this.cells.length; i++) {
    //         const a = this.cells[i - 1], b = this.cells[i];
    //         if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps) continue;
    //         if (Math.abs(a.x - b.x) < this.eps) {
    //             ctx.fillRect(a.x * L - r, Math.min(a.y, b.y) * L, d, Math.abs(a.y - b.y) * L);
    //         } else {
    //             ctx.fillRect(Math.min(a.x, b.x) * L, a.y * L - r, Math.abs(a.x - b.x) * L, d);
    //         }
    //     }

    //     // 🌟 4. 纸片马力欧标志性大眼睛
    //     if (this.status === "die") {
    //         ctx.strokeStyle = "#334155";
    //         ctx.lineWidth = L * 0.035;
    //         ctx.lineCap = "round";
    //         for (let i = 0; i < 2; i++) {
    //             const eye_x = (this.cells[0].x + this.eye_dx[this.eye_direction][i] * 0.14) * L;
    //             const eye_y = (this.cells[0].y + this.eye_dy[this.eye_direction][i] * 0.14) * L;
    //             const sz = L * 0.045;
    //             ctx.beginPath();
    //             ctx.moveTo(eye_x - sz, eye_y - sz);
    //             ctx.lineTo(eye_x + sz, eye_y + sz);
    //             ctx.moveTo(eye_x + sz, eye_y - sz);
    //             ctx.lineTo(eye_x - sz, eye_y + sz);
    //             ctx.stroke();
    //         }
    //     } else {
    //         for (let i = 0; i < 2; i++) {
    //             const eye_x = (this.cells[0].x + this.eye_dx[this.eye_direction][i] * 0.14) * L;
    //             const eye_y = (this.cells[0].y + this.eye_dy[this.eye_direction][i] * 0.14) * L;

    //             // ① 剪纸大眼白
    //             ctx.fillStyle = "#FFFFFF";
    //             ctx.beginPath();
    //             ctx.arc(eye_x, eye_y, L * 0.09, 0, 2 * Math.PI);
    //             ctx.fill();

    //             // ② 哑光黑眼珠
    //             ctx.fillStyle = "#0F172A";
    //             ctx.beginPath();
    //             ctx.arc(eye_x, eye_y, L * 0.055, 0, 2 * Math.PI);
    //             ctx.fill();

    //             // ③ 高光小白点
    //             ctx.fillStyle = "#FFFFFF";
    //             ctx.beginPath();
    //             ctx.arc(eye_x - L * 0.018, eye_y - L * 0.018, L * 0.02, 0, 2 * Math.PI);
    //             ctx.fill();
    //         }
    //     }
    // }

}
