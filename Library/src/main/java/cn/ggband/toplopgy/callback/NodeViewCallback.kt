package cn.ggband.toplopgy.callback

import android.view.View
import cn.ggband.toplopgy.ITopology

interface NodeViewCallback {
    fun onNodeView(view: View, iTopology: ITopology)
}