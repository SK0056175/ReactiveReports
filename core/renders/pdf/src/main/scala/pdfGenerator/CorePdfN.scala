package core

import java.io.{ByteArrayOutputStream, File}
import java.util.zip.{Deflater, Inflater}
import javax.imageio.ImageIO

import pdfGenerator.{PageTree, WriteUtil}

import scala.collection.mutable.ListBuffer


/**
  * Created by marian on 1/16/16.
  */
object CorePdfN {

  class CorePdfN(fileName: String, fastWeb: Boolean = false) {
    val writeUtil = WriteUtil(fileName)
    var id = 0l
    val objectList = ListBuffer[Base]()

    private def header(): Unit = {
      writeUtil <<< "%PDF-1.7"
      writeUtil <<< "%\u00a0"
      writeUtil <<< "%"
    }

    def nextId = {
      id = id + 1
      id
    }

    def addToList(obj: Base): Unit = {
      objectList += obj
    }


    def writeAll()(writeCall: () => Unit): Unit = {
      header
      val list = writeCall()
      objectList.foreach(item => item.write())
      writeTrailer()
    }


    def writeTrailer(): Unit = {
      val xrefOffset = writeUtil.position
      writeUtil <<< "xref"
      val objSize = objectList.size + 1
      writeUtil <<< s"0 $objSize"
      writeUtil <<< "0000000000 65535 f "
      objectList.sortBy(obj => obj.objId).foreach(obj => {
        val offset = obj.offset.toString
        val offsetFrmt = "0" * (10 - offset.length) + offset
        writeUtil <<< s"$offsetFrmt 00000 n "
      })
      val catalog = objectList.find(item => item.isInstanceOf[Catalog]).get.asInstanceOf[Catalog]

      writeUtil <<< "trailer"
      writeUtil <<< "<<"
      writeUtil <<< s" /Size $objSize"
      writeUtil <<< s" /Root ${catalog.objId} 0 R"
      writeUtil <<< ">>"
      writeUtil <<< "startxref"
      writeUtil <<< xrefOffset.toString
      writeUtil <<< "%%EOF"
    }


  }


  abstract class Base(pdf: CorePdfN, id: Long) extends Serializable {
    val objId = id
    var offset = 0L
    pdf.addToList(this)

    def writeFile(writeCall: () => Unit): Unit = {
      offset = pdf.writeUtil.position
      writeCall()
    }

    def write()
  }

