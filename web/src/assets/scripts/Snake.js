import AcGameObject from "./ACGameObject.js";
import { Cell } from "./Cell.js";

export class Snake extends AcGameObject {
    constructor(info, gamemap) {
        super();
        this.id = info.id;
        this.color = info.color
        this.username = info.username || ""; // shown above the head
        this.is_me = info.is_me || false; // whether this is the logged-in user's snake
        this.gamemap = gamemap;

        this.cells = [new Cell(info.r, info.c)]; // body cells, head first
        this.next_cell = null; // cell the head is moving to
        this.speed = 5; // cells per second
        // Directions received but not yet taken (0: up, 1: right, 2: down, 3: left). A new direction waits here while the previous step is still animating.
        // A single slot is not enough: animation pauses in a background tab, later directions would overwrite earlier ones, and the board would drift out of sync with the backend.
        this.directions = [];
        this.result = null; // game result, applied after the queued directions are played: { dead, eye_direction }
        this.status = "idle"; // "idle", "move" or "die"

        this.dr = [-1, 0, 1, 0]; // row delta for directions 0, 1, 2, 3
        this.dc = [0, 1, 0, -1]; // col delta for directions 0, 1, 2, 3

        this.step = 0; // number of steps taken
        this.eps = 1e-2; // distance tolerance

        this.eye_direction = 0; // facing up by default
        if (this.id === 1) {
            this.eye_direction = 2; // the top-right snake starts facing down
        }
        // eye offsets from the head center, for each direction
        this.eye_dx = [
            [-1, 1],
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
        this.directions.push(d);
    }

    has_next_step() {
        return this.directions.length > 0;
    }

    // Game over: dead means this snake lost; eye_direction is the direction of its last attempted move (may be empty)
    finish(dead, eye_direction) {
        this.result = { dead, eye_direction };
    }

    apply_result() { // show the result once the queued directions are played
        if (!this.result || this.status !== "idle" || this.has_next_step()) return;
        if (this.result.eye_direction != null) this.eye_direction = this.result.eye_direction;
        if (this.result.dead) this.status = "die";
        this.result = null;
    }

    check_tail_increasing() { // whether the snake grows on this step
        if (this.step <= 10) return true; // grows on each of the first 10 steps
        if (this.step % 3 === 1) return true; // then on every 3rd step
        return false;
    }

    next_step() { // compute the next head cell
        const d = this.directions.shift();
        this.eye_direction = d; // the eyes turn to face the direction of travel
        this.next_cell = new Cell(this.cells[0].r + this.dr[d], this.cells[0].c + this.dc[d]);
        this.status = "move";
        this.step++;

        const k = this.cells.length;

        for (let i = k; i > 0; i--) {
            this.cells[i] = JSON.parse(JSON.stringify(this.cells[i - 1]));
        }

        if (!this.gamemap.check_valid(this.next_cell)) { // next_cell is invalid, the snake dies
            this.status = "die";
        }
    }

    update_move() {
        const dx = this.next_cell.x - this.cells[0].x;
        const dy = this.next_cell.y - this.cells[0].y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        const move_distance = this.speed * this.timedelta / 1000;
        // Also count it as arrived if this frame would reach the target: after a stall or when a background tab comes back, timedelta is large, and a proportional move would overshoot and jitter around the target
        if (distance < this.eps || move_distance >= distance) { // reached the target cell
            this.status = "idle";
            this.cells[0] = this.next_cell; // add the new head
            this.next_cell = null;

            if (!this.check_tail_increasing()) { // drop the tail unless the snake grows
                this.cells.pop();
            }
        } else { // not there yet, keep moving
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

    update() { // called every frame
        if (this.status === "move") {
            this.update_move();
        }
        this.apply_result();
        this.render();
    }

    draw_body(radius, color) {
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;
        ctx.fillStyle = color;

        for (const cell of this.cells) {
            ctx.beginPath();
            ctx.arc(cell.x * L, cell.y * L, radius, 0, 2 * Math.PI);
            ctx.fill();
        }
        for (let i = 1; i < this.cells.length; i++) {
            const a = this.cells[i - 1], b = this.cells[i];
            if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps) continue;
            if (Math.abs(a.x - b.x) < this.eps) {
                ctx.fillRect(a.x * L - radius, Math.min(a.y, b.y) * L, 2 * radius, Math.abs(a.y - b.y) * L);
            } else {
                ctx.fillRect(Math.min(a.x, b.x) * L, a.y * L - radius, Math.abs(a.x - b.x) * L, 2 * radius);
            }
        }
    }

    render() {
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;

        const r = L * 0.38; // body radius
        const border = L * 0.045; // width of the white sticker-style outline

        // drop shadow for a paper-cutout look
        ctx.save();
        ctx.shadowColor = "rgba(0, 0, 0, 0.25)";
        ctx.shadowBlur = 6;
        ctx.shadowOffsetY = 3;

        const color = this.status === "die" ? "#CBD5E1" : this.color;
        this.draw_body(r + border, "white");
        this.draw_body(r, color);

        // restore the context (removes the shadow)
        ctx.restore();

        // eyes
        if (this.status === "die") {
            // dead: X X eyes
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
            // alive: dark pupils with a highlight
            for (let i = 0; i < 2; i++) {
                const eye_x = (this.cells[0].x + this.eye_dx[this.eye_direction][i] * 0.12) * L;
                const eye_y = (this.cells[0].y + this.eye_dy[this.eye_direction][i] * 0.12) * L;
                // pupil
                ctx.fillStyle = "#1E293B";
                ctx.beginPath();
                ctx.arc(eye_x, eye_y, L * 0.065, 0, 2 * Math.PI);
                ctx.fill();
                // small white highlight
                ctx.fillStyle = "white";
                ctx.beginPath();
                ctx.arc(eye_x - L * 0.02, eye_y - L * 0.02, L * 0.025, 0, 2 * Math.PI);
                ctx.fill();
            }
        }

        this.render_name();
    }

    render_name() { // draw the username above the head
        if (!this.username) return;
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;
        let name = this.username.length > 12 ? this.username.slice(0, 12) + "…" : this.username;
        if (this.is_me) name += " (you)";

        ctx.save();
        ctx.font = `bold ${Math.max(10, Math.round(L * 0.3))}px sans-serif`;
        const w = ctx.measureText(name).width + L * 0.24;
        const h = L * 0.42;
        // Center the label above the head; near the left or right edge, shift it inward so the canvas does not clip it
        const head = this.cells[0];
        const x = Math.min(Math.max(head.x * L, w / 2), ctx.canvas.width - w / 2);
        const y = (head.y - 0.62) * L;

        // Your snake: amber label with dark text. Opponent: translucent dark label with white text. Both stay readable on the grass.
        ctx.fillStyle = this.is_me ? "#F59E0B" : "rgba(15, 23, 42, 0.65)";
        ctx.beginPath();
        ctx.roundRect(x - w / 2, y - h / 2, w, h, h / 2);
        ctx.fill();

        ctx.fillStyle = this.is_me ? "#1E293B" : "white";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(name, x, y);
        ctx.restore();
    }
}
