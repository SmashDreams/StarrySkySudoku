package com.bird.starryskysudoku.ui.map

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import java.util.Locale

class PassListAdapter(
    private val mPassList: List<Array<MapEntity?>>,
    private var mLightStar: Int
) : RecyclerView.Adapter<PassListAdapter.LinearViewHolder>() {

    interface OpenPlayPage {
        fun onOpen(num: String)
    }

    private var mOpenListener: OpenPlayPage? = null
    private lateinit var mContext: Context
    private lateinit var mDialog: MyDialog
    private lateinit var mPassNumView: TextView
    private lateinit var mPassTimesView: TextView
    private lateinit var mPassStarView: ImageView

    fun setOpenListener(listener: OpenPlayPage) { mOpenListener = listener }

    class LinearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mStars = arrayOf<ImageView>(
            itemView.findViewById(R.id.star_1), itemView.findViewById(R.id.star_2),
            itemView.findViewById(R.id.star_3), itemView.findViewById(R.id.star_4)
        )
        val mLines = arrayOf<ImageView>(
            itemView.findViewById(R.id.line_0), itemView.findViewById(R.id.line_1),
            itemView.findViewById(R.id.line_2), itemView.findViewById(R.id.line_3),
            itemView.findViewById(R.id.line_4)
        )
        val mLights = arrayOf<ImageView>(
            itemView.findViewById(R.id.light_1), itemView.findViewById(R.id.light_2),
            itemView.findViewById(R.id.light_3), itemView.findViewById(R.id.light_4)
        )
        val mNums = arrayOf<TextView>(
            itemView.findViewById(R.id.num_1), itemView.findViewById(R.id.num_2),
            itemView.findViewById(R.id.num_3), itemView.findViewById(R.id.num_4)
        )
        val mStatus = arrayOfNulls<String>(4)
    }

    override fun getItemViewType(position: Int) = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinearViewHolder {
        mContext = parent.context
        return if (viewType == 0) {
            LinearViewHolder(LayoutInflater.from(mContext).inflate(R.layout.pass_first_item, parent, false))
        } else {
            mDialog = MyDialogManager.getInstance().initView(mContext, R.layout.dialog_passcheck)
            mPassNumView = mDialog.findViewById(R.id.passcheck_num)
            mPassTimesView = mDialog.findViewById(R.id.passcheck_passtimes)
            mPassStarView = mDialog.findViewById(R.id.passcheck_star)

            mDialog.findViewById<View>(R.id.passcheck_close).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    MyDialogManager.getInstance().hide(mDialog)
                }, 200)
            }

            mDialog.findViewById<View>(R.id.passcheck_start).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    mOpenListener?.onOpen(mPassNumView.text.toString())
                    MyDialogManager.getInstance().hide(mDialog)
                }, 165)
            }

            LinearViewHolder(LayoutInflater.from(mContext).inflate(R.layout.pass_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: LinearViewHolder, position: Int) {
        if (position == 0) return

        val item = mPassList[mPassList.size - position]

        for (i in 0 until 4) {
            val entity = item[i] ?: continue
            val idx = i
            holder.mNums[i].text = String.format(Locale.getDefault(), "%d", entity.mPassNum)
            holder.mStatus[i] = entity.mStatus
            holder.mStars[i].setOnClickListener(null)
            holder.mStars[i].clearAnimation()
            holder.mLights[i].clearAnimation()
            holder.mLights[i].visibility = View.GONE
            holder.mLights[i].alpha = 1f
            holder.mLines[i].visibility = View.VISIBLE

            when (entity.mStatus) {
                "已通关" -> {
                    holder.mNums[i].setTextColor(ContextCompat.getColor(mContext, R.color.map_pass))
                    holder.mStars[i].setImageResource(R.drawable.map_star_completed)

                    if (entity.mPassNum == mLightStar) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            PlayMusic.getInstance().playMapLightStar()
                            animateStar(holder.mStars[idx])
                        }, 180)
                    }

                    holder.mStars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        mPassStarView.setImageResource(R.drawable.star_earned)
                        mPassNumView.text = holder.mNums[idx].text
                        mPassTimesView.text = entity.mPlayTime
                        MyDialogManager.getInstance().show(mDialog)
                    }

                    holder.mLines[i].setImageResource(R.drawable.map_path_left_unlocked)
                    if (i == 2) holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)
                    if (i == 3) {
                        holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)
                        holder.mLines[4].setImageResource(R.drawable.map_path_left_unlocked)
                    }
                }
                "待通关" -> {
                    holder.mLights[i].visibility = View.VISIBLE
                    ValueAnimator.ofFloat(0.1f, 1f).apply {
                        duration = 1500
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        addUpdateListener { holder.mLights[idx].alpha = it.animatedValue as Float }
                        start()
                    }
                    holder.mLines[i].setImageResource(R.drawable.map_path_left_unlocked)
                    if (i == 0) holder.mLines[0].setImageResource(R.drawable.map_path_left_unlocked)
                    if (i == 2) holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)
                    if (i == 3) holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)

                    holder.mStars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        mPassStarView.setImageResource(R.drawable.star_empty)
                        mPassNumView.text = holder.mNums[idx].text
                        mPassTimesView.text = entity.mPlayTime
                        MyDialogManager.getInstance().show(mDialog)
                    }
                }
                else -> {
                    holder.mNums[i].setTextColor("#E7E8E9".toColorInt())
                    holder.mStars[i].setImageResource(R.drawable.map_star_locked)
                    holder.mLines[i].setImageResource(R.drawable.map_path_left_locked)
                    if (i == 2 || i == 3) holder.mLines[i].setImageResource(R.drawable.map_path_right_locked)
                }
            }

            if (holder.mNums[i].text.toString() == "1") {
                holder.mLines[0].visibility = View.GONE
            }
        }
    }

    private fun animateStar(star: ImageView) {
        star.scaleX = 0f; star.scaleY = 0f
        ObjectAnimator.ofFloat(star, "scaleY", 0f, 1.2f, 1f).setDuration(400).start()
        ObjectAnimator.ofFloat(star, "scaleX", 0f, 1.2f, 1f).setDuration(400).start()
        ObjectAnimator.ofFloat(star, "alpha", 0f, 1f).setDuration(400).start()
    }

    override fun getItemCount() = mPassList.size + 1

    fun getPosition(): Int {
        for (i in mPassList.indices) {
            for (j in 0 until 4) {
                if (mPassList[i][j]?.mStatus == "待通关") return mPassList.size - i
            }
        }
        return 0
    }
}
