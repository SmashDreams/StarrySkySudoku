package com.bird.starryskysudoku.ui.play

import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.databinding.ActivityPlayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LEVEL = 40
        const val EXTRA_USERNAME = PlayRoute.EXTRA_USERNAME
    }

    private lateinit var mBinding: ActivityPlayBinding
    private lateinit var mViewModel: PlayViewModel
    private lateinit var mBroadView: BroadView
    private lateinit var mGameResultRecorder: GameResultRecorder
    private lateinit var mDialogController: PlayDialogController
    private lateinit var mCountdownCoordinator: CountdownCoordinator
    private lateinit var mBoardController: PlayBoardController
    private lateinit var mInputController: PlayInputController
    private lateinit var mGameStateController: PlayGameStateController
    private lateinit var mNavigationController: PlayNavigationController

    /*
     * 游戏页面的主要控件引用集中在这里保存，便于统一管理倒计时、棋盘和输入按钮状态。
     */
    private lateinit var mPlayNum: TextView
    private lateinit var mTimeProgressBar: ProgressBar
    private lateinit var mRemainingMins: TextView
    private lateinit var mRemainingSecs: TextView
    private lateinit var mColon: TextView
    private val mNumbers = arrayOfNulls<TextView>(9)
    private lateinit var mRevoke: ImageView
    private lateinit var mTag: ImageView
    private lateinit var mPauseButton: ImageView

    /*
     * 页面状态只保存与界面交互强相关的数据；实际棋盘数据和倒计时状态由视图模型维护。
     */
    private var mIsPaused = false
    private var mNum = "1"
    private var mCurrentUsername = LauncherSessionReader.GUEST_USERNAME
    private var mTagData = Array(9) { arrayOfNulls<TagData>(9) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mNum = PlayRoute.readLevel(intent).toString()
        mCurrentUsername = PlayRoute.readUsername(intent)
            ?: LauncherSessionReader.readUsername(contentResolver)
        val maxNum = MAX_LEVEL

        val db = DatabaseInitializer.getDatabase(this)
        mViewModel = ViewModelProvider(this, PlayViewModelFactory(db))[PlayViewModel::class.java]
        mGameResultRecorder = GameResultRecorder(contentResolver)
        mCountdownCoordinator = CountdownCoordinator(
            mActivity = this,
            mGetRemainingSeconds = { mViewModel.getRemainingSeconds() },
            mGetLevel = { parseLevel(mNum) },
            mCanStart = {
                mViewModel.mHasWon.value != true && mViewModel.mTimerFinished.value != true
            },
            mOnTick = { remainingSeconds -> mViewModel.updateRemainingSeconds(remainingSeconds) }
        )
        mDialogController = PlayDialogController(
            mActivity = this,
            mMaxLevel = maxNum,
            mGetLevel = { parseLevel(mNum) },
            mGetUsername = { mCurrentUsername },
            mRunAfterClearingHistory = { action -> clearHistoryAndRun(action) },
            mSetPaused = { paused -> mIsPaused = paused },
            mStartCountdownService = { mCountdownCoordinator.start() }
        )

        /*
         * 进入页面后一次性绑定控件，后续只更新控件内容和可用状态。
         */
        mPlayNum = mBinding.playNum; mPlayNum.text = mNum
        mTimeProgressBar = mBinding.playTimeProgressbar
        mRemainingMins = mBinding.playTimeMin
        mRemainingSecs = mBinding.playTimeSec
        mColon = mBinding.textview74
        mBroadView = mBinding.playBroad
        mNumbers[0] = mBinding.play1; mNumbers[1] = mBinding.play2
        mNumbers[2] = mBinding.play3; mNumbers[3] = mBinding.play4
        mNumbers[4] = mBinding.play5; mNumbers[5] = mBinding.play6
        mNumbers[6] = mBinding.play7; mNumbers[7] = mBinding.play8
        mNumbers[8] = mBinding.play9
        mRevoke = mBinding.playRevoke; mTag = mBinding.playTag
        mPauseButton = mBinding.playPause
        mBoardController = PlayBoardController(
            mLifecycleOwner = this,
            mViewModel = mViewModel,
            mBroadView = mBroadView,
            mNumbers = mNumbers,
            mTag = mTag,
            mTagData = mTagData,
            mGetLevel = { parseLevel(mNum) }
        )
        mInputController = PlayInputController(
            mScope = lifecycleScope,
            mViewModel = mViewModel,
            mBroadView = mBroadView,
            mNumbers = mNumbers,
            mRevoke = mRevoke,
            mTag = mTag,
            mTagData = mTagData,
            mGetLevel = { parseLevel(mNum) },
            mGetUsername = { mCurrentUsername },
            mOnPuzzleCompleted = { level ->
                saveAndQueryGameResultThroughProvider(level, completed = true)
            }
        )
        mGameStateController = PlayGameStateController(
            mContext = this,
            mLifecycleOwner = this,
            mScope = lifecycleScope,
            mViewModel = mViewModel,
            mBroadView = mBroadView,
            mTimeProgressBar = mTimeProgressBar,
            mRemainingMins = mRemainingMins,
            mRemainingSecs = mRemainingSecs,
            mColon = mColon,
            mDialogController = mDialogController,
            mCountdownCoordinator = mCountdownCoordinator,
            mIsPaused = { mIsPaused },
            mGetLevel = { parseLevel(mNum) },
            mSaveGameResult = { level, completed ->
                saveAndQueryGameResultThroughProvider(level, completed)
            }
        )
        mNavigationController = PlayNavigationController(
            mActivity = this,
            mViewModel = mViewModel,
            mPauseButton = mPauseButton,
            mDialogController = mDialogController,
            mCountdownCoordinator = mCountdownCoordinator,
            mSetPaused = { paused -> mIsPaused = paused },
            mIsPaused = { mIsPaused }
        )

        mBoardController.init()
        mGameStateController.init()
        mInputController.init()
        mNavigationController.init()
    }

    override fun onStart() {
        super.onStart()
        mCountdownCoordinator.onStart()
    }

    override fun onPause() {
        super.onPause()
        mNavigationController.onPause()
    }

    override fun onStop() {
        mCountdownCoordinator.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        mNavigationController.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mGameStateController.clearCallbacks()
        mCountdownCoordinator.stop()
        mDialogController.hideAll()
    }

    private suspend fun saveAndQueryGameResultThroughProvider(levelNum: Int, completed: Boolean) {
        if (!mViewModel.markGameResultRecordStarted(levelNum, completed)) return
        val saved = withContext(Dispatchers.IO) {
            mGameResultRecorder.saveAndVerify(
                level = levelNum,
                remainingSeconds = mViewModel.getRemainingSeconds(),
                completed = completed,
                username = mCurrentUsername
            )
        }
        if (!saved) {
            mViewModel.clearGameResultRecordMark(levelNum, completed)
        }
    }

    private fun parseLevel(raw: String?): Int {
        return raw?.toIntOrNull()?.takeIf { it in 1..MAX_LEVEL } ?: 1
    }

    private fun clearHistoryAndRun(action: () -> Unit) {
        lifecycleScope.launch {
            mViewModel.clearHistory()
            action()
        }
    }
}
