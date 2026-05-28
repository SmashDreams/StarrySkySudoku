package com.bird.starryskysudoku.ui.map

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import com.bird.starryskysudoku.data.repository.PassStatus
import com.bird.starryskysudoku.databinding.PassFirstItemBinding
import com.bird.starryskysudoku.databinding.PassItemBinding
import com.bird.starryskysudoku.media.PlayMusic
import java.util.Locale

class PassListAdapter(
    private val mPassList: List<Array<MapEntity?>>,
    private var mLightStar: Int
) : RecyclerView.Adapter<PassListAdapter.LinearViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PASS_ROW = 1
    }

    interface OpenPlayPage {
        fun onOpen(entity: MapEntity)
    }

    private var mOpenListener: OpenPlayPage? = null

    fun setOpenListener(listener: OpenPlayPage) { mOpenListener = listener }

    class LinearViewHolder private constructor(
        itemView: View,
        val mStars: Array<ImageView>,
        val mLines: Array<ImageView>,
        val mLights: Array<ImageView>,
        val mNums: Array<TextView>
    ) : RecyclerView.ViewHolder(itemView) {
        companion object {
            fun fromHeader(binding: PassFirstItemBinding): LinearViewHolder {
                return LinearViewHolder(binding.root, emptyArray(), emptyArray(), emptyArray(), emptyArray())
            }

            fun fromPassRow(binding: PassItemBinding): LinearViewHolder {
                return LinearViewHolder(
                    binding.root,
                    arrayOf(binding.star1, binding.star2, binding.star3, binding.star4),
                    arrayOf(binding.line0, binding.line1, binding.line2, binding.line3, binding.line4),
                    arrayOf(binding.light1, binding.light2, binding.light3, binding.light4),
                    arrayOf(binding.num1, binding.num2, binding.num3, binding.num4)
                )
            }
        }

        val mStatus = arrayOfNulls<String>(4)
        private val mLightAnimators = arrayOfNulls<ValueAnimator>(4)
        private val mLightStarRunnables = arrayOfNulls<Runnable>(4)

        fun setLightAnimator(index: Int, animator: ValueAnimator) {
            mLightAnimators[index]?.cancel()
            mLightAnimators[index] = animator
        }

        fun setLightStarRunnable(index: Int, runnable: Runnable) {
            mLightStarRunnables[index]?.let(itemView::removeCallbacks)
            mLightStarRunnables[index] = runnable
            itemView.postDelayed(runnable, 180)
        }

        fun cancelPendingAnimations() {
            for (i in mLightAnimators.indices) {
                mLightAnimators[i]?.cancel()
                mLightAnimators[i] = null
                mLightStarRunnables[i]?.let(itemView::removeCallbacks)
                mLightStarRunnables[i] = null
                mLights.getOrNull(i)?.clearAnimation()
                mLights.getOrNull(i)?.visibility = View.GONE
                mLights.getOrNull(i)?.alpha = 1f
                mStars.getOrNull(i)?.clearAnimation()
                mStars.getOrNull(i)?.animate()?.cancel()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_PASS_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinearViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            LinearViewHolder.fromHeader(PassFirstItemBinding.inflate(inflater, parent, false))
        } else {
            LinearViewHolder.fromPassRow(PassItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: LinearViewHolder, position: Int) {
        if (position == 0) return

        holder.cancelPendingAnimations()
        val context = holder.itemView.context
        val item = mPassList[mPassList.size - position]

        for (i in 0 until 4) {
            val entity = item[i] ?: continue
            val idx = i
            holder.mNums[i].text = String.format(Locale.getDefault(), "%d", entity.mPassNum)
            holder.mStatus[i] = entity.mStatus
            holder.mStars[i].setOnClickListener(null)
            holder.mLines[i].visibility = View.VISIBLE

            when (entity.mStatus) {
                PassStatus.COMPLETED -> {
                    holder.mNums[i].setTextColor(ContextCompat.getColor(context, R.color.map_pass))
                    holder.mStars[i].setImageResource(R.drawable.map_star_completed)

                    if (entity.mPassNum == mLightStar) {
                        holder.setLightStarRunnable(idx, Runnable {
                            PlayMusic.getInstance().playMapLightStar()
                            animateStar(holder.mStars[idx])
                        })
                    }

                    holder.mStars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        mOpenListener?.onOpen(entity)
                    }

                    holder.mLines[i].setImageResource(R.drawable.map_path_left_unlocked)
                    if (i == 2) holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)
                    if (i == 3) {
                        holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)
                        holder.mLines[4].setImageResource(R.drawable.map_path_left_unlocked)
                    }
                }
                PassStatus.TODO -> {
                    holder.mLights[i].visibility = View.VISIBLE
                    ValueAnimator.ofFloat(0.1f, 1f).apply {
                        duration = 1500
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        addUpdateListener { holder.mLights[idx].alpha = it.animatedValue as Float }
                        start()
                        holder.setLightAnimator(idx, this)
                    }
                    holder.mLines[i].setImageResource(R.drawable.map_path_left_unlocked)
                    if (i == 0) holder.mLines[0].setImageResource(R.drawable.map_path_left_unlocked)
                    if (i == 2) holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)
                    if (i == 3) holder.mLines[i].setImageResource(R.drawable.map_path_right_unlocked)

                    holder.mStars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        mOpenListener?.onOpen(entity)
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

    override fun onViewRecycled(holder: LinearViewHolder) {
        holder.cancelPendingAnimations()
        super.onViewRecycled(holder)
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
                if (mPassList[i][j]?.mStatus == PassStatus.TODO) return mPassList.size - i
            }
        }
        return 0
    }
}
