package cn.ggband.toplopgy

import android.content.Context
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

    private var mNodeViewCallback: NodeViewCallback? = null

    private val LOGTAG = "TopologyView"

    init {
        //  orientation = VERTICAL
    }

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
            val index = sameFloorList.indexOf(topologyEx)
            Log.d(LOGTAG, "node on sameFloorList index:$index")
            Log.d(LOGTAG, "lastFloor:$lastFloor;currFloor:${topologyEx.floor}")

            if (lastFloor != topologyEx.floor) {
                layoutWidth = 0
            }
            Log.d(LOGTAG, "layoutWidth:$layoutWidth")

            childLeft = layoutWidth
            childTop = topologyEx.floor * nodeChildView.measuredHeight
            childRight = childLeft + nodeChildView.measuredWidth
            childBottom = childTop + nodeChildView.measuredHeight
            nodeChildView.layout(
                childLeft,
                childTop,
                childRight,
                childBottom
            )
            layoutWidth += nodeChildView.measuredWidth
            lastFloor = topologyEx.floor
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
            Log.d(LOGTAG,it.topology.toString()+"===floor:"+it.floor)
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
        val childCount = this.childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

    }
}