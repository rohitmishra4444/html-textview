/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@schuermann.eu>
 * Copyright (C) 2013-2015 Juha Kuitunen
 * Copyright (C) 2013 Mohammed Lakkadshaw
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sufficientlysecure.htmltextview

import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.text.style.*
import android.util.Log
import android.view.View
import org.sufficientlysecure.htmltextview.HtmlTextView
import org.xml.sax.Attributes
import java.util.*

/**
 * Some parts of this code are based on android.text.Html
 */
class HtmlTagHandler : WrapperTagHandler {
    /**
     * Newer versions of the Android SDK's [Html.TagHandler] handles &lt;ul&gt; and &lt;li&gt;
     * tags itself which means they never get delegated to this class. We want to handle the tags
     * ourselves so before passing the string html into Html.fromHtml(), we can use this method to
     * replace the &lt;ul&gt; and &lt;li&gt; tags with tags of our own.
     *
     * @param html String containing HTML, for example: "**Hello world!**"
     * @return html with replaced  and  *  tags
     * @see [Specific Android SDK Commit](https://github.com/android/platform_frameworks_base/commit/8b36c0bbd1503c61c111feac939193c47f812190)
     */
    fun overrideTags(html: String?): String? {
        var html = html ?: return null
        html = "<$PLACEHOLDER_ITEM></$PLACEHOLDER_ITEM>$html"
        html = html.replace("<ul", "<$UNORDERED_LIST")
        html = html.replace("</ul>", "</$UNORDERED_LIST>")
        html = html.replace("<ol", "<$ORDERED_LIST")
        html = html.replace("</ol>", "</$ORDERED_LIST>")
        html = html.replace("<li", "<$LIST_ITEM")
        html = html.replace("</li>", "</$LIST_ITEM>")
        html = html.replace("<a", "<$A_ITEM")
        html = html.replace("</a>", "</$A_ITEM>")
        return html
    }

    /**
     * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
     * and on top of Stack is the most nested list
     */
    var lists = Stack<String>()
    /**
     * Tracks indexes of ordered lists so that after a nested list ends
     * we can continue with correct index of outer list
     */
    var olNextIndex = Stack<Int>()
    /**
     * List indentation in pixels. Nested lists use multiple of this.
     */
    /**
     * Running HTML table string based off of the root table tag. Root table tag being the tag which
     * isn't embedded within any other table tag. Example:
     *
     * <table>
     * ...
     * <table>
     * ...
    </table> *
     * ...
    </table> *
     *
     */
    var tableHtmlBuilder = StringBuilder()
    /**
     * Tells us which level of table tag we're on; ultimately used to find the root table tag.
     */
    var tableTagLevel = 0
    private var clickableTableSpan: ClickableTableSpan? = null
    private var drawTableLinkSpan: DrawTableLinkSpan? = null
    private var onClickATagListener: OnClickATagListener? = null

    private class Ul
    private class Ol
    private class A constructor(val href: String?)

    private class Code
    private class Center
    private class Strike
    private class Table
    private class Tr
    private class Th
    private class Td

