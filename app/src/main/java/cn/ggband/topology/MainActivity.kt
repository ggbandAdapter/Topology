package cn.ggband.topology

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.ggband.toplopgy.ITopology
import cn.ggband.toplopgy.callback.NodeViewCallback
import cn.ggband.topology.bean.NodeBean
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setData()
    }

    private fun setData() {
        topologyView.setNodeViewCallback(object : NodeViewCallback {
            override fun onNodeView(view: View, iTopology: ITopology) {
                val node = iTopology as NodeBean
                view.findViewById<TextView>(R.id.tvNodeName).text = node.name
                view.findViewById<TextView>(R.id.tvNodeDesc).text = node.desc
            }
        })
        topologyView.setTopology(getNode())
    }

    private fun getNode(): NodeBean {

        val nodeBean = NodeBean().apply {
            type = 1
            name = "Router"
            desc = "Main"
            childs = arrayListOf(
                NodeBean().apply {
                    type = 1
                    name = "Router"
                    desc = "Router 2"
                    childs = arrayListOf(
                        NodeBean().apply {
                            type = 0
                            name = "Mesh"
                            desc = "Mesh 3"
                        },
                        NodeBean().apply {
                            type = 1
                            name = "Router"
                            desc = "Router 3"
                        }

                    )
                },
                NodeBean().apply {
                    type = 0
                    name = "Mesh"
                    desc = "Mesh 2"
                },
                NodeBean().apply {
                    type = 1
                    name = "Router"
                    desc = "Router 2"
                    childs = arrayListOf(
                        NodeBean().apply {
                            type = 0
                            name = "Mesh"
                            desc = "Mesh 3"
                        },
                        NodeBean().apply {
                            type = 1
                            name = "Router"
                            desc = "Router 3"
                            childs = arrayListOf(
                                NodeBean().apply {
                                    type = 0
                                    name = "Mesh"
                                    desc = "Mesh 4"
                                },
                                NodeBean().apply {
                                    type = 1
                                    name = "Router"
                                    desc = "Router 4"
                                }

                            )
                        }

                    )
                },
                NodeBean().apply {
                    type = 0
                    name = "Mesh"
                    desc = "Mesh 2"
                }


            )
        }

        return nodeBean
    }
}
