package fpp.compiler.codegen

import java.time.Year

/** Write a CppDoc to a cpp file */
object CppDocCppWriter extends CppDocWriter {

  def paramString(p: CppDoc.Function.Param) = s"${p.t.hppType} ${p.name}"

  def paramStringComma(p: CppDoc.Function.Param) = s"${paramString(p)},"

  def writeParams(prefix: String, params: List[CppDoc.Function.Param]) = {
    if (params.length == 0) lines(s"$prefix()")
    else if (params.length == 1)
      lines(s"$prefix(" ++ paramString(params.head) ++ ")")
    else {
      val head :: tail = params.reverse
      val paramLines = (writeParam(head) :: tail.map(writeParamComma(_))).reverse
      line(s"$prefix(") :: (paramLines.map(_.indentIn(2 * indentIncrement)) :+ line(")"))
    }
  }

  override def visitClass(in: Input, c: CppDoc.Class): Output = {
    val name = c.name
    val newClassNameList = name :: in.classNameList
    val in1 = in.copy(classNameList = newClassNameList)
    c.members.map(visitClassMember(in1, _)).flatten
  }

  override def visitConstructor(in: Input, constructor: CppDoc.Class.Constructor) = {
    val unqualifiedClassName = in.getEnclosingClassUnqualified
    val qualifiedClassName = in.getEnclosingClassQualified
    val outputLines = {
      val nameLines = lines(s"$qualifiedClassName ::")
      val paramLines = {
        val lines1 = writeParams(unqualifiedClassName, constructor.params)
        val lines2 = constructor.initializers match {
          case Nil => lines1
          case _ => Line.addSuffix(lines1, " :")
         }
        lines2.map(indentIn(_))
      }
      val initializerLines = constructor.initializers.reverse match {
        case Nil => Nil
        case head :: tail => {
          val list = head :: tail.map(_ ++ ",")
          list.reverse.map(line(_)).map(_.indentIn(2 * indentIncrement))
        }
      }
      val bodyLines = CppDocWriter.writeFunctionBody(constructor.body)
      Line.blank :: List(
        nameLines,
        paramLines,
        initializerLines,
        bodyLines
      ).flatten
    }
    outputLines
  }

  override def visitCppDoc(cppDoc: CppDoc): Output = {
    val in = Input(cppDoc.hppFile, cppDoc.cppFileName)
    List(
      CppDocWriter.writeBanner(in.cppFileName),
      cppDoc.members.map(visitMember(in, _)).flatten,
    ).flatten
  }

  override def visitDestructor(in: Input, destructor: CppDoc.Class.Destructor) = {
    val unqualifiedClassName = in.getEnclosingClassUnqualified
    val qualifiedClassName = in.getEnclosingClassQualified
    val outputLines = {
      val startLine1 = line(s"$qualifiedClassName ::")
      val startLine2 = indentIn(line(s"~$unqualifiedClassName()"))
      val bodyLines = CppDocWriter.writeFunctionBody(destructor.body)
      Line.blank :: startLine1 :: startLine2 :: bodyLines
    }
    outputLines
  }

  override def visitFunction(in: Input, function: CppDoc.Function) = {
    val contentLines = {
      val startLines = {
        val prototypeLines = {
          import CppDoc.Function._
          val lines1 = writeParams(function.name, function.params)
          function.constQualifier match {
            case Const => Line.addSuffix(lines1, " const")
            case NonConst => lines1
          }
        }
        in.classNameList match {
          case head :: _ => {
            val line1 = line(s"${function.retType.getCppType} $head ::")
            line1 :: prototypeLines.map(indentIn(_))
          }
          case Nil => prototypeLines
        }
      }
      val bodyLines = CppDocWriter.writeFunctionBody(function.body)
      in.classNameList match {
        case _ :: _ => startLines ++ bodyLines
        case Nil => Line.joinLists(Line.NoIndent)(startLines)(" ")(bodyLines)
      }
    }
    Line.blank :: contentLines
  }

  override def visitLines(in: Input, lines: CppDoc.Lines) = {
    val content = lines.content
    lines.output match {
      case CppDoc.Lines.Hpp => Nil
      case CppDoc.Lines.Cpp => content
      case CppDoc.Lines.Both => content
    }
  }

  override def visitNamespace(in: Input, namespace: CppDoc.Namespace): Output = {
    val name = namespace.name
    val startLines = List(Line.blank, line(s"namespace $name {"))
    val outputLines = namespace.members.map(visitNamespaceMember(in, _)).flatten
    val endLines = List(Line.blank, line("}"))
    startLines ++ outputLines.map(indentIn(_)) ++ endLines
  }

}
