package org.sufficientlysecure.htmltextview

import android.text.Editable
import android.text.Html.TagHandler
import org.xml.sax.*

class WrapperContentHandler(private val mTagHandler: WrapperTagHandler) : ContentHandler, TagHandler {
    private var mContentHandler: ContentHandler? = null
    private var mSpannableStringBuilder: Editable? = null
    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (mContentHandler == null) {
            mSpannableStringBuilder = output
            mContentHandler = xmlReader.contentHandler
            xmlReader.contentHandler = this
        }
    }

    override fun setDocumentLocator(locator: Locator) {
        mContentHandler!!.setDocumentLocator(locator)
    }

    @Throws(SAXException::class)
    override fun startDocument() {
        mContentHandler!!.startDocument()
    }

    @Throws(SAXException::class)
    override fun endDocument() {
        mContentHandler!!.endDocument()
    }

    @Throws(SAXException::class)
    override fun startPrefixMapping(prefix: String, uri: String) {
        mContentHandler!!.startPrefixMapping(prefix, uri)
    }

    @Throws(SAXException::class)
    override fun endPrefixMapping(prefix: String) {
        mContentHandler!!.endPrefixMapping(prefix)
    }

    @Throws(SAXException::class)
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        if (!mTagHandler.handleTag(true, localName, mSpannableStringBuilder, attributes)) {
            mContentHandler!!.startElement(uri, localName, qName, attributes)
        }
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String, localName: String, qName: String) {
        if (!mTagHandler.handleTag(false, localName, mSpannableStringBuilder, null)) {
            mContentHandler!!.endElement(uri, localName, qName)
        }
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        mContentHandler!!.characters(ch, start, length)
    }

    @Throws(SAXException::class)
    override fun ignorableWhitespace(ch: CharArray, start: Int, length: Int) {
        mContentHandler!!.ignorableWhitespace(ch, start, length)
    }

    @Throws(SAXException::class)
    override fun processingInstruction(target: String, data: String) {
        mContentHandler!!.processingInstruction(target, data)
    }

    @Throws(SAXException::class)
    override fun skippedEntity(name: String) {
        mContentHandler!!.skippedEntity(name)
    }

}