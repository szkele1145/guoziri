package com.guoziri.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 引导锚点注册表。
 *
 * 页面把关键控件用 [Modifier.guideAnchor] 挂进来，引导层根据
 * anchor id 找到控件在屏幕上的位置，画聚光高亮。
 *
 * 每个锚点带一个 scope（通常是所在页面的 tab id），避免切页后
 * 读到上一页残留的坐标。
 *
 * 用法：
 *   val anchors = remember { GuideAnchors() }
 *   Text("记账", Modifier.guideAnchor(anchors, "tab-ledger", scope = "ledger"))
 */
@Stable
class GuideAnchors {
    private val _rects = mutableStateMapOf<String, Entry>()

    data class Entry(val scope: String, val rect: Rect)

    /** 记录某锚点在根坐标系的矩形 */
    fun record(id: String, scope: String, rect: Rect) {
        val cur = _rects[id]
        if (cur == null || cur.scope != scope || cur.rect != rect) {
            _rects[id] = Entry(scope, rect)
        }
    }

    fun remove(id: String) {
        _rects.remove(id)
    }

    /** 取锚点矩形；scope 不匹配返回 null */
    fun rect(id: String, scope: String): Rect? {
        val e = _rects[id] ?: return null
        return if (e.scope == scope) e.rect else null
    }

    fun clear() = _rects.clear()

    val ids: Set<String> get() = _rects.keys
}

fun Modifier.guideAnchor(anchors: GuideAnchors, id: String, scope: String) =
    this.onGloballyPositioned { coords ->
        val p = coords.positionInWindow()
        anchors.record(id, scope, Rect(p.x, p.y, p.x + coords.size.width, p.y + coords.size.height))
    }
