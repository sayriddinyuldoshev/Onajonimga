package com.onajonimga.game;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    final String[] names = {"MEVALAR","GULLAR","SHIRINLIKLAR","HAYVONLAR"};
    final String[] icons = {"🍓","🌸","🍬","🐼"};
    final int[][] palettes = {
        {0xFFFFE6D9,0xFFE87950},
        {0xFFFFE3EF,0xFFC64B88},
        {0xFFFFF0D2,0xFFD28B30},
        {0xFFE0F2FF,0xFF4286C5}
    };

    final String[][] items = {
        {"🍎","🍐","🍊","🍋","🍇","🍓","🍉","🍒","🥝"},
        {"🌹","🌷","🌻","🌸","🌼","🌺","💐","🌱","🪻"},
        {"🍬","🍭","🍫","🍩","🍪","🧁","🍰","🍦","🍡"},
        {"🐱","🐶","🐼","🐸","🦊","🐰","🐻","🐵","🐧"}
    };

    static class Tile {
        int id,row,col,layer,type;
        boolean removed;

        Tile(int i,int r,int c,int l,int t) {
            id=i; row=r; col=c; layer=l; type=t;
        }
    }

    int mode=0, level=1, score=0, coins=755;
    boolean gameOver=false;
    Tile lastUndo=null;

    ArrayList<Tile> tiles = new ArrayList<>();
    ArrayList<Integer> tray = new ArrayList<>();

    Random random = new Random();

    LinearLayout root, trayRow;
    FrameLayout boardFrame;
    TextView scoreView,levelView,coinView,remainingView;
    ScrollView scroll;

    int dp(float n) {
        return (int)(n*getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int color,int radius,int stroke) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if(stroke!=0) d.setStroke(dp(1),stroke);
        return d;
    }

    TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(5),dp(5),dp(5),dp(5));
        if(bold) t.setTypeface(null,Typeface.BOLD);
        return t;
    }

    LinearLayout vertical() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    TextView button(String s) {
        TextView t=text(s,15,Color.WHITE,true);
        t.setBackground(shape(0xFF344F86,18,0xFF91B8F5));
        t.setElevation(dp(3));
        t.setPadding(dp(10),dp(12),dp(10),dp(12));
        return t;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF347EC5);
        getWindow().setNavigationBarColor(0xFF172B53);
        coins=getPreferences(0).getInt("coins",755);
        showHome();
    }

    void addModeCard(LinearLayout parent,int m) {
        TextView card=text(
            icons[m]+"   "+names[m]+"\n"+
            "O‘zingiz yoqtirgan mavzuda o‘ynang",
            18,Color.WHITE,true
        );
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(20),dp(10),dp(12),dp(10));
        card.setBackground(shape(palettes[m][1],20,Color.WHITE));
        card.setElevation(dp(4));

        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
            -1,dp(82));
        p.setMargins(0,dp(7),0,dp(7));
        parent.addView(card,p);

        card.setOnClickListener(v -> startMode(m));
    }

    void showHome() {
        scroll=new ScrollView(this);
        LinearLayout page=vertical();
        page.setPadding(dp(20),dp(22),dp(20),dp(25));
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF3E8DD1,0xFFB6DDF4,0xFFFFEAF2}
        ));

        page.addView(text("🌸 ONAJONIMGA 🌸",30,Color.WHITE,true));
        page.addView(text("MEHR BILAN YARATILGAN O‘YIN",13,Color.WHITE,true));
        page.addView(text("Qaysi mavzuda o‘ynaysiz?",21,0xFF253F70,true));

        addModeCard(page,0);
        addModeCard(page,1);
        addModeCard(page,2);
        addModeCard(page,3);

        TextView mix=button("🎲 TASODIFIY MAVZU");
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(55));
        mp.setMargins(0,dp(12),0,dp(15));
        page.addView(mix,mp);
        mix.setOnClickListener(v -> startMode(random.nextInt(4)));

        page.addView(text("100 BOSQICH  •  OFFLINE O‘YIN",13,0xFF344F86,true));
        page.addView(text("❤️ ONAJON, SIZNI YAXSHI KO‘RAMAN!",15,0xFFC64B88,true));

        scroll.addView(page);
        setContentView(scroll);
    }

    void startMode(int m) {
        mode=m;
        level=getPreferences(0).getInt("level_"+mode,1);
        if(level<1 || level>100) level=1;
        startGame();
    }

    void startGame() {
        score=0;
        tray.clear();
        tiles.clear();
        lastUndo=null;
        gameOver=false;

        ArrayList<Integer> deck=new ArrayList<>();
        for(int i=0;i<9;i++)
            for(int j=0;j<9;j++)
                deck.add(i);
        Collections.shuffle(deck,random);

        ArrayList<int[]> positions=new ArrayList<>();

        // Pastki qatlam: 7 x 7
        for(int r=0;r<7;r++)
            for(int c=0;c<7;c++)
                positions.add(new int[]{r,c,0});

        // O‘rta qatlam: 5 x 5
        for(int r=1;r<=5;r++)
            for(int c=1;c<=5;c++)
                positions.add(new int[]{r,c,1});

        // Yuqori qatlam: 7 ta plitka
        for(int c=0;c<7;c++)
            positions.add(new int[]{3,c,2});

        for(int i=0;i<positions.size();i++) {
            int[] p=positions.get(i);
            tiles.add(new Tile(i,p[0],p[1],p[2],deck.get(i)));
        }

        showGame();
    }

    void showGame() {
        scroll=new ScrollView(this);
        root=vertical();
        root.setPadding(dp(12),dp(12),dp(12),dp(20));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF3D87CB,0xFF8DC5EB,0xFFEAF4FA}
        ));

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView home=button("⌂");
        TextView title=text("ONAJONIMGA",21,Color.WHITE,true);

        top.addView(home, new LinearLayout.LayoutParams(dp(55),dp(48)));
        top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(top);

        home.setOnClickListener(v -> showHome());

        LinearLayout stats=new LinearLayout(this);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0,dp(8),0,dp(8));

        coinView=text("🪙 "+coins,17,Color.WHITE,true);
        levelView=text("LEVEL "+level+"/100",17,Color.WHITE,true);
        scoreView=text("⭐ "+score,17,Color.WHITE,true);

        stats.addView(coinView,new LinearLayout.LayoutParams(0,dp(55),1));
        stats.addView(levelView,new LinearLayout.LayoutParams(0,dp(55),1));
        stats.addView(scoreView,new LinearLayout.LayoutParams(0,dp(55),1));
        root.addView(stats);

        root.addView(text(
            icons[mode]+"  "+names[mode],19,Color.WHITE,true
        ));

        root.addView(text("UCHTA BIR XIL P L I T K A N I  YIG‘ING",12,Color.WHITE,true));

        LinearLayout trayBox=vertical();
        trayBox.setPadding(dp(5),dp(5),dp(5),dp(5));
        trayBox.setBackground(shape(0xFF263F70,17,0xFF8ABCF2));

        trayRow=new LinearLayout(this);
        trayRow.setGravity(Gravity.CENTER);
        trayBox.addView(trayRow);
        root.addView(trayBox,new LinearLayout.LayoutParams(-1,dp(70)));

        remainingView=text("",13,Color.WHITE,true);
        root.addView(remainingView);

        boardFrame=new FrameLayout(this);
        boardFrame.setClipChildren(false);
        boardFrame.setBackground(shape(0x55304F83,18,0xFFB9D9F6));
        boardFrame.setElevation(dp(5));

        int screen=getResources().getDisplayMetrics().widthPixels;
        int tileW=(screen-dp(52))/7;
        int tileH=dp(47);

        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(
            tileW*7+dp(10),tileH*7+dp(10),Gravity.CENTER
        );

        root.addView(boardFrame,bp);
        root.addView(text("Plitkalarni tanlang va 3 tasini birlashtiring",13,Color.WHITE,true));

        LinearLayout actions=new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0,dp(12),0,dp(12));

        TextView undo=button("↶\nQAYTAR");
        TextView hint=button("💡\nMASLAHAT");
        TextView shuffle=button("🔀\nARALASHTIR");

        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(65),1);
        ap.setMargins(dp(3),0,dp(3),0);

        actions.addView(undo,ap);
        actions.addView(hint,ap);
        actions.addView(shuffle,ap);
        root.addView(actions);

        undo.setOnClickListener(v -> undoMove());
        hint.setOnClickListener(v -> giveHint());
        shuffle.setOnClickListener(v -> shuffleTiles());

        root.addView(text("❤️ ONAJONIMGA MEHR BILAN ❤️",14,0xFFB52F75,true));

        scroll.addView(root);
        setContentView(scroll);
        updateGame();
    }

    boolean isFree(Tile t) {
        for(Tile other:tiles) {
            if(!other.removed &&
               other.layer>t.layer &&
               other.row==t.row &&
               other.col==t.col)
                return false;
        }
        return true;
    }

    void renderBoard() {
        boardFrame.removeAllViews();

        int screen=getResources().getDisplayMetrics().widthPixels;
        int tileW=(screen-dp(52))/7;
        int tileH=dp(47);

        for(Tile tile:tiles) {
            if(tile.removed) continue;

            TextView view=text(items[mode][tile.type],25,Color.BLACK,false);
            view.setBackground(shape(
                0xFFFFFFFF,10,
                tile.layer==2 ? 0xFFFFD77A : 0xFFB9CBE2
            ));
            view.setElevation(dp(2+tile.layer*3));

            FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(
                tileW-dp(3),tileH-dp(3)
            );
            p.leftMargin=tile.col*tileW+dp(tile.layer*2);
            p.topMargin=tile.row*tileH+dp(tile.layer*2);

            boardFrame.addView(view,p);

            if(isFree(tile)) {
                view.setOnClickListener(v -> pickTile(tile));
            } else {
                view.setAlpha(0.88f);
            }
        }
    }

    void renderTray() {
        trayRow.removeAllViews();

        for(int i=0;i<7;i++) {
            TextView slot;
            if(i<tray.size())
                slot=text(items[mode][tray.get(i)],23,Color.BLACK,false);
            else
                slot=text("",20,Color.WHITE,false);

            slot.setBackground(shape(
                0xFF172F5A,9,0xFF5277AA
            ));

            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                0,dp(53),1
            );
            p.setMargins(dp(2),0,dp(2),0);
            trayRow.addView(slot,p);
        }
    }

    int remaining() {
        int n=0;
        for(Tile t:tiles) if(!t.removed) n++;
        return n;
    }

    void updateGame() {
        renderBoard();
        renderTray();

        scoreView.setText("⭐ "+score);
        levelView.setText("LEVEL "+level+"/100");
        coinView.setText("🪙 "+coins);
        remainingView.setText("Qolgan plitkalar: "+remaining());
    }

    int countType(int type) {
        int n=0;
        for(int v:tray) if(v==type) n++;
        return n;
    }

    void pickTile(Tile tile) {
        if(gameOver || tile.removed || !isFree(tile)) return;

        tile.removed=true;
        tray.add(tile.type);
        lastUndo=tile;

        if(countType(tile.type)>=3) {
            int removed=0;
            for(int i=tray.size()-1;i>=0 && removed<3;i--) {
                if(tray.get(i)==tile.type) {
                    tray.remove(i);
                    removed++;
                }
            }

            score+=30;
            coins+=5;
            lastUndo=null;

            getPreferences(0).edit()
                .putInt("coins",coins).apply();

            Toast.makeText(this,
                "✨ Ajoyib! +30 ochko, +5 tanga",
                Toast.LENGTH_SHORT).show();
        }

        updateGame();

        if(tray.size()>=7) {
            loseGame();
            return;
        }

        if(remaining()==0) {
            if(tray.isEmpty()) winGame();
            else loseGame();
        }
    }

    void undoMove() {
        if(gameOver) return;

        if(lastUndo==null || !lastUndo.removed) {
            Toast.makeText(this,"Qaytarish uchun yurish yo‘q",Toast.LENGTH_SHORT).show();
            return;
        }

        for(int i=tray.size()-1;i>=0;i--) {
            if(tray.get(i)==lastUndo.type) {
                tray.remove(i);
                break;
            }
        }

        lastUndo.removed=false;
        lastUndo=null;
        updateGame();
    }

    void giveHint() {
        if(gameOver) return;

        for(Tile t:tiles) {
            if(!t.removed && isFree(t) && countType(t.type)>0) {
                Toast.makeText(this,
                    "Maslahat: "+items[mode][t.type]+" plitkasini tanlang",
                    Toast.LENGTH_SHORT).show();
                return;
            }
        }

        for(Tile t:tiles) {
            if(!t.removed && isFree(t)) {
                Toast.makeText(this,
                    "Ochiq plitka: "+items[mode][t.type],
                    Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    void shuffleTiles() {
        if(gameOver) return;

        ArrayList<Integer> deck=new ArrayList<>();
        for(Tile t:tiles)
            if(!t.removed) deck.add(t.type);

        Collections.shuffle(deck,random);

        int i=0;
        for(Tile t:tiles)
            if(!t.removed) t.type=deck.get(i++);

        lastUndo=null;
        updateGame();

        Toast.makeText(this,"🔀 Plitkalar aralashtirildi",Toast.LENGTH_SHORT).show();
    }

    void winGame() {
        if(gameOver) return;
        gameOver=true;

        if(level<100) {
            level++;
            getPreferences(0).edit()
                .putInt("level_"+mode,level).apply();

            new AlertDialog.Builder(this)
                .setTitle("🏆 AJOYIB!")
                .setMessage("Barcha plitkalar yig‘ildi!\n⭐ Ochko: "+score+
                    "\nKeyingi bosqich: "+level)
                .setCancelable(false)
                .setPositiveButton("KEYINGI BOSQICH",(d,w)->startGame())
                .show();
        } else {
            getPreferences(0).edit().putInt("level_"+mode,1).apply();

            new AlertDialog.Builder(this)
                .setTitle("👑 100-LEVEL TUGADI!")
                .setMessage("Siz barcha bosqichlarni tugatdingiz!")
                .setPositiveButton("YANGI O‘YIN",(d,w)->{
                    level=1;
                    startGame();
                })
                .show();
        }
    }

    void loseGame() {
        if(gameOver) return;
        gameOver=true;

        new AlertDialog.Builder(this)
            .setTitle("🌷 O‘yin tugadi")
            .setMessage("Yig‘ish joylari to‘lib qoldi.\nYana urinib ko‘rasizmi?")
            .setCancelable(false)
            .setNegativeButton("MENYU",(d,w)->showHome())
            .setPositiveButton("QAYTA O‘YNASH",(d,w)->startGame())
            .show();
    }

    @Override
    public void onBackPressed() {
        if(scroll!=null) showHome();
        else super.onBackPressed();
    }
}
