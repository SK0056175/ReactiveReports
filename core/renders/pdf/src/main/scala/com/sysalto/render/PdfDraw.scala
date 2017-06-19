package com.sysalto.render

import com.sysalto.report.reportTypes.{LineDashType, RColor}
import com.sysalto.render.basic.PdfBasic._
import PdfChart._

object PdfDraw {

	abstract class PdfGraphicChuck {
		def content: String
	}

	case class DrawPoint(x: Float, y: Float)


	def roundRectangle(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float): String = {
		movePoint(DrawPoint(x1 + radius, y1)) +
			lineTo(DrawPoint(x2 - radius, y1)) +
			arc(DrawPoint(x2 - radius, y1 - radius), radius, (Math.PI * 0.5).toFloat, 0f) +
			lineTo(DrawPoint(x2, y2 + radius)) +
			arc(DrawPoint(x2 - radius, y2 + radius), radius, 2 * Math.PI.toFloat, (3.0 * Math.PI * 0.5).toFloat) +
			lineTo(DrawPoint(x1 + radius, y2)) +
			arc(DrawPoint(x1 + radius, y2 + radius), radius, (3.0 * Math.PI * 0.5).toFloat, Math.PI.toFloat) +
			lineTo(DrawPoint(x1, y1 - radius)) +
			arc(DrawPoint(x1 + radius, y1 - radius), radius, Math.PI.toFloat, (Math.PI * 0.5).toFloat)
	}


	case class DrawArc(center: DrawPoint, radius: Float, startAngle: Float, endAngle: Float) extends PdfGraphicChuck {
		override def content: String = {
			val p0 = DrawPoint((center.x + radius * Math.cos(startAngle)).toFloat, (center.y + radius * Math.sin(startAngle)).toFloat)
			val moveStr = movePoint(p0)
			val arcStr = arc(center, radius, startAngle, endAngle)
			//https://stackoverflow.com/questions/1734745/how-to-create-circle-with-b%C3%A9zier-curves
			s"""${moveStr}
				 | ${arcStr}
     """.stripMargin
		}
	}

	case class DrawCircle(center: DrawPoint, radius: Float) extends PdfGraphicChuck {
		override def content: String = circle(center, radius)
	}


	case class DrawStroke() extends PdfGraphicChuck {
		override def content: String = {
			"S"
		}
	}

	case class DrawFill() extends PdfGraphicChuck {
		override def content: String = {
			"f"
		}
	}

	case class DrawFillStroke() extends PdfGraphicChuck {
		override def content: String = {
			"B"
		}
	}

	case class DrawMovePoint(x: Float, y: Float) extends PdfGraphicChuck {
		override def content: String = {
			s"""${x} ${y} m"""
		}
	}

	case class DrawLine(x1: Float, y1: Float, x2: Float, y2: Float, lineWidth: Float, color: RColor, lineDashType: Option[LineDashType]) extends PdfGraphicChuck {
		override def content: String = {
			movePoint(x1, y1) + lineTo(x2, y2)
		}
	}

	case class DrawRectangle(x: Float, y: Float, width: Float, height: Float) extends PdfGraphicChuck {
		override def content: String = {
			s"""${x} ${y} ${width} ${height} re"""
		}
	}

	case class DrawBorderColor(borderColor: RColor) extends PdfGraphicChuck {
		override def content: String = {
			val color = convertColor(borderColor)
			s"${color._1} ${color._2} ${color._3} RG"
		}
	}

	case class DrawFillColor(borderColor: RColor) extends PdfGraphicChuck {
		override def content: String = {
			val color = convertColor(borderColor)
			s"${color._1} ${color._2} ${color._3} rg"
		}
	}

	case class DrawPattern(pdfPattern: PdfGPattern) extends PdfGraphicChuck {
		override def content: String = {
			s"/Pattern cs /${pdfPattern.name} scn"
		}
	}


	case class PdfRectangle(x1: Long, y1: Long, x2: Long, y2: Long, radius: Float, borderColor: Option[RColor],
	                        fillColor: Option[RColor], patternColor: Option[PdfGPattern] = None) extends PdfGraphicChuck {
		override def content: String = {
			val paternStr = if (patternColor.isDefined) pattern(patternColor.get.name) else ""
			val borderStr = if (borderColor.isDefined) border(borderColor.get) else ""
			val fillStr = if (fillColor.isDefined) fill(fillColor.get) else ""
			val operator = fillStroke(fillColor.isDefined || patternColor.isDefined, borderColor.isDefined)
			val rectangleStr = if (radius == 0) rectangle(x1, y1, x2 - x1, y2 - y1) else roundRectangle(x1, y1, x2, y2, radius)
			s"""${saveStatus}
				 |${paternStr}
				 |${borderStr}
				 |${fillStr}
				 |${rectangleStr}
				 | ${operator}
				 |${restoreStatus}
       """.stripMargin.trim
		}

	}

	case class DrawPieChart(title: String, data: Map[String, Double], x: Float, y: Float, width: Float, height: Float) extends PdfGraphicChuck {
		override def content: String = pieChart(title, data.toList, x, y, width, height)
	}

}
