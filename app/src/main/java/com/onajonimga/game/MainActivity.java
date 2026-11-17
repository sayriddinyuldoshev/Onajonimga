package com.onajonimga.game;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    final int N=6;
    final String[] flowers={"🌹","🌷","🌻","🌸","🌼","💐"};
    int[][] board=new int[N][N];
    Button[][] cells=new Button[N][N];
    int selected=-1, score=0, moves=25, level=1;
    int target=100;
    boolean busy=false;
    TextView scoreView,movesView,levelView,targetView;
    GridLayout grid;
    Random random=new Random();

    int dp(int n) {
        return (int)(n*getResources().getDisplayMetrics().density);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        level=getPreferences(0).getInt("level",1);
        if(level>100) level=100;
        startLevel();
    }

    TextView label(String s,int size,int color) {
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(5,8,5,8);
        return t;
    }

    void startLevel() {
        score=0;
        selected=-1;
        moves=Math.max(15,25-(level-1)/10);
        target=100+(level-1)*75;
        busy=false;
        showGame();
    }

    void showGame() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(1);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(12),dp(15),dp(12),dp(10));
        root.setBackgroundColor(Color.rgb(255,235,245));

        root.addView(label("🌷 ONAJONIMGA 🌷",27,
            Color.rgb(174,65,115)));
        root.addView(label("Onajonim uchun mehr bilan ❤️",15,
            Color.rgb(130,75,105)));

        levelView=label("LEVEL "+level+" / 100",22,
            Color.rgb(180,65,120));
        root.addView(levelView);

        scoreView=label("Ochko: "+score,17,Color.DKGRAY);
        targetView=label("Maqsad: "+target,15,Color.DKGRAY);
        movesView=label("Yurishlar: "+moves,15,Color.DKGRAY);

        root.addView(scoreView);
        root.addView(targetView);
        root.addView(movesView);

        root.addView(label("3 ta yoki ko‘proq bir xil gulni toping!",13,
            Color.DKGRAY));

        grid=new GridLayout(this);
        grid.setColumnCount(N);
        grid.setRowCount(N);
        grid.setBackgroundColor(Color.rgb(255,210,230));
        grid.setPadding(dp(3),dp(3),dp(3),dp(3));

        int width=getResources().getDisplayMetrics().widthPixels;
        int cell=(width-dp(48))/N;

        for(int r=0;r<N;r++) {
            for(int c=0;c<N;c++) {
                Button btn=new Button(this);
                cells[r][c]=btn;
                btn.setTextSize(22);
                btn.setPadding(0,0,0,0);
                GridLayout.LayoutParams p=new GridLayout.LayoutParams();
                p.width=cell;
                p.height=cell;
                p.setMargins(dp(1),dp(1),dp(1),dp(1));
                grid.addView(btn,p);
                final int rr=r,cc=c;
                btn.setOnClickListener(v->tap(rr,cc));
            }
        }
        root.addView(grid);

        Button reset=new Button(this);
        reset.setText("LEVELNI QAYTA BOSHLASH");
        root.addView(reset);
        reset.setOnClickListener(v->startLevel());

        root.addView(label("❤️ Onajon, sizni yaxshi ko‘raman! ❤️",
            15,Color.rgb(190,55,115)));

        setContentView(root);
        fillBoard();
        update();
    }

    void fillBoard() {
        for(int r=0;r<N;r++)
            for(int c=0;c<N;c++)
                board[r][c]=random.nextInt(flowers.length);
    }

    void update() {
        for(int r=0;r<N;r++) {
            for(int c=0;c<N;c++) {
                cells[r][c].setText(flowers[board[r][c]]);
                cells[r][c].setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        selected==r*N+c ?
                        Color.rgb(255,165,205) : Color.WHITE
                    )
                );
            }
        }
        scoreView.setText("Ochko: "+score);
        targetView.setText("Maqsad: "+target);
        movesView.setText("Yurishlar: "+moves);
        levelView.setText("LEVEL "+level+" / 100");
    }

    void tap(int r,int c) {
        if(busy || moves<=0) return;
        int pos=r*N+c;

        if(selected<0) {
            selected=pos;
            update();
            return;
        }

        int sr=selected/N, sc=selected%N;
        selected=-1;

        if(Math.abs(sr-r)+Math.abs(sc-c)!=1) {
            update();
            return;
        }

        int temp=board[r][c];
        board[r][c]=board[sr][sc];
        board[sr][sc]=temp;

        if(!hasMatch()) {
            temp=board[r][c];
            board[r][c]=board[sr][sc];
            board[sr][sc]=temp;
            update();
            return;
        }

        moves--;
        busy=true;
        resolve();
    }

    boolean hasMatch() {
        return findMatches()>0;
    }

    int findMatches() {
        boolean[][] mark=new boolean[N][N];
        for(int r=0;r<N;r++) {
            for(int c=0;c<N;c++) {
                int v=board[r][c];
                if(v<0) continue;
                if(c+2<N && board[r][c+1]==v && board[r][c+2]==v) {
                    int k=c;
                    while(k<N && board[r][k]==v) mark[r][k++]=true;
                }
                if(r+2<N && board[r+1][c]==v && board[r+2][c]==v) {
                    int k=r;
                    while(k<N && board[k][c]==v) mark[k++][c]=true;
                }
            }
        }
        int count=0;
        for(int r=0;r<N;r++)
            for(int c=0;c<N;c++)
                if(mark[r][c]) {
                    board[r][c]=-1;
                    count++;
                }
        return count;
    }

    void resolve() {
        int count=findMatches();
        if(count>0) {
            score+=count*10;
            update();
            handler.postDelayed(()->{
                dropFlowers();
                update();
                resolve();
            },250);
        } else {
            busy=false;
            update();
            if(score>=target) winLevel();
            else if(moves<=0) loseLevel();
        }
    }

    void dropFlowers() {
        for(int c=0;c<N;c++) {
            int write=N-1;
            for(int r=N-1;r>=0;r--) {
                if(board[r][c]>=0) {
                    board[write--][c]=board[r][c];
                }
            }
            while(write>=0) {
                board[write--][c]=random.nextInt(flowers.length);
            }
        }
    }

    void winLevel() {
        busy=true;
        if(level<100) {
            level++;
            getPreferences(0).edit().putInt("level",level).apply();
            new AlertDialog.Builder(this)
                .setTitle("🌸 Ajoyib! Level bajarildi!")
                .setMessage("Keyingi bosqich ochildi!\nYangi level: "+level)
                .setCancelable(false)
                .setPositiveButton("DAVOM ETISH",(d,w)->startLevel())
                .show();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("🏆 TABRIKLAYMIZ!")
                .setMessage("100 ta levelni tugatdingiz! Onajoningizga mehr bilan!")
                .setPositiveButton("YANA BOSHLASH",(d,w)->{
                    level=1;
                    getPreferences(0).edit().putInt("level",1).apply();
                    startLevel();
                }).show();
        }
    }

    void loseLevel() {
        busy=true;
        new AlertDialog.Builder(this)
            .setTitle("🌷 Yana urinib ko‘ring!")
            .setMessage("Bu safar maqsadga yetmadingiz.\nLevel: "+level)
            .setPositiveButton("QAYTA O‘YNASH",(d,w)->startLevel())
            .show();
    }
}
