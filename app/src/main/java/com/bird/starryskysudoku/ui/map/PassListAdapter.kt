package com.bird.starryskysudoku.ui.map

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.media.PlayMusic

class PassListAdapter(
    private val passList: List<Array<MapEntity?>>,
    private var lightStar: Int
) : RecyclerView.Adapter<PassListAdapter.LinearViewHolder>() {

    interface OpenPlayPage {
        fun onOpen(num: String)
    }

    private var openListener: OpenPlayPage? = null
    private lateinit var context: Context
    private lateinit var dialog: MyDialog
    private lateinit var passNumView: TextView
    private lateinit var passTimesView: TextView
    private lateinit var passStarView: ImageView

    fun setOpenListener(listener: OpenPlayPage) { openListener = listener }

    class LinearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stars = arrayOf<ImageView>(
            itemView.findViewById(R.id.star_1), itemView.findViewById(R.id.star_2),
            itemView.findViewById(R.id.star_3), itemView.findViewById(R.id.star_4)
        )
        val lines = arrayOf<ImageView>(
            itemView.findViewById(R.id.line_0), itemView.findViewById(R.id.line_1),
            itemView.findViewById(R.id.line_2), itemView.findViewById(R.id.line_3),
            itemView.findViewById(R.id.line_4)
        )
        val lights = arrayOf<ImageView>(
            itemView.findViewById(R.id.light_1), itemView.findViewById(R.id.light_2),
            itemView.findViewById(R.id.light_3), itemView.findViewById(R.id.light_4)
        )
        val nums = arrayOf<TextView>(
            itemView.findViewById(R.id.num_1), itemView.findViewById(R.id.num_2),
            itemView.findViewById(R.id.num_3), itemView.findViewById(R.id.num_4)
        )
        val status = arrayOfNulls<String>(4)
    }

    override fun getItemViewType(position: Int) = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinearViewHolder {
        context = parent.context
        return if (viewType == 0) {
            LinearViewHolder(LayoutInflater.from(context).inflate(R.layout.pass_first_item, parent, false))
        } else {
            dialog = MyDialogManager.getInstance().initView(context, R.layout.dialog_passcheck)
            passNumView = dialog.findViewById(R.id.passcheck_num)
            passTimesView = dialog.findViewById(R.id.passcheck_passtimes)
            passStarView = dialog.findViewById(R.id.passcheck_star)

            dialog.findViewById<View>(R.id.passcheck_close).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    MyDialogManager.getInstance().hide(dialog)
                }, 200)
            }

            val passNumText: TextView = dialog.findViewById(R.id.passcheck_num)
            dialog.findViewById<View>(R.id.passcheck_start).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    openListener?.onOpen(passNumText.text.toString())
                    MyDialogManager.getInstance().hide(dialog)
                }, 165)
            }

            LinearViewHolder(LayoutInflater.from(context).inflate(R.layout.pass_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: LinearViewHolder, position: Int) {
        if (position == 0) return

        val item = passList[passList.size - position]

        for (i in 0 until 4) {
            val entity = item[i] ?: continue
            val idx = i
            holder.nums[i].text = entity.passNum.toString()
            holder.status[i] = entity.status
            holder.stars[i].setOnClickListener(null)
            holder.stars[i].clearAnimation()
            holder.lights[i].clearAnimation()
            holder.lights[i].visibility = View.GONE
            holder.lights[i].alpha = 1f
            holder.lines[i].visibility = View.VISIBLE

            when (entity.status) {
                "已通关" -> {
                    holder.nums[i].setTextColor(context.resources.getColor(R.color.map_pass))
                    holder.stars[i].setImageResource(R.drawable.ic_map_star_small_on)

                    if (entity.passNum == lightStar) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            PlayMusic.getInstance().playMapLightStar()
                            animateStar(holder.stars[idx])
                        }, 180)
                    }

                    holder.stars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        passStarView.setImageResource(R.drawable.ic_pop_star)
                        passNumView.text = holder.nums[idx].text
                        passTimesView.text = entity.playTime
                        MyDialogManager.getInstance().show(dialog)
                    }

                    holder.lines[i].setImageResource(R.drawable.ic_map_line_left_on)
                    if (i == 2) holder.lines[i].setImageResource(R.drawable.ic_map_line_right_on)
                    if (i == 3) {
                        holder.lines[i].setImageResource(R.drawable.ic_map_line_right_on)
                        holder.lines[4].setImageResource(R.drawable.ic_map_line_left_on)
                    }
                }
                "待通关" -> {
                    holder.lights[i].visibility = View.VISIBLE
                    ValueAnimator.ofFloat(0.1f, 1f).apply {
                        duration = 1500
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        addUpdateListener { holder.lights[idx].alpha = it.animatedValue as Float }
                        start()
                    }
                    holder.lines[i].setImageResource(R.drawable.ic_map_line_left_on)
                    if (i == 0) holder.lines[0].setImageResource(R.drawable.ic_map_line_left_on)
                    if (i == 2) holder.lines[i].setImageResource(R.drawable.ic_map_line_right_on)
                    if (i == 3) holder.lines[i].setImageResource(R.drawable.ic_map_line_right_on)

                    holder.stars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        passStarView.setImageResource(R.drawable.ic_pop_star_bg)
                        passNumView.text = holder.nums[idx].text
                        passTimesView.text = entity.playTime
                        MyDialogManager.getInstance().show(dialog)
                    }
                }
                else -> {
                    holder.nums[i].setTextColor(Color.parseColor("#E7E8E9"))
                    holder.stars[i].setImageResource(R.drawable.ic_map_star_small_off)
                    holder.lines[i].setImageResource(R.drawable.ic_map_line_left_off)
                    if (i == 2 || i == 3) holder.lines[i].setImageResource(R.drawable.ic_map_line_right_off)
                }
            }

            if (holder.nums[i].text.toString() == "1") {
                holder.lines[0].visibility = View.GONE
            }
        }
    }

    private fun animateStar(star: ImageView) {
        star.scaleX = 0f; star.scaleY = 0f
        ObjectAnimator.ofFloat(star, "scaleY", 0f, 1.2f, 1f).setDuration(400).start()
        ObjectAnimator.ofFloat(star, "scaleX", 0f, 1.2f, 1f).setDuration(400).start()
        ObjectAnimator.ofFloat(star, "alpha", 0f, 1f).setDuration(400).start()
    }

    override fun getItemCount() = passList.size + 1

    fun getPosition(): Int {
        for (i in passList.indices) {
            for (j in 0 until 4) {
                if (passList[i][j]?.status == "待通关") return passList.size - i
            }
        }
        return 0
    }
}
