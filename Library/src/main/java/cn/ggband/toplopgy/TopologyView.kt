package cn.ggband.toplopgy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.ggband.toplopgy.callback.NodeViewCallback

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

    private var mPathPaint: Paint = Paint().apply {
        color = Color.parseColor("#FF0000")
        isAntiAlias = true
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }


    private var mNodeViewCallback: NodeViewCallback? = null


    private val topologyList = ArrayList<TopologyEx>()


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        //  super.onLayout(changed, left, top, right, bottom)
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
                path.moveTo(childView.x+childView.width/2, childView.y+childView.height)
                path.lineTo(cChildView.x+cChildView.width/2, cChildView.y)
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


}