package cn.ggband.topology.bean

import cn.ggband.toplopgy.ITopology
import cn.ggband.topology.R

class NodeBean : ITopology {
    var type = 0
    var name = ""
    var desc = ""
    var childs: List<NodeBean> = emptyList()

    override fun childList(): List<ITopology> {
        return childs
    }

    override fun nodeLayoutId(): Int {
        return if (type == 0) R.layout.view_node_mesh else R.layout.view_node_router
    }

    override fun toString(): String {
        return "NodeBean(type=$type, name='$name', desc='$desc', childs=$childs)"
    }


}