    override fun handleTag(opening: Boolean, tag: String, output: Editable, attributes: Attributes?): Boolean {
        if (opening) { // opening tag
            if (HtmlTextView.DEBUG) {
                Log.d(HtmlTextView.TAG, "opening, output: $output")
            }
            if (tag.equals(UNORDERED_LIST, ignoreCase = true)) {
                lists.push(tag)
            } else if (tag.equals(ORDERED_LIST, ignoreCase = true)) {
                lists.push(tag)
                olNextIndex.push(1)
            } else if (tag.equals(LIST_ITEM, ignoreCase = true)) {
                if (output.isNotEmpty() && output[output.length - 1] != '\n') {
                    output.append("\n")
                }
                if (!lists.isEmpty()) {
                    val parentList = lists.peek()
                    if (parentList.equals(ORDERED_LIST, ignoreCase = true)) {
                        start(output, Ol())
                        olNextIndex.push(olNextIndex.pop() + 1)
                    } else if (parentList.equals(UNORDERED_LIST, ignoreCase = true)) {
                        start(output, Ul())
                    }
                }
            } else if (tag.equals(A_ITEM, ignoreCase = true)) {
                val href = attributes?.getValue("href")
                start(output, A(href))
            } else if (tag.equals("code", ignoreCase = true)) {
                start(output, Code())
            } else if (tag.equals("center", ignoreCase = true)) {
                start(output, Center())
            } else if (tag.equals("s", ignoreCase = true) || tag.equals("strike", ignoreCase = true)) {
                start(output, Strike())
            } else if (tag.equals("table", ignoreCase = true)) {
                start(output, Table())
                if (tableTagLevel == 0) {
                    tableHtmlBuilder = StringBuilder()
                    // We need some text for the table to be replaced by the span because
// the other tags will remove their text when their text is extracted
                    output.append("table placeholder")
                }
                tableTagLevel++
            } else if (tag.equals("tr", ignoreCase = true)) {
                start(output, Tr())
            } else if (tag.equals("th", ignoreCase = true)) {
                start(output, Th())
            } else if (tag.equals("td", ignoreCase = true)) {
                start(output, Td())
            } else {
                return false
            }
        } else { // closing tag
            if (HtmlTextView.DEBUG) {
                Log.d(HtmlTextView.TAG, "closing, output: $output")
            }
            if (tag.equals(UNORDERED_LIST, ignoreCase = true)) {
                lists.pop()
            } else if (tag.equals(ORDERED_LIST, ignoreCase = true)) {
                lists.pop()
                olNextIndex.pop()
            } else if (tag.equals(LIST_ITEM, ignoreCase = true)) {
                if (!lists.isEmpty()) {
                    val listItemIndent = if (userGivenIndent > -1) userGivenIndent * 2 else defaultListItemIndent
                    if (lists.peek().equals(UNORDERED_LIST, ignoreCase = true)) {
                        if (output.isNotEmpty() && output[output.length - 1] != '\n') {
                            output.append("\n")
                        }
                        // Nested BulletSpans increases distance between bullet and text, so we must prevent it.
                        var indent = if (userGivenIndent > -1) userGivenIndent else defaultIndent
                        val bullet = if (userGivenIndent > -1) BulletSpan(userGivenIndent) else defaultBullet
                        if (lists.size > 1) {
                            indent -= bullet.getLeadingMargin(true)
                            if (lists.size > 2) { // This get's more complicated when we add a LeadingMarginSpan into the same line:
// we have also counter it's effect to BulletSpan
                                indent -= (lists.size - 2) * listItemIndent
                            }
                        }
                        val newBullet = BulletSpan(indent)
                        end(output, Ul::class.java, false,
                                LeadingMarginSpan.Standard(listItemIndent * (lists.size - 1)),
                                newBullet)
                    } else if (lists.peek().equals(ORDERED_LIST, ignoreCase = true)) {
                        if (output.isNotEmpty() && output[output.length - 1] != '\n') {
                            output.append("\n")
                        }
                        // Nested NumberSpans increases distance between number and text, so we must prevent it.
                        var indent = if (userGivenIndent > -1) userGivenIndent else defaultIndent
                        val span = NumberSpan(indent, olNextIndex.lastElement() - 1)
                        if (lists.size > 1) {
                            indent -= span.getLeadingMargin(true)
                            if (lists.size > 2) { // As with BulletSpan, we need to compensate for the spacing after the number.
                                indent -= (lists.size - 2) * listItemIndent
                            }
                        }
                        val numberSpan = NumberSpan(indent, olNextIndex.lastElement() - 1)
                        end(output, Ol::class.java, false,
                                LeadingMarginSpan.Standard(listItemIndent * (lists.size - 1)),
                                numberSpan)
                    }
                }
            } else if (tag.equals(A_ITEM, ignoreCase = true)) {
                val a = getLast(output, A::class.java)
                val href = if (a is A) a.href else null
                end(output, A::class.java, false, object : URLSpan(href) {
                    override fun onClick(widget: View) {
                        if (onClickATagListener != null) {
                            onClickATagListener!!.onClick(widget, url)
                        } else {
                            super.onClick(widget)
                        }
                    }
                })
            } else if (tag.equals("code", ignoreCase = true)) {
                end(output, Code::class.java, false, TypefaceSpan("monospace"))
            } else if (tag.equals("center", ignoreCase = true)) {
                end(output, Center::class.java, true, AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER))
            } else if (tag.equals("s", ignoreCase = true) || tag.equals("strike", ignoreCase = true)) {
                end(output, Strike::class.java, false, StrikethroughSpan())
            } else if (tag.equals("table", ignoreCase = true)) {
                tableTagLevel--
                // When we're back at the root-level table
                if (tableTagLevel == 0) {
                    val tableHtml = tableHtmlBuilder.toString()
                    var myClickableTableSpan: ClickableTableSpan? = null
                    if (clickableTableSpan != null) {
                        myClickableTableSpan = clickableTableSpan!!.newInstance()
                        myClickableTableSpan!!.tableHtml = tableHtml
                    }
                    var myDrawTableLinkSpan: DrawTableLinkSpan? = null
                    if (drawTableLinkSpan != null) {
                        myDrawTableLinkSpan = drawTableLinkSpan!!.newInstance()
                    }
                    end(output, Table::class.java, false, myDrawTableLinkSpan!!, myClickableTableSpan!!)
                } else {
                    end(output, Table::class.java, false)
                }
            } else if (tag.equals("tr", ignoreCase = true)) {
                end(output, Tr::class.java, false)
            } else if (tag.equals("th", ignoreCase = true)) {
                end(output, Th::class.java, false)
            } else if (tag.equals("td", ignoreCase = true)) {
                end(output, Td::class.java, false)
            } else {
                return false
            }
        }
        storeTableTags(opening, tag)
        return true
    }

    /**
     * If we're arriving at a table tag or are already within a table tag, then we should store it
     * the raw HTML for our ClickableTableSpan
     */
    private fun storeTableTags(opening: Boolean, tag: String) {
        if (tableTagLevel > 0 || tag.equals("table", ignoreCase = true)) {
            tableHtmlBuilder.append("<")
            if (!opening) {
                tableHtmlBuilder.append("/")
            }
            tableHtmlBuilder
                    .append(tag.toLowerCase())
                    .append(">")
        }
    }

    /**
     * Mark the opening tag by using private classes
     */
    private fun start(output: Editable, mark: Any) {
        val len = output.length
        output.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK)
        if (HtmlTextView.DEBUG) {
            Log.d(HtmlTextView.TAG, "len: $len")
        }
    }

    /**
     * Modified from [android.text.Html]
     */
    private fun end(output: Editable, kind: Class<*>, paragraphStyle: Boolean, vararg replaces: Any) {
        val obj = getLast(output, kind)
        // start of the tag
        val where = output.getSpanStart(obj)
        // end of the tag
        val len = output.length
        // If we're in a table, then we need to store the raw HTML for later
        if (tableTagLevel > 0) {
            val extractedSpanText = extractSpanText(output, kind)
            tableHtmlBuilder.append(extractedSpanText)
        }
        output.removeSpan(obj)
        if (where != len) {
            var thisLen = len
            // paragraph styles like AlignmentSpan need to end with a new line!
            if (paragraphStyle) {
                output.append("\n")
                thisLen++
            }
            for (replace in replaces) {
                output.setSpan(replace, where, thisLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (HtmlTextView.DEBUG) {
                Log.d(HtmlTextView.TAG, "where: $where")
                Log.d(HtmlTextView.TAG, "thisLen: $thisLen")
            }
        }
    }

    /**
     * Returns the text contained within a span and deletes it from the output string
     */
    private fun extractSpanText(output: Editable, kind: Class<*>): CharSequence {
        val obj = getLast(output, kind)
        // start of the tag
        val where = output.getSpanStart(obj)
        // end of the tag
        val len = output.length
        val extractedSpanText = output.subSequence(where, len)
        output.delete(where, len)
        return extractedSpanText
    }

    // Util method for setting pixels.
    fun setListIndentPx(px: Float) {
        userGivenIndent = Math.round(px)
    }

    fun setClickableTableSpan(clickableTableSpan: ClickableTableSpan?) {
        this.clickableTableSpan = clickableTableSpan
    }

    fun setDrawTableLinkSpan(drawTableLinkSpan: DrawTableLinkSpan?) {
        this.drawTableLinkSpan = drawTableLinkSpan
    }

    fun setOnClickATagListener(onClickATagListener: OnClickATagListener?) {
        this.onClickATagListener = onClickATagListener
    }

    companion object {
        const val UNORDERED_LIST = "HTML_TEXTVIEW_ESCAPED_UL_TAG"
        const val ORDERED_LIST = "HTML_TEXTVIEW_ESCAPED_OL_TAG"
        const val LIST_ITEM = "HTML_TEXTVIEW_ESCAPED_LI_TAG"
        const val A_ITEM = "HTML_TEXTVIEW_ESCAPED_A_TAG"
        const val PLACEHOLDER_ITEM = "HTML_TEXTVIEW_ESCAPED_PLACEHOLDER"
        private var userGivenIndent = -1
        private const val defaultIndent = 10
        private const val defaultListItemIndent = defaultIndent * 2
        private val defaultBullet = BulletSpan(defaultIndent)
        /**
         * Get last marked position of a specific tag kind (private class)
         */
        private fun getLast(text: Editable, kind: Class<*>): Any? {
            val objs = text.getSpans(0, text.length, kind)
            return if (objs.isEmpty()) {
                null
            } else {
                for (i in objs.size downTo 1) {
                    if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                        return objs[i - 1]
                    }
                }
                null
            }
        }
    }
}