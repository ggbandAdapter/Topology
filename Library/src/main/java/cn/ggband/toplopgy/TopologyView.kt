package cn.ggband.toplopgy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import cn.ggband.toplopgy.callback.NodeViewCallback
import kotlin.math.abs


/**
 * 拓扑图视图
 */
class TopologyView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val LOGTAG = "TopologyView"

    //节点水平间距
    private var nodeHSpace = 25
    //节点垂直间距
    private var nodeVSpace = 129

    private val moveLimit = 8f

    private var downX = 0f
    private var downY = 0f
    private var mLastX = 0f
    private var mLastY = 0f

    private var mPathPaint: Paint = Paint().apply {
        color = Color.parseColor("#FF0000")
        isAntiAlias = true
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private var lastInterceptX = 0f
    private var lastInterceptY = 0f


    private var mNodeViewCallback: NodeViewCallback? = null


    private val topologyList = ArrayList<TopologyEx>()


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var nodeChildView: View
        var childLeft = 0
        var childTop = 0
        var childRight = 0
        var childBottom = 0
        var sameFloorList: List<TopologyEx>
        var topologyEx: TopologyEx
        var lastFloor = 0
        var layoutWidth = 0
        for (i in 0 until childCount) {
            nodeChildView = getChildAt(i)
            topologyEx = topologyList[i]
            sameFloorList = topologyList.filter { it.floor == topologyEx.floor }
            val sameFloorIndex = sameFloorList.indexOf(topologyEx)
            Log.d(LOGTAG, "node on sameFloorList index:$sameFloorIndex")
            Log.d(LOGTAG, "lastFloor:$lastFloor;currFloor:${topologyEx.floor}")

            if (lastFloor != topologyEx.floor) {
                layoutWidth = 0
            }
            Log.d(LOGTAG, "layoutWidth:$layoutWidth")

            childLeft = layoutWidth
            childTop =
                topologyEx.floor * nodeChildView.measuredHeight + topologyEx.floor * nodeVSpace
            childRight = childLeft + nodeChildView.measuredWidth
            childBottom = childTop + nodeChildView.measuredHeight
            nodeChildView.layout(
                childLeft,
                childTop,
                childRight,
                childBottom
            )
            layoutWidth += nodeChildView.measuredWidth + nodeHSpace
            lastFloor = topologyEx.floor
        }
    }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val path = Path()
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            topologyList[i].topology.childList()?.forEach { iTopology ->
                val cChildIndex = topologyList.indexOfFirst { it.topology == iTopology }
                val cChildView = getChildAt(cChildIndex)
                path.moveTo(childView.x + childView.width / 2, childView.y + childView.height)
                path.lineTo(cChildView.x + cChildView.width / 2, cChildView.y)
                canvas.drawPath(path, mPathPaint)
            }
        }
    }


    /**
     * 更新
     */
    fun setTopology(topology: ITopology) {
        topologyList.clear()
        val root = TopologyEx().apply {
            floor = 0
            this.topology = topology
        }
        buildChild(root)
        val sortTopologyList = topologyList.sortedBy { it.floor }
        topologyList.clear()
        topologyList.addAll(sortTopologyList)
        topologyList.forEach {
            val nodeView = getNodeItemView(it.topology)
            mNodeViewCallback?.onNodeView(nodeView, it.topology)
            addView(nodeView)
            Log.d(LOGTAG, it.topology.toString() + "===floor:" + it.floor)
        }
    }

    private fun buildChild(root: TopologyEx) {
        topologyList.add(root)
        root.topology.childList()?.forEach { iTopology ->
            TopologyEx().apply {
                floor = root.floor + 1
                this.topology = iTopology
            }.run {
                buildChild(this)
            }
        }
    }


    private fun getNodeItemView(topology: ITopology): View {
        return LayoutInflater.from(context).inflate(topology.nodeLayoutId(), null)
    }

    fun setNodeViewCallback(callback: NodeViewCallback) {
        this.mNodeViewCallback = callback
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val childCount = this.childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    /**
     * 最大水平宽度
     */
    private fun getMaxFloorWidth(): Int {
        val maxTopologyChildSize = topologyList.maxBy {
            it.topology.childList()?.size ?: 0
        }?.topology?.childList()?.size ?: 0
        var maxChildViewWidth = 0
        for (i in 0 until childCount) {
            if (getChildAt(i).measuredWidth > maxChildViewWidth)
                maxChildViewWidth = getChildAt(i).measuredWidth
        }
        return maxTopologyChildSize * maxChildViewWidth + (maxTopologyChildSize - 1) * nodeVSpace
    }


    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        if (scrollX == 0) return ifIntercept
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastInterceptX = event.rawX
                lastInterceptY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                //检查是横向移动的距离大，还是纵向
                val xDistance: Float = Math.abs(lastInterceptX - event.rawX)
                val yDistance: Float = Math.abs(lastInterceptY - event.rawY)
                ifIntercept = xDistance > moveLimit || yDistance > moveLimit
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        return ifIntercept
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                mLastX = event.x
                mLastY = event.y
                Log.d(LOGTAG, "width:${width};height:${height}")

            }
            MotionEvent.ACTION_MOVE -> {
                Log.d(LOGTAG, "scrollX:${scrollX};scrollX:${scrollY}")
                val moveX = (event.x - mLastX).toInt()
                val moveY = (event.y - mLastY).toInt()
                Log.d(LOGTAG, "moveX:${moveX};moveY:${moveY}")

                if (canMove(event)) {


                    scrollBy(-moveX, -moveY) //滑动
                }
                mLastX = event.x
                mLastY = event.y
            }
            MotionEvent.ACTION_UP -> {


            }

            MotionEvent.ACTION_CANCEL -> {
            }
        }
        return true
    }

    private fun canMove(event: MotionEvent): Boolean {
        val moveX = event.x
        val moveY = event.y
        val xDistance: Float = abs(moveX - downX)
        val yDistance: Float = abs(moveY - downY)
        return xDistance > moveLimit || yDistance > moveLimit
    }


}