  class Catalog(pdf: CorePdfN, id: Long, pageListId: Long) extends Base(pdf, id) {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"$id 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< " /Type /Catalog"
        pdf.writeUtil <<< s" /Pages ${pageListId} 0 R"
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }

    }
  }

  class PageList(pdf: CorePdfN, id: Long, parent: Option[Long],
                 children: List[Long], pagesSize: Long) extends Base(pdf, id) {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< " /Type /Pages"
        if (parent != None) {
          pdf.writeUtil <<< s" /Parent ${parent.get} 0 R"
        }
        pdf.writeUtil << " /Kids ["
        children.foreach(item => {
          pdf.writeUtil << s"${item} 0 R "
        })
        pdf.writeUtil <<< "]"
        pdf.writeUtil <<< s" /Count ${pagesSize}"
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }


  class Page(pdf: CorePdfN, id: Long, parentId: Long,
             resourceId: Long, contentId: Long) extends Base(pdf, id) {
    var parent = parentId

    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< " /Type /Page"
        pdf.writeUtil <<< s" /Parent ${parent} 0 R"
        pdf.writeUtil <<< " /MediaBox [ 0 0 612 792 ]"
        pdf.writeUtil <<< s" /Resources ${resourceId} 0 R"
        pdf.writeUtil <<< s" /Contents ${contentId} 0 R"
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }

  trait ResourceElement

  class ResourceOld(pdf: CorePdfN, id: Long, fontId: Long,
                    imageList: List[ResourceImage] = List(), opacityIdList: List[Opacity] = List() /*, shadeList: List[Shade] = List()  */)
    extends Base(pdf, id) {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< " /ProcSet [/PDF /Text]"
        if (fontId > 0) {
          pdf.writeUtil <<< s" /Font <</F1 ${fontId} 0 R >>"
        }
        if (!opacityIdList.isEmpty) {
          pdf.writeUtil <<< s" /ExtGState <<"
          opacityIdList.foreach(opacity => pdf.writeUtil <<< s"/${opacity.name} << /CA ${opacity.opacity} /ca ${opacity.opacity} >>")
          pdf.writeUtil <<< ">>"
        }
        if (!imageList.isEmpty) {
          pdf.writeUtil <<< "/XObject <<"
          imageList.foreach(image => pdf.writeUtil <<< s"/${image.imageName} ${image.id} 0 R ")
          pdf.writeUtil <<< ">>"
        }
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }


  class Resource(pdf: CorePdfN, id: Long, resourceElements: List[ResourceElement])
    extends Base(pdf, id) {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< " /ProcSet [/PDF /Text]"
        resourceElements.foreach(resourceElement => {
          resourceElement match {
            case font: Font => {
              pdf.writeUtil <<< s" /Font <</F1 ${font.objId} 0 R >>"
            }
            case opacity: Opacity => {
              pdf.writeUtil <<< s" /ExtGState <<"
              pdf.writeUtil <<< s"/${opacity.name} << /CA ${opacity.opacity} /ca ${opacity.opacity} >>"
              pdf.writeUtil <<< ">>"
            }
            case image: ResourceImage => {
              pdf.writeUtil <<< "/XObject <<"
              pdf.writeUtil <<< s"/${image.imageName} ${image.id} 0 R "
              pdf.writeUtil <<< ">>"
            }
            case pattern: Pattern => {
              pdf.writeUtil <<< s"/Pattern<</P1 ${pattern.id} 0 R>>"
            }
          }
        })
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }

  class Font(pdf: CorePdfN, id: Long) extends Base(pdf, id) with ResourceElement {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< " /Type /Font"
        pdf.writeUtil <<< " /Subtype /Type1"
        pdf.writeUtil <<< " /Name /F1"
        pdf.writeUtil <<< " /BaseFont/Helvetica"
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }

  def writeText(pdf: CorePdfN, compress: Boolean, txt: String) {
    if (!compress) {
      pdf.writeUtil <<< s"<</Length ${txt.length}>>"
      pdf.writeUtil <<< "stream"
      pdf.writeUtil <<< txt
      pdf.writeUtil <<< "endstream"
      pdf.writeUtil <<< "endobj"
    } else {
      val input = txt.getBytes("UTF-8");
      val compresser = new Deflater(Deflater.BEST_COMPRESSION);
      compresser.setInput(input);
      compresser.finish();
      val output = new Array[Byte](input.size)
      val compressedDataLength = compresser.deflate(output);
      compresser.end();
      val compressTxt = output.take(compressedDataLength)
      pdf.writeUtil <<< s"<</Filter/FlateDecode/Length ${compressTxt.length}>>"
      pdf.writeUtil <<< "stream"
      pdf.writeUtil << compressTxt
      pdf.writeUtil <<< "endstream"
      pdf.writeUtil <<< "endobj"
    }
  }

  class Content(pdf: CorePdfN, id: Long, txtList: List[String],
                compress: Boolean = false) extends Base(pdf, id) {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        val txt = txtList.foldLeft("")((s1, s2) => s1 + s2)
        writeText(pdf, compress, txt)
      }
    }
  }


  abstract class BaseContent() extends Serializable {
    def content: String
  }


  case class Point(x: Int, y: Int)

  case class Rectangle(point: Point, width: Int, height: Int)

  case class Color(r: Double, g: Double, b: Double)

  case class Box(rectangle: Rectangle, lineWidth: Int, marginColor: Color = Color(0, 0, 0))
    extends BaseContent {
    def content: String =
      s"""[ ] 0 d
          |$lineWidth w
          |${marginColor.r} ${marginColor.g} ${marginColor.b} RG
          |${rectangle.point.x} ${rectangle.point.y} ${rectangle.width} ${rectangle.height} re
          |S
      """.stripMargin
  }

  case class BoxFill(rectangle: Rectangle, fillColor: Color, opacity: Option[Opacity] = None) extends BaseContent {
    val opacityStr = if (opacity == None) "" else s"/${opacity.get.name} gs"

    def content: String =
      s"""[ ] 0 d
          |$opacityStr
          |${fillColor.r} ${fillColor.g} ${fillColor.b} rg
          |${rectangle.point.x} ${rectangle.point.y} ${rectangle.width} ${rectangle.height} re
          |f
      """.stripMargin
  }

  //  case class BoxGradientFill(rectangle: Rectangle, fillColor: Color, shadeName: String, opacity: Option[Opacity] = None) extends BaseContent {
  //    val opacityStr = if (opacity == None) "" else s"/${opacity.get.name} gs"
  //
  //    def content: String =
  //      s"""[ ] 0 d
  //          |$opacityStr
  //          |${shadeName} sh
  //          |${fillColor.r} ${fillColor.g} ${fillColor.b} rg
  //          |${rectangle.point.x} ${rectangle.point.y} ${rectangle.width} ${rectangle.height} re
  //          |<< /ShadingType 2
  //          |/ColorSpace /DeviceGray
  //          |/Coords [${rectangle.point.x} ${rectangle.point.y} ${rectangle.point.x + rectangle.width} ${rectangle.point.y + rectangle.height}]
  //          |/Function << /FunctionType 2
  //          |% Value is C0 + t ** N * (C1 - C0)
  //          |/Domain [0 1]
  //          |/C0 0
  //          |/C1 1
  //          |/N 1
  //          |>>
  //          |>>
  //          |sh
  //      """.stripMargin
  //  }

  case class Line(lineWidth: Int, point1: Point, point2: Point) extends BaseContent {

    def content: String =
      s"""[ ] 0 d
          |$lineWidth w
          |${point1.x} ${point1.y} m
          |${point2.x} ${point2.y} l
          |S
      """.stripMargin
  }


  case class ResourceImage(pdf: CorePdfN, id: Long, image: ImageMeta) extends Base(pdf, id) with ResourceElement {
    val imageName = image.imageName

    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< " /Type /XObject"
        pdf.writeUtil <<< " /Subtype /Image"
        pdf.writeUtil <<< s" /Width ${image.width}"
        pdf.writeUtil <<< s" /Height ${image.height}"
        pdf.writeUtil <<< " /ColorSpace /DeviceRGB"
        pdf.writeUtil <<< s" /BitsPerComponent ${image.pixelSize}"
        pdf.writeUtil <<< s" /Length ${image.imageInByte.length}"
        pdf.writeUtil <<< " /Filter /DCTDecode"
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "stream"
        pdf.writeUtil << image.imageInByte
        pdf.writeUtil <<< "endstream"
        pdf.writeUtil <<< "endobj"
      }
    }
  }

  case class DrawImage(point: Point, scale: Double, image: ImageMeta, opacity: Option[Opacity] = None) extends BaseContent {
    val width = image.width * scale
    val height = image.height * scale
    val opacityStr = if (opacity == None) "" else s"/${opacity.get.name} gs"

    def content: String =
      s"""q
          |$opacityStr
          |$width 0 0 $height ${point.x} ${point.y} cm
          |/${image.imageName} Do
          |Q
    """.stripMargin
  }

  case class Opacity(name: String, opacity: Double) extends BaseContent with ResourceElement {
    def content: String = s"/${name} << /CA ${opacity} /ca ${opacity} >>"
  }


  case class FunctionType(pdf: CorePdfN, id: Long) extends Base(pdf, id) {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< "/FunctionType 2/C0[1 1 1]/C1[0 1 0]/Domain[0 1]/N 1"
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }


  case class ShadingType(pdf: CorePdfN, id: Long, functionType: FunctionType) extends Base(pdf, id) {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< s"/ColorSpace/DeviceRGB/Coords[0 300 0 0]/ShadingType 2/Function ${functionType.id} 0 R/Extend[true true]"
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }


  case class Pattern(pdf: CorePdfN, id: Long, shadingType: ShadingType) extends Base(pdf, id) with ResourceElement {
    def write(): Unit = {
      writeFile { () =>
        pdf.writeUtil <<< s"${id} 0 obj"
        pdf.writeUtil <<< "<<"
        pdf.writeUtil <<< s"/Shading ${shadingType.id} 0 R/Matrix[1 0 0 1 0 0]/PatternType "
        pdf.writeUtil <<< ">>"
        pdf.writeUtil <<< "endobj"
      }
    }
  }


  class ImageMeta(imgName: String, fileName: String) {
    val imageName = imgName
    val file = new File(fileName)
    val bimg = ImageIO.read(file);
    val width = bimg.getWidth();
    val height = bimg.getHeight();
    val size = file.length
    val baos = new ByteArrayOutputStream()
    ImageIO.write(bimg, "jpg", baos)
    baos.flush
    val imageInByte = baos.toByteArray
    baos.close
    val pixelSize = bimg.getColorModel.getComponentSize(0)
  }

  case class Text(x: Int, y: Int, scale: Int, txt: String) extends BaseContent {
    def content =
      s"""BT/F1 1 Tf
          				    |${scale} 0 0 ${scale} $x $y Tm
          				    |($txt)Tj
          				    |ET
          				    |""".stripMargin
  }


  def test1(): Unit = {
    val pdf = new CorePdfN("b2.pdf")
    pdf.writeAll() {
      () => {
        val pageList1Id = pdf.nextId
        val font = new Font(pdf, pdf.nextId)
        val resource = new ResourceOld(pdf, pdf.nextId, font.objId)
        val txt1 = Text(10, 750, 24, "Page1 compress")
        val txt2 = Text(10, 650, 24, "test1")
        val content = new Content(pdf, pdf.nextId, List(txt1.content, txt2.content), true)
        val pageId = pdf.nextId
        val pageList2Id = pdf.nextId
        val page = new Page(pdf, pageId, pageList2Id, resource.objId, content.objId)
        val pageList2 = new PageList(pdf, pageList2Id, Some(pageList1Id), List(page.objId), 1)
        val pageList1 = new PageList(pdf, pageList1Id, None, List(pageList2.objId), 1)
        val catalog = new Catalog(pdf, pdf.nextId, pageList1.objId)

      }
    }
  }

  def test2(): Unit = {
    val pdf = new CorePdfN("b2.pdf")
    pdf.writeAll() {
      () => {
        val pageList1Id = pdf.nextId
        val font = new Font(pdf, pdf.nextId)
        val resource = new ResourceOld(pdf, pdf.nextId, font.objId)
        val pageList2Id = pdf.nextId


        val pageList = for {i <- 1 to 3
                            txt = Text(10, 750, 24, s"Page$i compress")
                            content = new Content(pdf, pdf.nextId, List(txt.content), true)
                            page = new Page(pdf, pdf.nextId, pageList2Id, resource.objId, content.objId)
        } yield page.objId


        //        val txt2 = Text(10, 750, "Page2 compress")
        //        val content2 = new Content(pdf, pdf.nextId, List(txt2.content), true)
        //        val page2 = new Page(pdf, pdf.nextId, pageList2Id, resource.objId, content2.objId)


        val pageList2 = new PageList(pdf, pageList2Id, Some(pageList1Id), pageList.toList, 3)
        val pageList1 = new PageList(pdf, pageList1Id, None, List(pageList2.objId), 3)
        val catalog = new Catalog(pdf, pdf.nextId, pageList1.objId)

      }
    }
  }


  def test3(): Unit = {
    val pdf = new CorePdfN("b3.pdf")
    pdf.writeAll() {
      () => {
        val font = new Font(pdf, pdf.nextId)
        val resource = new ResourceOld(pdf, pdf.nextId, font.objId)
        val pageList = (for {i <- 1 to 10000
                             txt = Text(10, 750, 24, s"Page$i")
                             content = new Content(pdf, pdf.nextId, List(txt.content), false)
                             page = new Page(pdf, pdf.nextId, 0L, resource.objId, content.objId)
        } yield page).toList
        val pageListId = pageList.map(pg => pg.objId)

        val list = PageTree.make(pageListId)
        var pageListRoot: PageList = null

        println("Levels size:" + list)
        PageTree.display(pageList.size, List(), list) {
          () => pdf.nextId
        } {
          (parent: Option[Long], nodeId: Long, children: List[Long], leafNbr: Long, isleaf: Boolean) => {
            if (!isleaf) {
              val pageList = new PageList(pdf, nodeId, parent, children, leafNbr)
              if (pageListRoot == null) {
                pageListRoot = pageList
              }
            } else {
              val leafNode = pageList.find(pg => pg.objId == nodeId)
              if (leafNode.isDefined) {
                leafNode.get.parent = parent.get
              }

            }
          }
        }
        val catalog = new Catalog(pdf, pdf.nextId, pageListRoot.objId)

      }
    }
  }

  def test4(): Unit = {
    val pdf = new CorePdfN("b4.pdf")
    pdf.writeAll() {
      () => {
        val pageList1Id = pdf.nextId
        val font = new Font(pdf, pdf.nextId)
        val resource = new ResourceOld(pdf, pdf.nextId, font.objId)
        val pageList2Id = pdf.nextId


        val pageList = for {i <- 1 to 3
                            fontSize = 8
                            txt = Text(10, 750, fontSize, s"Page$i")
                            txtList = List()
                            content = new Content(pdf, pdf.nextId, txtList, false)
                            page = new Page(pdf, pdf.nextId, pageList2Id, resource.objId, content.objId)
        } yield page.objId


        //        val txt2 = Text(10, 750, "Page2 compress")
        //        val content2 = new Content(pdf, pdf.nextId, List(txt2.content), true)
        //        val page2 = new Page(pdf, pdf.nextId, pageList2Id, resource.objId, content2.objId)


        val pageList2 = new PageList(pdf, pageList2Id, Some(pageList1Id), pageList.toList, 3)
        val pageList1 = new PageList(pdf, pageList1Id, None, List(pageList2.objId), 3)
        val catalog = new Catalog(pdf, pdf.nextId, pageList1.objId)

      }
    }
  }


  def test5(): Unit = {
    val pdf = new CorePdfN("b4.pdf")
    pdf.writeAll() {
      () => {
        val pageList1Id = pdf.nextId
        val font = new Font(pdf, pdf.nextId)
        val resource = new ResourceOld(pdf, pdf.nextId, font.objId)
        val pageList2Id = pdf.nextId
        val pageList = for {i <- 1 to 1
                            txt = Text(10, 750, 24, s"Test")
                            content = new Content(pdf, pdf.nextId, List(txt.content), false)
                            page = new Page(pdf, pdf.nextId, pageList2Id, resource.objId, content.objId)
        } yield page.objId

        val pageList2 = new PageList(pdf, pageList2Id, Some(pageList1Id), pageList.toList, 1)
        val catalog = new Catalog(pdf, pageList1Id, pageList2.objId)

      }
    }
  }

  def test6(): Unit = {
    val pdf = new CorePdfN("b4.pdf")
    pdf.writeAll() {
      () => {
        val pageList1Id = pdf.nextId
        val font = new Font(pdf, pdf.nextId)
        val image = new ImageMeta("im1", "examples/src/main/resources/images/bank_banner.jpg")
       // val opacity = Opacity("op1", 0.2)
        val resourceImage = ResourceImage(pdf, pdf.nextId, image)
        val resource = new ResourceOld(pdf, pdf.nextId, font.objId, List(resourceImage))
//        val txt1 = Text(10, 750, 24, "Page1 compress")
//        val rectangle1 = Rectangle(Point(10, 700), 100, 70)
//        val rectangle2 = Rectangle(Point(10, 500), 100, 70)
//        val box = Box(rectangle1, 1)
//        val fillBox1 = BoxFill(rectangle1, Color(0.2, 0.5, 1), Some(opacity))
//        val fillBox2 = BoxFill(rectangle2, Color(0.2, 0.5, 1), Some(opacity))
//        val line = Line(1, Point(30, 50), Point(300, 400))

        val drawImage = DrawImage(Point(100, 100), 0.5, image)
        val content = new Content(pdf, pdf.nextId, List(drawImage.content), false)
        val pageId = pdf.nextId
        val pageList2Id = pdf.nextId
        val page = new Page(pdf, pageId, pageList2Id, resource.objId, content.objId)
        val pageList2 = new PageList(pdf, pageList2Id, Some(pageList1Id), List(page.objId), 1)
        val pageList1 = new PageList(pdf, pageList1Id, None, List(pageList2.objId), 1)
        val catalog = new Catalog(pdf, pdf.nextId, pageList1.objId)

      }
    }
  }



  def test7(): Unit = {
    val pdf = new CorePdfN("b4.pdf")
    pdf.writeAll() {
      () => {
        val pageList1Id = pdf.nextId
        val font = new Font(pdf, pdf.nextId)
//        val image = new ImageMeta("im1", "swiss.jpg")
//        val opacity = Opacity("op1", 0.2)
//        val resourceImage = ResourceImage(pdf, pdf.nextId, image)

//        val functionType=FunctionType(pdf,pdf.nextId)
//        val shadingType=ShadingType(pdf,pdf.nextId,functionType)
//        val pattern=Pattern(pdf,pdf.nextId,shadingType)

//        val resource = new Resource(pdf, pdf.nextId, List(font,resourceImage,opacity))
        val resource = new Resource(pdf, pdf.nextId, List(font))
        val txt1 = Text(10, 750, 24, "Test")
//        val rectangle1 = Rectangle(Point(10, 700), 100, 70)
//        val rectangle2 = Rectangle(Point(10, 500), 100, 70)
//        val box = Box(rectangle1, 1)
//        val fillBox1 = BoxFill(rectangle1, Color(0.2, 0.5, 1), Some(opacity))
//        val fillBox2 = BoxFill(rectangle2, Color(0.2, 0.5, 1), Some(opacity))
        //        val fillBox2 = BoxGradientFill(rectangle2,Color(0.2,0.5,1),"aaa",Some(opacity))
//        val line = Line(1, Point(30, 50), Point(300, 400))

//        val drawImage = DrawImage(Point(100, 100), 0.5, image, Some(opacity))
//        val content = new Content(pdf, pdf.nextId, List(txt1.content, box.content, fillBox1.content, fillBox2.content, line.content, drawImage.content), false)
        val content = new Content(pdf, pdf.nextId, List(txt1.content), false)
        val pageId = pdf.nextId
        val pageList2Id = pdf.nextId
        val page = new Page(pdf, pageId, pageList2Id, resource.objId, content.objId)
        val pageList2 = new PageList(pdf, pageList2Id, Some(pageList1Id), List(page.objId), 1)
        val pageList1 = new PageList(pdf, pageList1Id, None, List(pageList2.objId), 1)
        val catalog = new Catalog(pdf, pdf.nextId, pageList1.objId)

      }
    }
  }

  def printEncoded(): Unit = {
    import java.nio.file.{Files, Paths}

    val input = Files.readAllBytes(Paths.get("a.txt"))

    val inflater = new Inflater()
    var output = new Array[Byte](input.size * 100)
    inflater.setInput(input)
    val inflateSize: Int = inflater.inflate(output)
    val result = new String(output.take(inflateSize))
    println(result)
  }

  def main(args: Array[String]) {
    test6
    //    test3
  }
}