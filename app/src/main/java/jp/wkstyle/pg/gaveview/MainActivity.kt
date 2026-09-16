package jp.wkstyle.pg.gaveview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 通常のレイアウトファイル(XML)の代わりに、
        // 自分で作った「GameView」を全画面に表示します
        val gameView = GameView(this)
        setContentView(gameView)
    }
}

/**
 * 自分で作る専用の画用紙（Custom View）
 */
// context（コンテキスト）：
// この画面を表示している「アプリの環境・状況」をまとめた情報です。
// 「いま画面の大きさはどれくらい？」「画像や音のデータはどこにある？」など、
// スマホ本体の機能やデータを使いたいときに頼る「案内窓口」のようなものです。
class GameView(context: Context) : View(context) {

    // -------------------------------------------------------------
    // 1. キャラクターの状態（データ）を覚える変数
    // -------------------------------------------------------------
    // キャラクターの現在の位置 (X座標, Y座標)
    private var charaX = 300f
    private var charaY = 500f

    // キャラクターの目標位置（タッチされた場所）
    private var targetX = 300f
    private var targetY = 500f

    // キャラクターの大きさ（半径）
    private val charaRadius = 60f

    // キャラクターの移動スピード（大きいほど早く動く）
    private val speed = 12f

    // タッチしたときのエフェクト（輪っかの波紋）用
    private var rippleX = -100f
    private var rippleY = -100f
    private var rippleRadius = 0f
    private var rippleAlpha = 0

    // -------------------------------------------------------------
    // 2. お絵描き道具（Paint）の準備
    // -------------------------------------------------------------
    // キャラクターの体を描く筆
    private val bodyPaint = Paint().apply {
        color = "#38BDF8".toColorInt()
        isAntiAlias = true // 輪郭をなめらかにする
        style = Paint.Style.FILL
    }

    // キャラクターのほっぺを描く筆
    private val cheekPaint = Paint().apply {
        color = "#F472B6".toColorInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // 目や口を描く筆
    private val facePaint = Paint().apply {
        color = "#0F172A".toColorInt() // 濃いネイビー
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // 目のハイライト（白目）
    private val highlightPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // 背景のマス目を描く筆
    private val gridPaint = Paint().apply {
        color = "#E2E8F0".toColorInt() // 薄いグレー
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    // タッチ波紋用の筆
    private val ripplePaint = Paint().apply {
        color = "#38BDF8".toColorInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // 文字（案内用）を描く筆
    private val textPaint = Paint().apply {
        color = "#475569".toColorInt()
        textSize = 46f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // 座標表示用の筆
    private val subTextPaint = Paint().apply {
        color = "#94A3B8".toColorInt()
        textSize = 34f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // -------------------------------------------------------------
    // 3. 画面を描く魔法のメソッド onDraw
    // -------------------------------------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ① 背景色を塗る（やさしいオフホワイト）
        canvas.drawColor("#F8FAFC".toColorInt())

        // ② 背景にグリッド（方眼紙のようなマス目）を描く
        drawGrid(canvas)

        // ③ タッチされた波紋エフェクトを描く
        drawRipple(canvas)

        // ④ キャラクターを描く
        drawCharacter(canvas, charaX, charaY)

        // ⑤ 画面の上部に説明と現在の座標を表示する
        canvas.drawText("画面をタッチしてみてね！キャラが歩いてくるよ", width / 2f, 120f, textPaint)
        canvas.drawText("現在地: X = ${charaX.toInt()}, Y = ${charaY.toInt()}", width / 2f, 180f, subTextPaint)

        // ---------------------------------------------------------
        // 4. キャラクターの位置を更新して次のコマへ！
        // ---------------------------------------------------------
        updateCharacterPosition()

        // 「もう一度すぐに描き直して！」とAndroidに依頼（約60FPSのアニメーションループ）
        postInvalidateOnAnimation()
    }

    /**
     * 背景のグリッド（マス目）を描く関数
     */
    private fun drawGrid(canvas: Canvas) {
        val step = 100f
        // 縦線
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        // 横線
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }

    /**
     * かわいいキャラクターを描画する関数
     */
    private fun drawCharacter(canvas: Canvas, x: Float, y: Float) {
        // --- 1. からだ（丸） ---
        canvas.drawCircle(x, y, charaRadius, bodyPaint)

        // --- 2. つの／アンテナ ---
        val antennaPaint = Paint(bodyPaint).apply {
            strokeWidth = 8f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(x, y - charaRadius, x, y - charaRadius - 25f, antennaPaint)
        canvas.drawCircle(x, y - charaRadius - 25f, 10f, cheekPaint)

        // --- 3. ほっぺ（左右） ---
        canvas.drawCircle(x - 32f, y + 10f, 12f, cheekPaint)
        canvas.drawCircle(x + 32f, y + 10f, 12f, cheekPaint)

        // --- 4. 目（黒目とハイライト） ---
        // 左目
        canvas.drawCircle(x - 20f, y - 8f, 10f, facePaint)
        canvas.drawCircle(x - 17f, y - 12f, 4f, highlightPaint)

        // 右目
        canvas.drawCircle(x + 20f, y - 8f, 10f, facePaint)
        canvas.drawCircle(x + 23f, y - 12f, 4f, highlightPaint)

        // --- 5. にっこり口 ---
        val mouthRect = RectF(x - 14f, y - 4f, x + 14f, y + 18f)
        val mouthPaint = Paint(facePaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        // 0度〜180度のお椀型の円弧を描く
        canvas.drawArc(mouthRect, 0f, 180f, false, mouthPaint)
    }

    /**
     * タッチした場所に広がる波紋を描く関数
     */
    private fun drawRipple(canvas: Canvas) {
        if (rippleAlpha > 0) {
            ripplePaint.alpha = rippleAlpha
            canvas.drawCircle(rippleX, rippleY, rippleRadius, ripplePaint)

            // 波紋を大きくしながら徐々に消していく
            rippleRadius += 4f
            rippleAlpha = (rippleAlpha - 8).coerceAtLeast(0)
        }
    }

    /**
     * キャラクターを目標地点に向かってスムーズに移動させる計算
     */
    private fun updateCharacterPosition() {
        val dx = targetX - charaX
        val dy = targetY - charaY
        val distance = hypot(dx, dy)

        // 目標地点にまだ届いていない場合、少しずつ近づく
        if (distance > speed) {
            // スピードの分だけ目標の方向へ進める
            charaX += (dx / distance) * speed
            charaY += (dy / distance) * speed
        } else {
            // 到着したらピッタリその位置にする
            charaX = targetX
            charaY = targetY
        }
    }

    // -------------------------------------------------------------
    // 5. 画面のタッチを検出する onTouchEvent
    // -------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            // 指が触れた瞬間、または指を画面上でスライドさせたとき
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // タッチされた座標を新しい移動目標にする！
                targetX = event.x
                targetY = event.y

                // タッチした場所にエフェクトを発生させる
                if (event.action == MotionEvent.ACTION_DOWN) {
                    rippleX = event.x
                    rippleY = event.y
                    rippleRadius = 15f
                    rippleAlpha = 220
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}