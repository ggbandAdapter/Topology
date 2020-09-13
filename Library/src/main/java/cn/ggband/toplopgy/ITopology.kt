package cn.ggband.toplopgy

import android.view.View

interface ITopology {

    fun childList(): List<ITopology>?

    fun nodeLayoutId(): Int


}