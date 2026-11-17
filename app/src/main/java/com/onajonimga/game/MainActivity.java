package com.onajonimga.game;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    final int N = 7;
    final String[] flowers = {"🌹","🌷","🌻","🌸","🌼","🌺"};
    final int BG = Color.rgb(255,239,247);
    final int PINK = Color.rgb(190,65,125);
    final int PURPLE = Color.rgb(130,75,155);

    int[][] board = new int[N][N];
    TextView[][] cells = new TextView[N][N];

    int level = 1, score = 0, best = 0;
    int moves = 25, target = 100;
    int selected = -1;
    int combo = 1;
    boolean busy = false;

    GridLayout grid;
    LinearLayout scoreView, movesView, targetView;
    TextView levelView, bestView, progressView;

    Random random = new Random();
    Handler handler = new Handler();

    int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke > 0) d.setStroke(dp(1), stroke);
        return d;
    }

    TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(dp(5),dp(5),dp(5),dp(5));
        return t;
    }

    LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w,h);
    }

    TextView button(String title) {
        TextView b = text(title,14,Color.WHITE,true);
        b.setBackground(shape(PINK,18,0));
        b.setPadding(dp(15),dp(12),dp(15),dp(12));
        b.setElevation(dp(3));
        return b;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(Color.rgb(245,215,230));
        getWindow().setNavigationBarColor(Color.rgb(255,239,247));

        level = getPreferences(0).getInt("level",1);
        best = getPreferences(0).getInt("best",0);

        if (level < 1 || level > 100) level = 1;

        startLevel();
    }

    void startLevel() {
        score = 0;
        selected = -1;
        busy = false;
        combo = 1;

        moves = Math.max(15,25-(level-1)/10);
        target = 100+(level-1)*75;

        fillBoard();
        showGame();
    }

    void fillBoard() {
        for (int r=0;r<N;r++) {
            for (int c=0;c<N;c++) {
                int v;
                do {
                    v = random.nextInt(flowers.length);
                    board[r][c] = v;
                } while (
                    (c>=2 && board[r][c-1]==v && board[r][c-2]==v) ||
                    (r>=2 && board[r-1][c]==v && board[r-2][c]==v)
                );
            }
        }
    }

    void showGame() {
        ScrollView scroll = new ScrollView(this);

        LinearLayout root = vertical();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(13),dp(12),dp(13),dp(15));
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.rgb(255,229,241),BG,Color.WHITE}
        ));

        TextView title = text("🌸 ONAJONIMGA 🌸",29,PINK,true);
        root.addView(title,lp(-1,-2));

        root.addView(text(
            "ONAJONIM UCHUN MEHR BILAN ❤️",12,PURPLE,true
        ));

        root.addView(text("✦  FLOWER GARDEN  ✦",12,PURPLE,true));

        LinearLayout levelCard = vertical();
        levelCard.setGravity(Gravity.CENTER);
        levelCard.setPadding(dp(12),dp(9),dp(12),dp(9));
        levelCard.setBackground(shape(Color.WHITE,20,Color.rgb(245,190,215)));

        levelView = text("LEVEL "+level+" / 100",23,PINK,true);
        levelCard.addView(levelView);

        TextView subtitle = text("Har bir gul — onajoningizga mehr!",12,PURPLE,false);
        levelCard.addView(subtitle);

        root.addView(levelCard,lp(-1,-2));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0,dp(10),0,dp(8));

        scoreView = statCard("⭐ OCHKO","0");
        movesView = statCard("👣 YURISH",""+moves);
        targetView = statCard("🎯 MAQSAD",""+target);

        stats.addView(scoreView,new LinearLayout.LayoutParams(0,dp(76),1));
        stats.addView(movesView,new LinearLayout.LayoutParams(0,dp(76),1));
        stats.addView(targetView,new LinearLayout.LayoutParams(0,dp(76),1));

        root.addView(stats);

        bestView = text("🏆 Rekord: "+best,14,PURPLE,true);
        root.addView(bestView);

        progressView = text("Maqsad sari: 0%",13,PINK,true);
        root.addView(progressView);

        root.addView(text(
            "Bir xil 3 yoki undan ortiq gulni birlashtiring",
            13,Color.DKGRAY,false
        ));

        grid = new GridLayout(this);
        grid.setColumnCount(N);
        grid.setRowCount(N);
        grid.setPadding(dp(5),dp(5),dp(5),dp(5));
        grid.setBackground(shape(Color.rgb(250,205,225),20,Color.WHITE));
        grid.setElevation(dp(4));

        int width = getResources().getDisplayMetrics().widthPixels;
        int cell = (width-dp(62))/N;

        for(int r=0;r<N;r++) {
            for(int c=0;c<N;c++) {
                TextView tile = text("",23,Color.BLACK,false);
                cells[r][c] = tile;

                GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                p.width = cell;
                p.height = cell;
                p.setMargins(dp(1),dp(1),dp(1),dp(1));

                grid.addView(tile,p);

                final int rr=r, cc=c;
                tile.setOnClickListener(v -> tap(rr,cc));
            }
        }

        LinearLayout.LayoutParams boardParams = lp(-2,-2);
        boardParams.setMargins(0,dp(10),0,dp(10));
        root.addView(grid,boardParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView restart = button("↻ QAYTA");
        TextView shuffle = button("✦ ARALASHTIR");

        LinearLayout.LayoutParams actionParams =
            new LinearLayout.LayoutParams(0,-2,1);
        actionParams.setMargins(dp(4),0,dp(4),0);

        actions.addView(restart,actionParams);
        actions.addView(shuffle,actionParams);
        root.addView(actions);

        restart.setOnClickListener(v -> confirmRestart());
        shuffle.setOnClickListener(v -> shuffleBoard());

        root.addView(text(
            "💗 ONAJON, SIZNI JUDA YAXSHI KO‘RAMAN! 💗",
            14,PINK,true
        ));

        root.addView(text(
            "100 ta bosqich • Offline o‘yin",
            11,PURPLE,false
        ));

        scroll.addView(root);
        setContentView(scroll);

        update();
    }

    LinearLayout statCard(String name,String value) {
        LinearLayout card = vertical();
        card.setGravity(Gravity.CENTER);
        card.setBackground(shape(Color.WHITE,15,Color.rgb(245,200,220)));
        card.setPadding(dp(3),dp(5),dp(3),dp(5));

        TextView a = text(name,10,PURPLE,true);
        TextView b = text(value,19,PINK,true);

        card.addView(a);
        card.addView(b);

        if(name.contains("OCHKO")) scoreHolder = b;
        if(name.contains("YURISH")) movesHolder = b;
        if(name.contains("MAQSAD")) targetHolder = b;

        return card;
    }

    TextView scoreHolder, movesHolder, targetHolder;

    void update() {
        for(int r=0;r<N;r++) {
            for(int c=0;c<N;c++) {
                TextView t = cells[r][c];
                int pos = r*N+c;

                t.setText(board[r][c] < 0 ? "" : flowers[board[r][c]]);
                t.setBackground(shape(
                    selected==pos ? Color.rgb(255,170,205) : Color.WHITE,
                    11,
                    selected==pos ? PINK : Color.rgb(250,215,230)
                ));

                t.setScaleX(1f);
                t.setScaleY(1f);
                t.setAlpha(1f);
            }
        }

        scoreHolder.setText(""+score);
        movesHolder.setText(""+moves);
        targetHolder.setText(""+target);

        levelView.setText("LEVEL "+level+" / 100");
        bestView.setText("🏆 Rekord: "+best);

        int percent = Math.min(100,score*100/Math.max(1,target));
        progressView.setText("Maqsad sari: "+percent+"%");
    }

    void tap(int r,int c) {
        if(busy || moves<=0) return;

        int pos=r*N+c;

        if(selected<0) {
            selected=pos;
            cells[r][c].animate()
                .scaleX(1.12f).scaleY(1.12f)
                .setDuration(100).start();
            update();
            return;
        }

        int sr=selected/N;
        int sc=selected%N;
        selected=-1;

        if(Math.abs(sr-r)+Math.abs(sc-c)!=1) {
            update();
            return;
        }

        int temp=board[r][c];
        board[r][c]=board[sr][sc];
        board[sr][sc]=temp;

        if(countMatches()==0) {
            temp=board[r][c];
            board[r][c]=board[sr][sc];
            board[sr][sc]=temp;

            cells[sr][sc].animate()
                .translationX(dp(5))
                .setDuration(80)
                .withEndAction(() ->
                    cells[sr][sc].animate().translationX(0).start()
                ).start();

            update();
            return;
        }

        moves--;
        busy=true;

        cells[r][c].animate().scaleX(1.15f).scaleY(1.15f)
            .setDuration(100).start();

        resolve(1);
    }

    boolean[][] matches() {
        boolean[][] mark = new boolean[N][N];

        for(int r=0;r<N;r++) {
            int c=0;
            while(c<N) {
                int v=board[r][c];
                int end=c+1;

                while(v>=0 && end<N && board[r][end]==v) end++;

                if(v>=0 && end-c>=3)
                    for(int k=c;k<end;k++) mark[r][k]=true;

                c=end;
            }
        }

        for(int c=0;c<N;c++) {
            int r=0;
            while(r<N) {
                int v=board[r][c];
                int end=r+1;

                while(v>=0 && end<N && board[end][c]==v) end++;

                if(v>=0 && end-r>=3)
                    for(int k=r;k<end;k++) mark[k][c]=true;

                r=end;
            }
        }

        return mark;
    }

    int countMatches() {
        boolean[][] m=matches();
        int n=0;

        for(int r=0;r<N;r++)
            for(int c=0;c<N;c++)
                if(m[r][c]) n++;

        return n;
    }

    void resolve(int chain) {
        boolean[][] mark=matches();
        int count=0;

        for(int r=0;r<N;r++)
            for(int c=0;c<N;c++)
                if(mark[r][c]) count++;

        if(count==0) {
            busy=false;
            update();

            if(score>=target) winLevel();
            else if(moves<=0) loseLevel();

            return;
        }

        score += count*10*chain;
        if(score>best) {
            best=score;
            getPreferences(0).edit().putInt("best",best).apply();
        }

        for(int r=0;r<N;r++) {
            for(int c=0;c<N;c++) {
                if(mark[r][c]) {
                    board[r][c]=-1;
                    cells[r][c].animate()
                        .scaleX(0.15f).scaleY(0.15f)
                        .alpha(0f).setDuration(170).start();
                }
            }
        }

        update();

        final int next=chain+1;

        handler.postDelayed(() -> {
            dropFlowers();
            update();
            resolve(next);
        },190);
    }

    void dropFlowers() {
        for(int c=0;c<N;c++) {
            int write=N-1;

            for(int r=N-1;r>=0;r--) {
                if(board[r][c]>=0)
                    board[write--][c]=board[r][c];
            }

            while(write>=0)
                board[write--][c]=random.nextInt(flowers.length);
        }
    }

    void shuffleBoard() {
        if(busy) return;

        selected=-1;
        fillBoard();
        update();

        Toast.makeText(this,
            "🌸 Gullar yangilandi!",
            Toast.LENGTH_SHORT).show();
    }

    void confirmRestart() {
        new AlertDialog.Builder(this)
            .setTitle("🌷 Qayta boshlash")
            .setMessage("Ushbu bosqichni boshidan boshlaysizmi?")
            .setNegativeButton("YO‘Q",null)
            .setPositiveButton("HA",(d,w)->startLevel())
            .show();
    }

    void winLevel() {
        busy=true;

        if(level<100) {
            level++;
            getPreferences(0).edit()
                .putInt("level",level).apply();

            new AlertDialog.Builder(this)
                .setTitle("🏆 AJOYIB NATIJA!")
                .setMessage(
                    "🌸 Bosqich muvaffaqiyatli bajarildi!\n\n"+
                    "⭐ Ochko: "+score+
                    "\n🌷 Keyingi bosqich: "+level
                )
                .setCancelable(false)
                .setPositiveButton("KEYINGI LEVEL",(d,w)->startLevel())
                .show();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("👑 G‘ALABA!")
                .setMessage(
                    "100 ta bosqichning barchasi tugadi!\n\n"+
                    "Onajoningizga bo‘lgan mehringiz cheksiz! ❤️"
                )
                .setCancelable(false)
                .setPositiveButton("YANGI O‘YIN",(d,w)->{
                    level=1;
                    getPreferences(0).edit()
                        .putInt("level",1).apply();
                    startLevel();
                })
                .show();
        }
    }

    void loseLevel() {
        busy=true;

        new AlertDialog.Builder(this)
            .setTitle("🌷 Yana bir bor urinib ko‘ring!")
            .setMessage(
                "Bu safar maqsadga yetilmadi.\n"+
                "Yig‘ilgan ochko: "+score+
                "\nKerakli ochko: "+target
            )
            .setCancelable(false)
            .setPositiveButton("QAYTA O‘YNASH",(d,w)->startLevel())
            .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("🌸 O‘yindan chiqish")
            .setMessage("O‘yinni yopmoqchimisiz?")
            .setNegativeButton("DAVOM ETISH",null)
            .setPositiveButton("CHIQISH",(d,w)->finish())
            .show();
    }
}
