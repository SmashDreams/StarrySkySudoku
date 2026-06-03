package com.bird.starryskysudoku.ui.map

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
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
        private const val LEVEL_VERTICAL_STEP_DP = 84
    }

    interface OpenPlayPage {
        fun onOpen(entity: MapEntity)
    }

    private var mOpenListener: OpenPlayPage? = null

    fun setOpenListener(listener: OpenPlayPage) { mOpenListener = listener }

    class LinearViewHolder private constructor(
        itemView: View,
        val mStars: Array<ImageView>,
        val mPathOverlay: MapPathOverlayView?,
        val mLights: Array<ImageView>,
        val mNums: Array<TextView>
    ) : RecyclerView.ViewHolder(itemView) {
        companion object {
            fun fromHeader(binding: PassFirstItemBinding): LinearViewHolder {
                return LinearViewHolder(binding.root, emptyArray(), null, emptyArray(), emptyArray())
            }

            fun fromPassRow(binding: PassItemBinding): LinearViewHolder {
                return LinearViewHolder(
                    binding.root,
                    arrayOf(binding.star1, binding.star2, binding.star3, binding.star4),
                    binding.pathOverlay,
                    arrayOf(binding.light1, binding.light2, binding.light3, binding.light4),
                    arrayOf(binding.num1, binding.num2, binding.num3, binding.num4)
                )
            }
        }

        val mStatus = arrayOfNulls<String>(4)
        // 每一行的星星光效都独立管理，回收时统一取消，避免旧动画串到新行。
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
        // 列表头部固定在最上方，数据行按视觉顺序需要从尾部倒着取。
        val rowIndex = mPassList.size - position
        val item = mPassList[rowIndex]
        val previousRowBelow = mPassList.getOrNull(rowIndex - 1)
        val hasNextRowAbove = rowIndex < mPassList.lastIndex
        holder.mPathOverlay?.bind(holder.mStars, item, previousRowBelow, hasNextRowAbove)

        for (i in 0 until 4) {
            val entity = item[i] ?: continue
            val idx = i
            holder.mNums[i].text = String.format(Locale.getDefault(), "%d", entity.mPassNum)
            holder.mStatus[i] = entity.mStatus
            holder.mStars[i].setOnClickListener(null)

            when (entity.mStatus) {
                PassStatus.COMPLETED -> {
                    holder.mNums[i].setTextColor(ContextCompat.getColor(context, R.color.map_pass))
                    holder.mStars[i].setImageResource(R.drawable.map_star_completed)

                    if (entity.mPassNum == mLightStar) {
                        // 新拿到的星星延迟一点再点亮，让滚动和音效更有层次感。
                        holder.setLightStarRunnable(idx, Runnable {
                            PlayMusic.getInstance().playMapLightStar()
                            animateStar(holder.mStars[idx])
                        })
                    }

                    holder.mStars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        mOpenListener?.onOpen(entity)
                    }
                }
                PassStatus.TODO -> {
                    holder.mLights[i].visibility = View.VISIBLE
                    // 当前可挑战关卡持续做呼吸灯效果，提示玩家下一步入口。
                    ValueAnimator.ofFloat(0.1f, 1f).apply {
                        duration = 1500
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        interpolator = DecelerateInterpolator()
                        addUpdateListener { holder.mLights[idx].alpha = it.animatedValue as Float }
                        start()
                        holder.setLightAnimator(idx, this)
                    }

                    holder.mStars[i].setOnClickListener {
                        PlayMusic.getInstance().playDialogShow()
                        mOpenListener?.onOpen(entity)
                    }
                }
                else -> {
                    holder.mNums[i].setTextColor("#E7E8E9".toColorInt())
                    holder.mStars[i].setImageResource(R.drawable.map_star_locked)
                }
            }
        }
    }

    override fun onViewRecycled(holder: LinearViewHolder) {
        holder.cancelPendingAnimations()
        super.onViewRecycled(holder)
    }

    private fun animateStar(star: ImageView) {
        star.scaleX = 0.8f
        star.scaleY = 0.8f
        star.alpha = 0f
        ObjectAnimator.ofFloat(star, "scaleY", 0.8f, 1.2f, 1f).setDuration(400L).start()
        ObjectAnimator.ofFloat(star, "scaleX", 0.8f, 1.2f, 1f).setDuration(400L).start()
        ObjectAnimator.ofFloat(star, "alpha", 0f, 1f).setDuration(200L).start()
    }

    override fun getItemCount() = mPassList.size + 1

    fun getPosition(): Int {
        // 默认滚动到第一个待通关关卡所在行，方便玩家继续当前进度。
        for (i in mPassList.indices) {
            for (j in 0 until 4) {
                if (mPassList[i][j]?.mStatus == PassStatus.TODO) return mPassList.size - i
            }
        }
        return 0
    }

    fun getPositionForLevel(level: Int): Int {
        // 胜利、失败和通知返回都可能指定具体关卡，这里按关卡号直接反查所在行。
        for (i in mPassList.indices) {
            for (j in 0 until 4) {
                if (mPassList[i][j]?.mPassNum == level) return mPassList.size - i
            }
        }
        return getPosition()
    }

    fun getTopOffsetDpForLevel(level: Int): Int {
        // 同一行四颗星在竖直方向错落排布，返回时需要按列号补上额外高度偏移。
        val indexInRow = (level - 1).floorMod(4)
        return (3 - indexInRow) * LEVEL_VERTICAL_STEP_DP
    }

    fun getCurrentTodoLevel(): Int? {
        // 当前待挑战关卡用于地图页首次定位和胜利返回后的默认落点。
        for (row in mPassList) {
            for (entity in row) {
                if (entity?.mStatus == PassStatus.TODO) return entity.mPassNum
            }
        }
        return null
    }

    fun getCurrentProgressOffsetDp(): Int {
        val completedLevel = getCurrentTodoLevel()?.minus(1) ?: getMaxCompletedLevel()
        return MapScrollPolicy.offsetDpAfterCompletedLevel(completedLevel)
    }

    private fun getMaxCompletedLevel(): Int {
        var maxCompletedLevel = 0
        for (row in mPassList) {
            for (entity in row) {
                if (entity?.mStatus == PassStatus.COMPLETED) {
                    maxCompletedLevel = maxOf(maxCompletedLevel, entity.mPassNum)
                }
            }
        }
        return maxCompletedLevel
    }

    private fun Int.floorMod(other: Int): Int {
        return ((this % other) + other) % other
    }

}
