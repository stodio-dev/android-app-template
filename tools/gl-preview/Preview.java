package com.stodio.arrowcubes;
import com.stodio.arrowcubes.core.*;
import java.io.*;
import java.util.Locale;
/** Dumps a level's mesh and the shaders as JSON for page.html. Args: level skinIndex backgroundIndex [tapsFirst]. */
public class Preview {
  public static void main(String[] a) throws Exception {
    int level = Integer.parseInt(a[0]);
    Catalog.Skin skin = Catalog.SKINS[Integer.parseInt(a[1])];
    Catalog.Background bg = Catalog.BACKGROUNDS[Integer.parseInt(a[2])];
    Levels.Spec s = Levels.campaign(level);
    Game g = Levels.build(s);
    int remove = a.length > 3 ? Integer.parseInt(a[3]) : 0;
    for (int k = 0; k < remove; k++) { int[] h = g.hints(); if (h.length == 0) break; g.tap(h[0]); }
    float[][] buf = {new float[1024]};
    int n = Mesh.build(g.board, null, skin, s.seed, buf);
    StringBuilder b = new StringBuilder("{");
    b.append("\"sx\":").append(g.board.sx).append(",\"sy\":").append(g.board.sy).append(",\"sz\":").append(g.board.sz);
    b.append(",\"glyph\":").append(skin.glyph).append(",\"accent\":").append(skin.accent).append(",\"spec\":").append(skin.shine);
    b.append(",\"arrow\":").append(skin.arrow).append(",\"corner\":").append(skin.corner).append(",\"border\":").append(skin.border);
    b.append(",\"top\":").append(bg.top).append(",\"bottom\":").append(bg.bottom).append(",\"stars\":").append(bg.stars);
    b.append(",\"vs\":").append(q(Shaders.BLOCK_VS)).append(",\"fs\":").append(q(Shaders.BLOCK_FS));
    b.append(",\"bvs\":").append(q(Shaders.BG_VS)).append(",\"bfs\":").append(q(Shaders.BG_FS));
    b.append(",\"verts\":[");
    for (int i = 0; i < n; i++) { if (i > 0) b.append(','); b.append(String.format(Locale.US, "%.4f", buf[0][i])); }
    b.append("]}");
    System.out.print(b);
  }
  static String q(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }
}
