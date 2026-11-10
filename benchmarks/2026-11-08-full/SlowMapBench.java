import java.util.Locale;
import java.util.Random;

public class SlowMapBench {
    public static void main(String[] args) {
        Random random = new Random(20261026);
        int target = args.length > 0 ? Integer.parseInt(args[0]) : 52;
        int[][] grid = null;
        for (int i = 0; i <= target; i++) grid = MapBench.map(random);
        boolean expected = MapBench.bfs(grid);
        MapBench.calls = 0;
        long start = System.nanoTime();
        MapBench.deadline = start + 30_000_000_000L;
        try {
            boolean result = MapBench.dfs(grid, MapBench.R - 2, 1);
            System.out.printf(Locale.US, "seed=20261026 case=%d bfs_result=%s dfs_result=%s elapsed_ms=%.3f calls=%d%n", target, expected, result, (System.nanoTime()-start)/1e6, MapBench.calls);
        } catch (MapBench.Timeout e) {
            System.out.printf(Locale.US, "seed=20261026 case=%d bfs_result=%s dfs_timeout=true cutoff_ms=30000 elapsed_ms=%.3f calls=%d%n", target, expected, (System.nanoTime()-start)/1e6, MapBench.calls);
        }
    }
}
