package cn.ggband.toplopgy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.Scroller
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

    private var lastInterceptX = 0f
    private var lastInterceptY = 0f
    // //滑动速度追踪器
    private lateinit var mVelocityTracker: VelocityTracker
    //这个scroller是为了平滑滑动
    private lateinit var mScroller: Scroller
    private var downX = 0f
    private var distanceX = 0f
    private var isFirstTouch = true
    private var childIndex = -1


    private var mNodeViewCallback: NodeViewCallback? = null


    private val topologyList = ArrayList<TopologyEx>()

    init {
        mScroller =  Scroller(context);
        //初始化追踪器
        mVelocityTracker = VelocityTracker.obtain();//获得追踪器对象，这里用obtain，按照谷歌的尿性，应该是考虑了对象重用
    }


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
        setMeasuredDimension(getMaxFloorWidth(), height)
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
        var ifIntercept = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastInterceptX = event.rawX
                lastInterceptY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                //检查是横向移动的距离大，还是纵向
                val xDistance: Float = Math.abs(lastInterceptX - event.rawX)
                val yDistance: Float = Math.abs(lastInterceptY - event.rawY)
                ifIntercept = if (xDistance > yDistance) {
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        return ifIntercept
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(LOGTAG, "rawX:${event.rawX};rawY:${event.rawY}")
        val scrollX = scrollX //控件的左边界，与屏幕原点的X轴坐标

        val scrollXMax = (childCount - 1) * getChildAt(1).measuredWidth
        val childWidth = getChildAt(0).width
        mVelocityTracker!!.addMovement(event) //在onTouchEvent这里，截取event对象

        val configuration = ViewConfiguration.get(context)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
                //先让你滑动起来
                val moveX = event.rawX
                if (isFirstTouch) { //一次事件序列，只会赋值一次？
                    downX = moveX
                    isFirstTouch = false
                }
                Log.d(LOGTAG, "$downX|$moveX|$distanceX")
                distanceX = downX - moveX
                //判定是否可以滑动
//这里有一个隐患，由于不知道Move事件，会以什么频率来分发，所以，这里多少都会出现一点误差
                if (childCount >= 2) { //子控件在2个或者2个以上时，才有下面的效果
//如果命令是向左滑动,distanceX>0 ，那么判断命令是否可以执行
//如果命令是向右滑动,distanceX<0 ，那么判断命令是否可以执行
                    Log.d(LOGTAG, "scrollX:$scrollX")
                    if (distanceX <= 0) {
                        if (scrollX >= 0) scrollBy(distanceX.toInt(), 0) //滑动
                    } else {
                        if (scrollX <= scrollXMax) scrollBy(distanceX.toInt(), 0) //滑动
                    }
                } //如果只有一个，则不允许滑动，防止bug
            }
            MotionEvent.ACTION_UP -> {
                mVelocityTracker.computeCurrentVelocity(
                    1000,
                    configuration.scaledMaximumFlingVelocity.toFloat()
                ) //计算，最近的event到up之间的速率
                val xVelocity = mVelocityTracker.xVelocity //当前横向的移动速率
                val edgeXVelocity =
                    configuration.scaledMinimumFlingVelocity.toFloat() //临界点
                childIndex =
                    (scrollX + childWidth / 2) / childWidth //整除的方式，来确定X轴应该所在的单元，将每一个item的竖向中间线定为滑动的临界线
                if (Math.abs(xVelocity) > edgeXVelocity) { //如果当前横向的速率大于零界点，
                    childIndex =
                        if (xVelocity > 0) childIndex - 1 else childIndex + 1 //xVelocity正数，表示从左往右滑，所以child应该是要显示前面一个
                }
                //                childIndex = Math.min(getChildCount() - 1, Math.max(childIndex, 0));//不可以超出左右边界,这种写法可能很难一眼看懂，那就替换成下面的写法
                if (childIndex < 0) //计算出的childIndex可能是负数。那就赋值为0
                    childIndex =
                        0 else if (childIndex >= childCount) { //也有可能超出childIndex的最大值，那就赋值为最大值-1
                    childIndex = childCount - 1
                }
                smoothScrollBy(childIndex * childWidth - scrollX, 0) // 回滚的距离
                mVelocityTracker.clear()
                isFirstTouch = true
            }
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        downX = event.rawX
        return super.onTouchEvent(event)
    }

    /**
     * 最叼的还是这个方法，平滑地回滚，从当前位置滚到目标位置
     * @param dx
     * @param dy
     */
   private fun smoothScrollBy(dx: Int, dy: Int) {
        mScroller!!.startScroll(scrollX, scrollY, dx, dy, 500) //从当前滑动的位置，平滑地过度到目标位置
        invalidate()
    }

    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {
            scrollTo(mScroller.currX, mScroller.currY)
            invalidate()
        }
    }


}