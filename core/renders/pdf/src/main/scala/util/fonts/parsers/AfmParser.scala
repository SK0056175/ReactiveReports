/*
 * ReactiveReports - Free Java /Scala Reporting Library.
 * Copyright (C) 2017 SysAlto Corporation. All rights reserved.
  *
 * Unless you have purchased a commercial license agreement from SysAlto
 * the following license terms apply:
 *
 * This program is part of ReactiveReports.
 *
 * ReactiveReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReactiveReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY. Without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReactiveReports.
 * If not, see https://www.gnu.org/licenses/lgpl-3.0.en.html.
 */


package util.fonts.parsers

import scala.collection.mutable

/**
	* Created by marian on 5/6/17.
	*/
class AfmParser(fontFile: String) extends AbstractFontParser {


	private[this] val fontsMetrict = mutable.Map[String, FontAfmMetric]()


	private[this] def getValue(list: List[String], key: String): (Int, Int) = {
		val index: Int = list.indexWhere(line => line.startsWith(key))
		if (index == -1) {
			(index, 0)
		} else {
			val line = list(index)
			val tbl = line.trim.split("""\s""")
			(index, tbl(1).toInt)
		}
	}


	private[this] def parseFont(fontName: String): FontAfmMetric = {
		if (!fontsMetrict.contains(fontName)) {
			fontsMetrict += fontName -> parseFontInternal(fontName)
		}
		fontsMetrict(fontName)
	}

	private[this] def parseFontInternal(fontName: String): FontAfmMetric = {
		val textList = AfmParser.readFile(s"fonts/${fontName}.afm")
		val upperHeight = getValue(textList, "CapHeight")._2
		val lowerHeight = getValue(textList, "XHeight")._2
		val metrics = getValue(textList, "StartCharMetrics")
		val lineNbr = metrics._1
		val itemNbr = metrics._2
		val list1 = textList.slice(lineNbr.toInt + 1, lineNbr.toInt + 1 + itemNbr)
		val charList = list1.map(line => {
			val regExpr1 ="""C\s+-?\d+\s+;\s+WX\s+(\d+)\s+;\s+N\s+(\S+).*""".r
			val regExpr1(width, name) = line.trim
			name -> (width.toFloat * 0.001).toFloat
		})
		val charList1 = charList.map { case (glyph, code) => AfmParser.glyphDef.glypMap(glyph) -> code }.toMap
		FontAfmMetric(upperHeight, charList1)
	}

	def getStringWidth(str: String, fontMetric: FontAfmMetric): Float = {
		str.toCharArray.map(char => fontMetric.fontMap(char.toInt)).sum
	}

	override def getCharWidth(char: Char, fontMetric: FontAfmMetric): Float = {
		if (!fontMetric.fontMap.contains(char.toInt)) {
			0f
		} else {
			fontMetric.fontMap(char.toInt)
		}
	}


	val fontAfmMetric:FontAfmMetric=parseFont(fontFile)



}

object AfmParser {

	private[this] case class GlyphDef(glypMap: Map[String, Int])

	private def readFile(fileName: String): List[String] = {
		val stream = getClass.getClassLoader.getResourceAsStream(fileName)
		scala.io.Source.fromInputStream(stream)("latin1").getLines().toList
	}

	private def parseGlyph(): GlyphDef = {
		val charMap = scala.collection.mutable.HashMap[String, Int]()
		val content = readFile("fonts/agl-aglfn-master/glyphlist.txt")
		val list1 = content.filter(s => !s.trim.startsWith("#"))
		val list2 = list1.map(line => {
			val tbl = line.split(";")
			val tbl1 = tbl(1).split(" ").toList
			tbl(0) -> tbl1.map(item => Integer.parseInt(item, 16))
		}).toMap
		val result = list2.filter { case (name, list) =>
			list.length == 1
		}.map { case (name, list) => name -> list.head }
		GlyphDef(result)
	}


	private val glyphDef = parseGlyph()


	//	def main(args: Array[String]): Unit = {
	//		implicit val glypList = parseGlyph()
	//		val fontMetric = parseFont("Helvetica")
	//		val w = getStringWidth("IAW", fontMetric)
	//		println(w)
	//	}
}
