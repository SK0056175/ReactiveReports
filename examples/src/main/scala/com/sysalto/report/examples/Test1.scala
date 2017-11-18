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



package com.sysalto.report.examples

import com.sysalto.render.PdfNativeFactory
import com.sysalto.report.Implicits._
import com.sysalto.report.reportTypes.{RFont, ReportPageOrientation}

/**
  * Created by marian on 4/1/17.
  */
object Test1  {

  def run(): Unit = {
    implicit val pdfFactory = new PdfNativeFactory()
    val report = Report("Test.pdf" ,ReportPageOrientation.LANDSCAPE)
    runReport(report)
  }

  def runReport(report: Report): Unit = {
    report.nextLine(3)
    val char="BB"
    val size=12
    val txt1=RText(char,RFont(size,fontName = "Roboto",fontFile = Some("/home/marian/transfer/font/Roboto-Regular.ttf")))
  //  val txt5=RText(char,RFont(size,fontName = "Roboto",fontFile = Some("/home/marian/transfer/font/Roboto-Regular.ttf")))
//    val txt3=RText(char,RFont(size,fontName = "Calibri",fontFile = Some("/home/marian/transfer/font/calibri/Calibri.ttf")))
//    val txt4=RText(char,RFont(size,fontName = "Lily",fontFile = Some("/home/marian/transfer/font/lily/LilyoftheValley.ttf")))
    val txt2=RText(char,RFont(size))
    report print txt1 at 100
    report.nextLine()
//    report print txt2 at 100
 //   report.nextLine()
//    report print txt5 at 100
//    report.nextLine()
//    report print txt4 at 100

    report.render()
  }


  def main(args: Array[String]): Unit = {
    run()
  }

}