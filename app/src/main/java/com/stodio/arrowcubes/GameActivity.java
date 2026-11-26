package com.stodio.arrowcubes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stodio.arrowcubes.core.Catalog;
import com.stodio.arrowcubes.core.Game;
import com.stodio.arrowcubes.core.Levels;
import com.stodio.arrowcubes.core.Shapes;

/**
 * A level (or a free-play structure): the 3D view, the counters on top, the boosters at the bottom, and the cards for
 * a tip, a win and running out of moves. The game itself lives on the GL thread (see CubeRenderer).
 */
public final class GameActivity extends Activity implements CubeRenderer.Listener {
    static final String EXTRA_LEVEL = "level", EXTRA_SHAPE = "shape", EXTRA_SIZE = "size";

    private GameView gl;
    private CubeRenderer renderer;
    private Catalog.Background bg;
    private int level, freeShape, freeSize, cells;
    private TextView title, coins, moves, left, keys, message, hintBtn, hammerBtn;
    private FrameLayout root, overlay;
    private ConfettiView confetti;
    private View tipCard;
    private boolean hammerOn, ended, loading;
    private final Runnable hideMessage = new Runnable() {
        @Override public void run() {
            message.animate().alpha(0f).setDuration(250);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        level = getIntent().getIntExtra(EXTRA_LEVEL, 0);
        freeShape = getIntent().getIntExtra(EXTRA_SHAPE, 0);
        freeSize = getIntent().getIntExtra(EXTRA_SIZE, 0);
        bg = Store.background(this);
        Ui.bars(this, bg);
        Sfx.on = Store.sound(this);

        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(Ui.gradient(bg));
        gl = new GameView(this, false);
        renderer = gl.renderer;
        renderer.setListener(this);
        final Catalog.Skin skin = Store.skin(this);
        gl.queueEvent(new Runnable() {
            @Override public void run() {
                renderer.setLook(skin, bg);
            }
        });
        root.addView(gl, new FrameLayout.LayoutParams(-1, -1));
        root.addView(topBar(), new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        root.addView(bottomBar(), new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        message = Ui.text(this, "", 15, Color.WHITE, true);
        message.setGravity(Gravity.CENTER);
        message.setBackground(Ui.round(0xDD23262E, Ui.dp(this, 16)));
        message.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));
        message.setAlpha(0f);
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        mp.bottomMargin = Ui.dp(this, 110);
        mp.leftMargin = mp.rightMargin = Ui.dp(this, 24);
        root.addView(message, mp);

        confetti = new ConfettiView(this);
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x88000000);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        root.addView(confetti, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        startLevel();
    }

    @Override protected void onResume() {
        super.onResume();
        gl.onResume();
        refreshCoins();
    }

    @Override protected void onPause() {
        gl.onPause();
        super.onPause();
    }

    // ---------------- layout ----------------

    private View topBar() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), 0);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        title = Ui.text(this, "", 22, Ui.onBg(bg), true);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        coins = Ui.coinChip(this, Store.coins(this));
        row.addView(coins);
        TextView close = Ui.button(this, "✕", 0xEEFFFFFF, Ui.INK, 16);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 40));
        cp.setMarginStart(Ui.dp(this, 8));
        row.addView(close, cp);
        col.addView(row);

        LinearLayout chips = new LinearLayout(this);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        moves = chip();
        left = chip();
        keys = chip();
        keys.setBackground(Ui.round(0xFFF5BE2E, Ui.dp(this, 14)));
        keys.setVisibility(View.GONE);
        chips.addView(moves);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(-2, -2);
        gap.setMarginStart(Ui.dp(this, 8));
        chips.addView(left, gap);
        LinearLayout.LayoutParams gap2 = new LinearLayout.LayoutParams(-2, -2);
        gap2.setMarginStart(Ui.dp(this, 8));
        chips.addView(keys, gap2);
        col.addView(chips, Ui.lp(-2, -2, Ui.dp(this, 8)));
        return col;
    }

    private TextView chip() {
        TextView t = Ui.text(this, "", 15, Ui.INK, true);
        t.setBackground(Ui.round(0xE6FFFFFF, Ui.dp(this, 14)));
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 5), Ui.dp(this, 12), Ui.dp(this, 5));
        return t;
    }

    private View bottomBar() {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), Ui.dp(this, 14));
        hintBtn = tool("💡", "רמז · " + Catalog.HINT_PRICE);
        hammerBtn = tool("🔨", "פטיש · " + Catalog.HAMMER_PRICE);
        TextView center = tool("◎", "מרכוז");
        TextView restart = tool("↻", "מחדש");
        hintBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                hint();
            }
        });
        hammerBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                toggleHammer();
            }
        });
        center.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                renderer.resetView();
            }
        });
        restart.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!loading) startLevel();
            }
        });
        int m = Ui.dp(this, 4);
        row.addView(hintBtn, Ui.weight(1, m));
        row.addView(hammerBtn, Ui.weight(1, m));
        row.addView(center, Ui.weight(1, m));
        row.addView(restart, Ui.weight(1, m));
        return row;
    }

    private TextView tool(String icon, String label) {
        TextView t = Ui.button(this, icon + "\n" + label, 0xEEFFFFFF, Ui.INK, 13);
        t.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
        return t;
    }

    // ---------------- level ----------------

    private void startLevel() {
        ended = false;
        loading = true;
        hideOverlay();
        if (hammerOn) toggleHammer();
        if (tipCard != null) {
            root.removeView(tipCard);
            tipCard = null;
        }
        final Levels.Spec spec = level > 0 ? Levels.campaign(level) : Levels.free(freeShape, freeSize, System.nanoTime());
        title.setText(level > 0 ? "שלב " + level : "חופשי · " + Shapes.NAMES[freeShape]);
        moves.setText("…");
        left.setText("");
        App.BG.execute(new Runnable() {
            @Override public void run() {
                long t0 = SystemClock.uptimeMillis();
                final Game g;
                try {
                    g = Levels.build(spec);
                } catch (Throwable e) {
                    App.log("level " + level + " build failed", e);
                    App.ui(new Runnable() {
                        @Override public void run() {
                            say("לא הצלחתי לבנות את השלב. שתפו את יומן התקלות מהמסך הראשי.");
                        }
                    });
                    return;
                }
                final int removable = g.board.removable(), movesLeft = g.movesLeft(), keyCount = g.board.keys();
                App.log("level " + level + " shape " + spec.shape + " step " + spec.step + " blocks "
                        + removable + " limit " + g.limit() + " in " + (SystemClock.uptimeMillis() - t0) + " ms");
                App.ui(new Runnable() {
                    @Override public void run() {
                        if (isFinishing()) return;
                        cells = removable;
                        loading = false;
                        showCounts(movesLeft, 0, removable, keyCount);
                        renderer.resetView();
                        gl.queueEvent(new Runnable() {
                            @Override public void run() {
                                renderer.setGame(g, spec.seed);
                            }
                        });
                        maybeTip();
                    }
                });
            }
        });
    }

    private void showCounts(int movesLeft, int used, int remaining, int keyCount) {
        if (movesLeft >= 0) {
            moves.setText("מהלכים: " + movesLeft);
            moves.setBackground(Ui.round(movesLeft <= 5 ? 0xFFFFD6D3 : 0xE6FFFFFF, Ui.dp(this, 14)));
        } else {
            moves.setText("מהלכים: " + used);
        }
        left.setText("נותרו: " + remaining);
        keys.setVisibility(keyCount > 0 ? View.VISIBLE : View.GONE);
        keys.setText("מוזהבות: " + keyCount);
    }

    private void maybeTip() {
        String tip = level > 0 ? Levels.tip(level) : null;
        if (tip == null || !Store.firstTip(this, level)) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.round(0xF7FFFFFF, Ui.dp(this, 18)));
        int p = Ui.dp(this, 16);
        card.setPadding(p, p, p, p);
        card.setElevation(Ui.dp(this, 6));
        card.addView(Ui.text(this, tip, 16, Ui.INK, false));
        TextView ok = Ui.button(this, "הבנתי", Ui.ACCENT, Color.WHITE, 15);
        card.addView(ok, Ui.lp(-1, -2, Ui.dp(this, 12)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        lp.topMargin = Ui.dp(this, 110);
        lp.leftMargin = lp.rightMargin = Ui.dp(this, 18);
        tipCard = card;
        root.addView(card, root.indexOfChild(overlay), lp);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                root.removeView(tipCard);
                tipCard = null;
            }
        });
    }

    // ---------------- boosters ----------------

    private void hint() {
        if (ended || loading) return;
        if (!Store.spend(this, Catalog.HINT_PRICE)) {
            say("אין מספיק מטבעות לרמז");
            return;
        }
        refreshCoins();
        gl.queueEvent(new Runnable() {
            @Override public void run() {
                renderer.hint();
            }
        });
    }

    private void toggleHammer() {
        if (!hammerOn && (ended || loading)) return;
        if (!hammerOn && Store.coins(this) < Catalog.HAMMER_PRICE) {
            say("אין מספיק מטבעות לפטיש");
            return;
        }
        hammerOn = !hammerOn;
        final boolean on = hammerOn;
        gl.queueEvent(new Runnable() {
            @Override public void run() {
                renderer.setHammerMode(on);
            }
        });
        hammerBtn.setBackground(Ui.pressable(this, on ? 0xFFFFE08A : 0xEEFFFFFF, 14));
        if (on) say("הקישו על קובייה כדי לנפץ אותה");
    }

    // ---------------- results from the GL thread ----------------

    @Override public void onTap(final CubeRenderer.State s) {
        App.ui(new Runnable() {
            @Override public void run() {
                showCounts(s.movesLeft, s.moves, s.remaining, s.keys);
                switch (s.result) {
                    case Game.EXIT:
                        Sfx.play(Sfx.POP);
                        gl.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        break;
                    case Game.BLOCKED:
                        Sfx.play(Sfx.BUMP);
                        gl.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        break;
                    case Game.CRACKED:
                        Sfx.play(Sfx.CRACK);
                        gl.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        break;
                    case Game.LOCKED:
                        Sfx.play(Sfx.DENY);
                        say("נעולה: קודם צריך להוציא את כל הקוביות המוזהבות");
                        break;
                    case Game.FIXED:
                        Sfx.play(Sfx.DENY);
                        say("אבן לא זזה, אבל גם לא צריך להוציא אותה");
                        break;
                    default:
                        break;
                }
                checkEnd(s);
            }
        });
    }

    @Override public void onHint(final boolean found, final boolean visible) {
        App.ui(new Runnable() {
            @Override public void run() {
                if (!found) {
                    Store.addCoins(GameActivity.this, Catalog.HINT_PRICE);
                    refreshCoins();
                    say("אין כרגע קובייה פנויה");
                    return;
                }
                Sfx.play(Sfx.HINT);
                say(visible ? "הקובייה המהבהבת יכולה לצאת" : "סובבו את המבנה: הקובייה המהבהבת מסתתרת מאחור");
            }
        });
    }

    @Override public void onHammer(final CubeRenderer.State s) {
        App.ui(new Runnable() {
            @Override public void run() {
                Store.spend(GameActivity.this, Catalog.HAMMER_PRICE);
                refreshCoins();
                hammerOn = false;
                hammerBtn.setBackground(Ui.pressable(GameActivity.this, 0xEEFFFFFF, 14));
                Sfx.play(Sfx.CRACK);
                gl.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                showCounts(s.movesLeft, s.moves, s.remaining, s.keys);
                checkEnd(s);
            }
        });
    }

    private void checkEnd(final CubeRenderer.State s) {
        if (ended) return;
        if (s.won) {
            ended = true;
            root.postDelayed(new Runnable() {
                @Override public void run() {
                    showWin(s);
                }
            }, 700);
        } else if (s.lost) {
            ended = true;
            root.postDelayed(new Runnable() {
                @Override public void run() {
                    showLose(s);
                }
            }, 550);
        }
    }

    // ---------------- cards ----------------

    private void showWin(CubeRenderer.State s) {
        if (isFinishing()) return;
        int earned;
        String head;
        if (level > 0) {
            earned = Store.finish(this, level, s.stars);
            head = level >= Levels.COUNT ? "סיימתם את כל השלבים!" : "כל הכבוד!";
        } else {
            earned = Catalog.freeReward(cells);
            Store.addCoins(this, earned);
            head = "פירקתם את ה" + Shapes.NAMES[freeShape] + "!";
        }
        App.log("won level " + level + " stars " + s.stars + " wasted " + s.wasted + " coins +" + earned);
        refreshCoins();
        Sfx.play(Sfx.WIN);
        LinearLayout card = card();
        card.addView(center(Ui.text(this, head, 26, Ui.INK, true)));
        if (level > 0) {
            TextView stars = center(Ui.text(this, Ui.stars(s.stars), 44, 0xFFF2B01E, false));
            card.addView(stars, Ui.lp(-1, -2, Ui.dp(this, 6)));
        }
        TextView coinsLine = center(Ui.text(this, "+" + earned + " מטבעות", 18, Ui.INK, true));
        coinsLine.setCompoundDrawablesRelative(Ui.coinIcon(this, 18), null, null, null);
        coinsLine.setCompoundDrawablePadding(Ui.dp(this, 6));
        card.addView(coinsLine, Ui.lp(-2, -2, Ui.dp(this, 8)));
        card.addView(center(Ui.text(this, "מהלכים: " + s.moves + " · הקשות מבוזבזות: " + s.wasted, 14, Ui.MUTED, false)),
                Ui.lp(-1, -2, Ui.dp(this, 6)));
        if (level > 0 && level < Levels.COUNT) {
            card.addView(action("לשלב הבא", Ui.ACCENT, Color.WHITE, new Runnable() {
                @Override public void run() {
                    level++;
                    startLevel();
                }
            }), Ui.lp(-1, -2, Ui.dp(this, 18)));
            card.addView(action("לשחק שוב", 0xFFEEF1F7, Ui.INK, new Runnable() {
                @Override public void run() {
                    startLevel();
                }
            }), Ui.lp(-1, -2, Ui.dp(this, 10)));
        } else {
            card.addView(action(level > 0 ? "לשחק שוב" : "עוד אחד", Ui.ACCENT, Color.WHITE, new Runnable() {
                @Override public void run() {
                    startLevel();
                }
            }), Ui.lp(-1, -2, Ui.dp(this, 18)));
        }
        card.addView(action("לתפריט", 0xFFEEF1F7, Ui.INK, new Runnable() {
            @Override public void run() {
                finish();
            }
        }), Ui.lp(-1, -2, Ui.dp(this, 10)));
        showOverlay(card);
        confetti.burst();
    }

    private void showLose(final CubeRenderer.State s) {
        if (isFinishing()) return;
        App.log("lost level " + level + " remaining " + s.remaining);
        Sfx.play(Sfx.LOSE);
        LinearLayout card = card();
        card.addView(center(Ui.text(this, "נגמרו המהלכים", 26, Ui.INK, true)));
        card.addView(center(Ui.text(this, "נשארו עוד " + s.remaining + " קוביות", 16, Ui.MUTED, false)),
                Ui.lp(-1, -2, Ui.dp(this, 6)));
        boolean afford = Store.coins(this) >= Catalog.MOVES_PRICE;
        TextView more = action("+" + Catalog.MOVES_BONUS + " מהלכים · " + Catalog.MOVES_PRICE + " מטבעות",
                afford ? Ui.GOOD : 0xFFB9BDC6, Color.WHITE, new Runnable() {
                    @Override public void run() {
                        if (!Store.spend(GameActivity.this, Catalog.MOVES_PRICE)) {
                            say("אין מספיק מטבעות");
                            return;
                        }
                        refreshCoins();
                        hideOverlay();
                        ended = false;
                        gl.queueEvent(new Runnable() {
                            @Override public void run() {
                                renderer.addMoves(Catalog.MOVES_BONUS);
                            }
                        });
                    }
                });
        card.addView(more, Ui.lp(-1, -2, Ui.dp(this, 18)));
        card.addView(action("לנסות שוב", Ui.ACCENT, Color.WHITE, new Runnable() {
            @Override public void run() {
                startLevel();
            }
        }), Ui.lp(-1, -2, Ui.dp(this, 10)));
        card.addView(action("לתפריט", 0xFFEEF1F7, Ui.INK, new Runnable() {
            @Override public void run() {
                finish();
            }
        }), Ui.lp(-1, -2, Ui.dp(this, 10)));
        showOverlay(card);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setBackground(Ui.round(Color.WHITE, Ui.dp(this, 24)));
        int p = Ui.dp(this, 22);
        card.setPadding(p, p, p, p);
        card.setElevation(Ui.dp(this, 10));
        return card;
    }

    private TextView center(TextView t) {
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView action(String label, int fill, int textColor, final Runnable r) {
        TextView b = Ui.button(this, label, fill, textColor, 17);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                r.run();
            }
        });
        return b;
    }

    private void showOverlay(View card) {
        overlay.removeAllViews();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        lp.leftMargin = lp.rightMargin = Ui.dp(this, 28);
        overlay.addView(card, lp);
        overlay.setAlpha(0f);
        overlay.setVisibility(View.VISIBLE);
        overlay.animate().alpha(1f).setDuration(220);
    }

    private void hideOverlay() {
        overlay.removeAllViews();
        overlay.setVisibility(View.GONE);
    }

    private void say(String s) {
        message.setText(s);
        message.removeCallbacks(hideMessage);
        message.animate().cancel();
        message.setAlpha(1f);
        message.postDelayed(hideMessage, 2600);
    }

    private void refreshCoins() {
        if (coins != null) coins.setText(String.valueOf(Store.coins(this)));
    }

    static Intent campaign(Activity from, int level) {
        return new Intent(from, GameActivity.class).putExtra(EXTRA_LEVEL, level);
    }

    static Intent free(Activity from, int shape, int size) {
        return new Intent(from, GameActivity.class).putExtra(EXTRA_SHAPE, shape).putExtra(EXTRA_SIZE, size);
    }
}
