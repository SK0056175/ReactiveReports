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
import com.sysalto.report.reportTypes.ReportPageOrientation
import com.sysalto.report.util.{PdfFactory, ResultSetUtilTrait}

import scala.collection.mutable.ListBuffer
import scala.util.Random

object ReportKeepTogether extends ResultSetUtilTrait {
	val MAX_TRAN_LENGTH = 20
	val MAX_AMMOUNT = 100000

	case class Transaction(clientId: Long, amount: Int)

	case class Client(id: Long, name: String, address: String,
	                  transList1: List[Transaction])

	val clientList: ListBuffer[Client] = ListBuffer()

	def initClients(): Unit = {
		(1 to 50).foreach(clientId => {
			val nbr1 = Random.nextInt(MAX_TRAN_LENGTH)
			val transList1 = for (i <- 1 to nbr1) yield Transaction(clientId, Random.nextInt(MAX_AMMOUNT))
			clientList += Client(clientId, "name" + clientId, "address" + clientId, transList1.toList)
		})
	}

	private def report(report: Report): Unit = {
		var firstRecord=true
		clientList.foreach(client => {
			report.nextLine()
			val checkpoint=report.checkpoint()
			var detailMoved=firstRecord
			firstRecord=false
			val crtPos = report.getCurrentPosition
			var lastPosition=crtPos
			report print "Name: " at 10
			report print client.name at 60
			report.nextLine()
			report print "Address: " at 10
			report print client.address at 60
			report.setCurrentPosition(crtPos)
			client.transList1.foreach(tran1 => {
				report print "" + tran1.amount at 200
				report.nextLine()
				if (report.lineLeft < 3) {
					if (detailMoved) {
						report.nextPage()
					} else {
						val cut = report.cut(checkpoint)
						report.nextPage()
						report.paste(checkpoint, cut)
						detailMoved = true
					}
				}
			})
			report.gotoLastPosition()
			report.nextLine(2)
			if (report.lineLeft < 3) {
				report.nextPage()
			}
		})

		report.render()
	}


	def runReport(): Unit = {
		implicit val pdfFactory: PdfFactory = new PdfNativeFactory()
		val report1 = Report("KeepTogether.pdf", ReportPageOrientation.LANDSCAPE)
		report(report1)
	}


	def main(args: Array[String]): Unit = {
		initClients()
		runReport()
	}
}
